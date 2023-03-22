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
package org.springframework.cloud.dataflow.common.test.docker.compose.logging;

import java.util.Optional;

public class LogDirectory {

    private LogDirectory() {}

    /**
     * For tests running on CircleCI, save logs into <code>$CIRCLE_ARTIFACTS/dockerLogs/&lt;testClassName&gt;</code>.
     * This ensures partial logs can be recovered if the build is cancelled or times out, and
     * also avoids needless copying.
     *
     * Otherwise, save logs from local runs to a folder inside <code>$project/build/dockerLogs</code> named
     * after the test class.
     *
     * @param testClass the JUnit test class whose name will appear on the log folder
     * @return log directory
     */
    public static String circleAwareLogDirectory(Class<?> testClass) {
        return circleAwareLogDirectory(testClass.getSimpleName());
    }

    public static String circleAwareLogDirectory(String logDirectoryName) {
        String artifactRoot = Optional.ofNullable(System.getenv("CIRCLE_ARTIFACTS")).orElse("build");
        return artifactRoot + "/dockerLogs/" + logDirectoryName;
    }

    /**
     * Save logs into a new folder, $project/build/dockerLogs/&lt;testClassName&gt;.
     *
     * @param testClass the JUnit test class whose name will appear on the log folder
     * @return log directory
     */
    public static String gradleDockerLogsDirectory(Class<?> testClass) {
        return "build/dockerLogs/" + testClass.getSimpleName();
    }
}
