/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.shell;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.shell.SimpleShellCommandLineOptions;
import org.springframework.stereotype.Component;

/**
 * Spring Boot {@link ConfigurationProperties} to specify well known Spring Shell properties.
 * The property prefix is <code>spring.shell</code>.
 *
 * @author Mark Pollack
 */
@ConfigurationProperties(prefix = "spring.shell")
public class ShellProperties {

    /**
     * The maximum number of lines to store in the command history file.
     */
    private int historySize = SimpleShellCommandLineOptions.DEFAULT_HISTORY_SIZE;

    /**
     * The file to read that contains shell commands
     */
    private String commandFile;

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }

    public String getCommandFile() {
        return commandFile;
    }

    public void setCommandFile(String commandFile) {
        this.commandFile = commandFile;
    }
}
