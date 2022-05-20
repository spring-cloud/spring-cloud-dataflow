/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.cloud.dataflow.integration.test.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.rest.client.dsl.Stream;
import org.springframework.util.StringUtils;

/**
 * Provide helpers for using Awaitility.
 * @author Corneil du Plessis
 */
public class AwaitUtils {
    private static final Logger logger = LoggerFactory.getLogger(AwaitUtils.class);
    public static boolean hasErrorInLog(Stream stream) {
        String log = stream.logs();
        String status = stream.getStatus();
        if (log.contains("ERROR")) {
            logger.error("hasErrorInLog:" + stream.getName() + ":" + status + ":" + expand(linesBeforeAfter(log, "ERROR")));
            return true;
        } else {
            if (StringUtils.hasText(log)) {
                logger.debug("hasErrorInLog:{}:{}:{}", stream.getName(), status, expand(log));
            }
            return false;
        }
    }

    public static String expand(String log) {
        return log.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r");
    }

    public static String linesBeforeAfter(String log, String match) {
        int matchIndex = log.indexOf(match);
        if (matchIndex > 0) {
            String target = log.substring(matchIndex > 320 ? matchIndex - 320 : matchIndex);
            int start = target.indexOf('\n');
            if (start < 0) {
                start = 0;
            }
            return target.substring(start);
        }
        return log;
    }
}
