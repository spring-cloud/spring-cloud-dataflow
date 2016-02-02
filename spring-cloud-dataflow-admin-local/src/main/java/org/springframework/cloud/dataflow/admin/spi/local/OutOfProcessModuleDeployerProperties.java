/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.admin.spi.local;

import java.io.File;
import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the {@link OutOfProcessModuleDeployer}.
 *
 * @author Eric Bottard
 */
@ConfigurationProperties(prefix = LocalDeployerAutoConfiguration.LOCAL_DEPLOYER_PREFIX)
public class OutOfProcessModuleDeployerProperties {

	/**
	 * Whether to use the out-of-process module deployer.
	 */
	private boolean useOutOfProcess = false;

	/**
	 * Directory in which all created processes will run and create log files.
	 */
	private Path workingDirectoriesRoot = new File(System.getProperty("java.io.tmpdir")).toPath();

	/**
	 * Whether to delete created files and directories on JVM exit.
	 */
	private boolean deleteFilesOnExit = true;

	/**
	 * The command to run java.
	 */
	private String javaCmd = "java";

	public String getJavaCmd() {
		return javaCmd;
	}

	public void setJavaCmd(String javaCmd) {
		this.javaCmd = javaCmd;
	}

	public boolean isUseOutOfProcess() {
		return useOutOfProcess;
	}

	public void setUseOutOfProcess(boolean useOutOfProcess) {
		this.useOutOfProcess = useOutOfProcess;
	}

	public Path getWorkingDirectoriesRoot() {
		return workingDirectoriesRoot;
	}

	public void setWorkingDirectoriesRoot(Path workingDirectoriesRoot) {
		this.workingDirectoriesRoot = workingDirectoriesRoot;
	}

	public boolean isDeleteFilesOnExit() {
		return deleteFilesOnExit;
	}

	public void setDeleteFilesOnExit(boolean deleteFilesOnExit) {
		this.deleteFilesOnExit = deleteFilesOnExit;
	}
}
