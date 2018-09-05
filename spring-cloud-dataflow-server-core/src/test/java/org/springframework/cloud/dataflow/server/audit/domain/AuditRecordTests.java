/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.audit.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gunnar Hillert
 */
public class AuditRecordTests {

	@Test
	public void testPopulateServerHost() {
		String serverHost;
		try {
			serverHost = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			serverHost = AuditRecord.UNKNOW_SERVER_HOST;
		}

		final AuditRecord auditRecord = new AuditRecord();
		assertNull(auditRecord.getServerHost());
		auditRecord.populateServerHost();
		assertNotNull(auditRecord.getServerHost());
		assertEquals(serverHost, auditRecord.getServerHost());
	}

}
