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
package org.springframework.cloud.dataflow.common.test.docker.compose.execution;

import java.io.IOException;
import org.joda.time.Duration;
import org.joda.time.ReadableDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Retryer {
    private static final Logger log = LoggerFactory.getLogger(Retryer.class);
    public static final ReadableDuration STANDARD_DELAY = Duration.standardSeconds(5);

    public interface RetryableDockerOperation<T> {
        T call() throws IOException, InterruptedException;
    }

    private final int retryAttempts;
    private final ReadableDuration delay;

    public Retryer(int retryAttempts, ReadableDuration delay) {
        this.retryAttempts = retryAttempts;
        this.delay = delay;
    }

    public <T> T runWithRetries(RetryableDockerOperation<T> operation) throws IOException, InterruptedException {
        DockerExecutionException lastExecutionException = null;
        for (int i = 0; i <= retryAttempts; i++) {
            try {
                return operation.call();
            } catch (DockerExecutionException e) {
                lastExecutionException = e;
                log.warn("Caught exception: {}", e.getMessage());
                log.warn("Retrying after {}", delay);
                if (i < retryAttempts) {
                    Thread.sleep(delay.getMillis());
                }
            }
        }

        log.error("Exhausted all retry attempts. Tried {} times.", retryAttempts);
        throw lastExecutionException;
    }
}
