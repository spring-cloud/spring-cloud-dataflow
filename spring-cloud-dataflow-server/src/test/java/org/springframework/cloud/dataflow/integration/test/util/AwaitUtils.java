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

import java.util.regex.Pattern;

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
        return hasInLog(stream," ERROR ");
    }
    public static boolean hasInLog(Stream stream, String value) {
        String log = stream.logs();
        String status = stream.getStatus();
        if (log.contains(value)) {
            logger.error("hasInLog:" + value + ":" + stream.getName() + ":" + status + ":" + expand(linesBeforeAfter(log, value)));
            return true;
        } else {
            if (StringUtils.hasText(log)) {
                logger.debug("hasInLog:{}:{}:{}:{}", value, stream.getName(), status, expand(log));
            }
            return false;
        }
    }
    public static boolean hasRegexInLog(Stream stream, String regex) {
        String log = stream.logs();
        String status = stream.getStatus();
        if (Pattern.matches(regex, log)) {
            logger.error("hasRegexInLog:" + stream.getName() + ":" + status + ":" + expand(linesBeforeAfterRegex(log, regex)));
            return true;
        } else {
            if (StringUtils.hasText(log)) {
                logger.debug("hasRegexInLog:{}:{}:{}", stream.getName(), status, expand(log));
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
    public static String linesBeforeAfterRegex(String log, String regex) {
        Pattern pattern = Pattern.compile(regex);
        String [] sections = pattern.split(log);
        int matchIndex = log.indexOf(sections.length > 1 ? sections[1] : sections[0]);
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
