/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.local.security;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.rules.ExternalResource;
import org.springframework.security.ldap.server.ApacheDSContainer;
import org.springframework.util.SocketUtils;

/**
 * @author Marius Bogoevici
 * @author Gunnar Hillert
 */
public class LdapServerResource extends ExternalResource {

	private ApacheDSContainer apacheDSContainer;

	private File workingDir;

	@Override
	protected void before() throws Throwable {
		apacheDSContainer = new ApacheDSContainer("dc=springframework,dc=org",
				"classpath:org/springframework/cloud/dataflow/server/local/security/testUsers.ldif");
		int ldapPort = SocketUtils.findAvailableTcpPort();
		apacheDSContainer.setPort(ldapPort);
		apacheDSContainer.afterPropertiesSet();
		workingDir = new File(FileUtils.getTempDirectoryPath() + "/" + UUID.randomUUID().toString());
		apacheDSContainer.setWorkingDirectory(workingDir);
		apacheDSContainer.start();
		System.setProperty("ldap.port", Integer.toString(ldapPort));
	}

	@Override
	protected void after() {
		apacheDSContainer.stop();
		try {
			apacheDSContainer.destroy();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		try {
			FileUtils.deleteDirectory(workingDir);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}