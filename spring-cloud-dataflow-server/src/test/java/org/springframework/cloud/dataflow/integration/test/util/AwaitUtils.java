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
import org.springframework.cloud.dataflow.rest.client.dsl.StreamApplication;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Provide helpers for using Awaitility.
 *
 * @author Corneil du Plessis
 */
public class AwaitUtils {
    /**
     * StreamLog will maintain the offset to the end of the log so that subsequent calls will retrieve the remainder.
     */
    public static class StreamLog {
        Stream stream;
        int offset = 0;
        String appName;

        public StreamLog(Stream stream) {
            this.stream = stream;
            setupOffset(false);
        }

        public StreamLog(Stream stream, String appName) {
            this.stream = stream;
            this.appName = appName;
            setupOffset(false);
        }
        public StreamLog(Stream stream, boolean atCurrentEnd) {
            this.stream = stream;
            setupOffset(atCurrentEnd);
        }

        public StreamLog(Stream stream, String appName, boolean atCurrentEnd) {
            this.stream = stream;
            this.appName = appName;
            setupOffset(atCurrentEnd);
        }

        private void setupOffset(boolean atCurrentEnd) {
            offset = 0;
            if (atCurrentEnd) {
                String log = extractLog();
                offset = log.length();
                Assert.isTrue(offset >= 0, "Expected offset >= 0 not " + offset);
            }
        }


        public String logs() {
            String log = extractLog();
            Assert.isTrue(offset >= 0, "Expected offset >= 0 not " + offset);
            String result = log.length() < offset ? log.substring(offset) : log;
            offset = log.length();
            Assert.isTrue(offset >= 0, "Expected offset >= 0 not " + offset);
            return result;
        }

        private String extractLog() {
            String log;
            if (StringUtils.hasText(appName)) {
                StreamApplication application = new StreamApplication(appName);
                log = stream.logs(application);
            } else {
                log = stream.logs();
            }
            return log == null ? "" : log;
        }

        public String getStatus() {
            return stream.getStatus();
        }

        public String getName() {
            return stream.getName();
        }
    }

    public static StreamLog logOffset(Stream stream) {
        return new StreamLog(stream);
    }

    public static StreamLog logOffset(Stream stream, boolean atCurrentEnd) {
        return new StreamLog(stream, atCurrentEnd);
    }

    public static StreamLog logOffset(Stream stream, String app) {
        return new StreamLog(stream, app);
    }

    public static StreamLog logOffset(Stream stream, String app, boolean atCurrentEnd) {
        return new StreamLog(stream, app, atCurrentEnd);
    }

    private static final Logger logger = LoggerFactory.getLogger(AwaitUtils.class);

    public static boolean hasErrorInLog(StreamLog offset) {
        return hasInLog(offset, " ERROR ");
    }

    public static boolean hasInLog(StreamLog offset, String value) {
        String log = offset.logs();
        String status = offset.getStatus();
        if (log.contains(value)) {
            logger.error("hasInLog:" + value + ":" + offset.getName() + ":" + status + ":" + expand(linesBeforeAfter(log, value)));
            return true;
        } else {
            if (StringUtils.hasText(log)) {
                logger.debug("hasInLog:{}:{}:{}:{}", value, offset.getName(), status, expand(log));
            }
            return false;
        }
    }

    public static boolean hasRegexInLog(StreamLog offset, String regex) {
        String log = offset.logs();
        String status = offset.getStatus();
        if (Pattern.matches(regex, log)) {
            logger.error("hasRegexInLog:" + offset.getName() + ":" + status + ":" + expand(linesBeforeAfterRegex(log, regex)));
            return true;
        } else {
            if (StringUtils.hasText(log)) {
                logger.debug("hasRegexInLog:{}:{}:{}", offset.getName(), status, expand(log));
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
        String[] sections = pattern.split(log);
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
