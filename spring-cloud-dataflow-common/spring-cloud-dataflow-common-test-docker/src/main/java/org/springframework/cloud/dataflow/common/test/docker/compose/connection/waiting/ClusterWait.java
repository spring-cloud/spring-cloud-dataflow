/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.joda.time.ReadableDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Cluster;

public class ClusterWait {
    private static final Logger log = LoggerFactory.getLogger(ClusterWait.class);
    private final ClusterHealthCheck clusterHealthCheck;
    private final ReadableDuration timeout;

    public ClusterWait(ClusterHealthCheck clusterHealthCheck, ReadableDuration timeout) {
        this.clusterHealthCheck = clusterHealthCheck;
        this.timeout = timeout;
    }

    public void waitUntilReady(Cluster cluster) {
        final AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure = new AtomicReference<>(
                Optional.empty());

        log.info("Waiting for cluster to be healthy");
        try {
            Awaitility.await()
                    .pollInterval(50, TimeUnit.MILLISECONDS)
                    .atMost(timeout.getMillis(), TimeUnit.MILLISECONDS)
                    .until(weHaveSuccess(cluster, lastSuccessOrFailure));
        } catch (ConditionTimeoutException e) {
            throw new IllegalStateException(serviceDidNotStartupExceptionMessage(lastSuccessOrFailure));
        }
    }

    private Callable<Boolean> weHaveSuccess(Cluster cluster,
            AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure) {
        return () -> {
            SuccessOrFailure successOrFailure = clusterHealthCheck.isClusterHealthy(cluster);
            lastSuccessOrFailure.set(Optional.of(successOrFailure));
            return successOrFailure.succeeded();
        };
    }

    private static String serviceDidNotStartupExceptionMessage(
            AtomicReference<Optional<SuccessOrFailure>> lastSuccessOrFailure) {
        String healthcheckFailureMessage = lastSuccessOrFailure.get()
                .flatMap(SuccessOrFailure::toOptionalFailureMessage)
                .orElse("The healthcheck did not finish before the timeout");

        return "The cluster failed to pass a startup check: " + healthcheckFailureMessage;
    }

}
