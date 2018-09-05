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
package org.springframework.cloud.dataflow.server.audit.service;



import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditRecord;
import org.springframework.cloud.dataflow.server.audit.repository.AuditRecordRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Gunnar Hillert
 */
public class DefaultAuditRecordServiceTests {

	private AuditRecordRepository auditRecordRepository;

	@Before
	public void setupMock() {
		this.auditRecordRepository = mock(AuditRecordRepository.class);
	}

	@Test
	public void testInitializationWithNullParameters() {
		try {
			new DefaultAuditRecordService(null, null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditRecordRepository must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testInitializationWithSecondNullParameter() {
		try {
			new DefaultAuditRecordService(auditRecordRepository, null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("objectMapper must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecord() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());
		auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", "my data");

		final ArgumentCaptor<AuditRecord> argument = ArgumentCaptor.forClass(AuditRecord.class);
		verify(this.auditRecordRepository, times(1)).save(argument.capture());
		verifyNoMoreInteractions(this.auditRecordRepository);

		AuditRecord auditRecord = argument.getValue();

		assertEquals(AuditActionType.CREATE, auditRecord.getAuditAction());
		assertEquals(AuditOperationType.SCHEDULE, auditRecord.getAuditOperation());
		assertEquals("1234", auditRecord.getCorrelationId());
		assertEquals("my data", auditRecord.getAuditData());
	}

	@Test
	public void testPopulateAndSaveAuditRecordWithNullDataParameter() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		try {
			auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("data must not be null nor empty.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordWithEmptyDataParameter() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		try {
			auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", " ");
		}
		catch (IllegalArgumentException e) {
			assertEquals("data must not be null nor empty.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordWithNullAuditActionType() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		try {
			auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, null, "1234", "my audit data");
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditActionType must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordWithNullAuditOperationType() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		try {
			auditRecordService.populateAndSaveAuditRecord(null, AuditActionType.CREATE, "1234", "my audit data");
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditOperationType must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordWithNullCorellationId() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		try {
			auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, AuditActionType.CREATE, null, "my audit data");
		}
		catch (IllegalArgumentException e) {
			assertEquals("correlationId must not be null nor empty.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordWithEmptyCorellationId() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		try {
			auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, AuditActionType.CREATE, " ", "my audit data");
		}
		catch (IllegalArgumentException e) {
			assertEquals("correlationId must not be null nor empty.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordWithMapData() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo1", "bar1");
		mapAuditData.put("foofoo", "barbar");

		auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", mapAuditData);

		final ArgumentCaptor<AuditRecord> argument = ArgumentCaptor.forClass(AuditRecord.class);
		verify(this.auditRecordRepository, times(1)).save(argument.capture());
		verifyNoMoreInteractions(this.auditRecordRepository);

		final AuditRecord auditRecord = argument.getValue();

		assertEquals(AuditActionType.CREATE, auditRecord.getAuditAction());
		assertEquals(AuditOperationType.SCHEDULE, auditRecord.getAuditOperation());
		assertEquals("1234", auditRecord.getCorrelationId());
		assertEquals("{\"foofoo\":\"barbar\",\"foo1\":\"bar1\"}", auditRecord.getAuditData());
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataWithNullMapData() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		try {
			auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("data map must not be null and must contain at least 1 entry.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataWithEmptyMapData() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		final Map<String, Object> mapAuditData = new HashMap<>(2);

		try {
			auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", mapAuditData);
		}
		catch (IllegalArgumentException e) {
			assertEquals("data map must not be null and must contain at least 1 entry.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataWithNullAuditActionType() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo", "bar");

		try {
			auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, null, "1234", mapAuditData);
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditActionType must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataWithNullAuditOperationType() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo", "bar");

		try {
			auditRecordService.populateAndSaveAuditRecordUsingMapData(null, AuditActionType.CREATE, "1234", mapAuditData);
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditOperationType must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataWithNullCorellationId() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo", "bar");

		try {
			auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE, null, mapAuditData);
		}
		catch (IllegalArgumentException e) {
			assertEquals("correlationId must not be null nor empty.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataWithEmptyCorellationId() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, new ObjectMapper());

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo", "bar");

		try {
			auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE, " ", mapAuditData);
		}
		catch (IllegalArgumentException e) {
			assertEquals("correlationId must not be null nor empty.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataThrowingJsonProcessingException() throws JsonProcessingException {
		final ObjectMapper objectMapper = mock(ObjectMapper.class);
		when(objectMapper.writeValueAsString(any(Object.class))).thenThrow(new JsonProcessingException("Error"){
			private static final long serialVersionUID = 1L;
		});

		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, objectMapper);

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo", "bar");

		try {
			auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", mapAuditData);
		}
		catch (IllegalStateException e) {
			assertEquals("Error serializing audit record data.", e.getMessage());
			assertTrue("The cause of the exception should a JsonProcessingException", e.getCause() instanceof JsonProcessingException);
			return;
		}
		fail("Expected an Exception to be thrown.");
	}
}
