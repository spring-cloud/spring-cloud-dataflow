/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.local.security;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.ldap.server.ApacheDSContainer;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SocketUtils;

/**
 * @author Marius Bogoevici
 * @author Gunnar Hillert
 */
public class LdapServerResource implements AfterEachCallback, BeforeEachCallback {

	private static final String LDAP_PORT_PROPERTY = "ldap.port";

	private static final ClassPathResource keyStoreResource = new ClassPathResource(
			"/org/springframework/cloud/dataflow/server/local/security/dataflow.keystore");

	private static final ClassPathResource trustStoreResource = new ClassPathResource(
			"/org/springframework/cloud/dataflow/server/local/security/dataflow.truststore");

	private static final String TRUST_STORE_PASSWORD = "dataflow";

	private static final String KEY_STORE_PASSWORD = "dataflow";

	private String originalLdapPort;

	private ApacheDSContainer apacheDSContainer;

	private final File temporaryFolder = Files.newTemporaryFolder();

	private File workingDir;

	private boolean enabledSsl = false;

	private final String ldapFileName;

	public LdapServerResource() {
		super();
		this.ldapFileName = "testUsers.ldif";
	}

	public LdapServerResource(String ldapFileName) {
		super();
		this.ldapFileName = ldapFileName;
	}

	public LdapServerResource(boolean enabledSsl) {
		this.enabledSsl = true;
		this.ldapFileName = "testUsers.ldif";
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		originalLdapPort = System.getProperty(LDAP_PORT_PROPERTY);
		apacheDSContainer = new ApacheDSContainer("dc=springframework,dc=org",
				"classpath:org/springframework/cloud/dataflow/server/local/security/" + this.ldapFileName);
		int ldapPort = SocketUtils.findAvailableTcpPort();
		if (enabledSsl) {

			apacheDSContainer.setLdapOverSslEnabled(true);

			final File temporaryKeyStoreFile = new File(temporaryFolder, "dataflow.keystore");
			final File temporaryTrustStoreFile = new File(temporaryFolder, "dataflow.truststore");

			FileCopyUtils.copy(keyStoreResource.getInputStream(), new FileOutputStream(temporaryKeyStoreFile));
			FileCopyUtils.copy(trustStoreResource.getInputStream(), new FileOutputStream(temporaryTrustStoreFile));

			Assert.isTrue(temporaryKeyStoreFile.isFile(), "temporaryKeyStoreFile.isFile can not be null");
			Assert.isTrue(temporaryTrustStoreFile.isFile(), "temporaryTrustStoreFile.isfile can not be null");

			apacheDSContainer.setKeyStoreFile(temporaryKeyStoreFile);
			apacheDSContainer.setCertificatePassord(KEY_STORE_PASSWORD);

			System.setProperty("javax.net.ssl.trustStorePassword", TRUST_STORE_PASSWORD);
			System.setProperty("javax.net.ssl.trustStore", temporaryTrustStoreFile.getAbsolutePath());
			System.setProperty("javax.net.ssl.trustStoreType", "jks");
		}

		apacheDSContainer.setPort(ldapPort);
		apacheDSContainer.afterPropertiesSet();
		workingDir = new File(temporaryFolder, UUID.randomUUID().toString());
		apacheDSContainer.setWorkingDirectory(workingDir);
		apacheDSContainer.start();
		System.setProperty(LDAP_PORT_PROPERTY, Integer.toString(ldapPort));
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		apacheDSContainer.stop();
		try {
			apacheDSContainer.destroy();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {

			if (originalLdapPort != null) {
				System.setProperty(LDAP_PORT_PROPERTY, originalLdapPort);
			}
			else {
				System.clearProperty(LDAP_PORT_PROPERTY);
			}

			System.clearProperty("javax.net.ssl.trustStorePassword");
			System.clearProperty("javax.net.ssl.trustStore");
			System.clearProperty("javax.net.ssl.trustStoreType");

			temporaryFolder.delete();
		}
	}

}
