/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.audit.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
class DefaultAuditRecordServiceTests {

    private AuditRecordRepository auditRecordRepository;

    @BeforeEach
    void setupMock() {
        this.auditRecordRepository = mock(AuditRecordRepository.class);
    }

    @Test
    void initializationWithNullParameters() {
        try {
            new DefaultAuditRecordService(null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("auditRecordRepository must not be null.");
            return;
        }
        fail("Expected an Exception to be thrown.");
    }

    @Test
    void populateAndSaveAuditRecord() {
        final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);
        auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234",
                "my data", "test-platform");

        final ArgumentCaptor<AuditRecord> argument = ArgumentCaptor.forClass(AuditRecord.class);
        verify(this.auditRecordRepository, times(1)).save(argument.capture());
        verifyNoMoreInteractions(this.auditRecordRepository);

        AuditRecord auditRecord = argument.getValue();

        assertThat(auditRecord.getAuditAction()).isEqualTo(AuditActionType.CREATE);
        assertThat(auditRecord.getAuditOperation()).isEqualTo(AuditOperationType.SCHEDULE);
        assertThat(auditRecord.getCorrelationId()).isEqualTo("1234");
        assertThat(auditRecord.getAuditData()).isEqualTo("my data");
        assertThat(auditRecord.getPlatformName()).isEqualTo("test-platform");
    }

    @Test
    void populateAndSaveAuditRecordWithNullAuditActionType() {
        final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);

        try {
            auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, null, "1234", "my audit data", "test-platform");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("auditActionType must not be null.");
            return;
        }
        fail("Expected an Exception to be thrown.");
    }

    @Test
    void populateAndSaveAuditRecordWithNullAuditOperationType() {
        final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);

        try {
            auditRecordService.populateAndSaveAuditRecord(null, AuditActionType.CREATE, "1234", "my audit data", "test-platform");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("auditOperationType must not be null.");
            return;
        }
        fail("Expected an Exception to be thrown.");
    }

    @Test
    void populateAndSaveAuditRecordWithMapData() throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, mapper);

        final Map<String, Object> mapAuditData = new HashMap<>(2);
        mapAuditData.put("foo1", "bar1");
        mapAuditData.put("foofoo", "barbar");

        auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE,
                "1234", mapAuditData, "test-platform");

        final ArgumentCaptor<AuditRecord> argument = ArgumentCaptor.forClass(AuditRecord.class);
        verify(this.auditRecordRepository, times(1)).save(argument.capture());
        verifyNoMoreInteractions(this.auditRecordRepository);

        final AuditRecord auditRecord = argument.getValue();

        assertThat(auditRecord.getAuditAction()).isEqualTo(AuditActionType.CREATE);
        assertThat(auditRecord.getAuditOperation()).isEqualTo(AuditOperationType.SCHEDULE);
        assertThat(auditRecord.getCorrelationId()).isEqualTo("1234");
        assertThat(mapper.readTree(auditRecord.getAuditData())).isEqualTo(mapper.convertValue(mapAuditData, JsonNode.class));
        assertThat(auditRecord.getPlatformName()).isEqualTo("test-platform");
    }

    @Test
    void populateAndSaveAuditRecordUsingMapDataWithNullAuditActionType() {
        final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);

        final Map<String, Object> mapAuditData = new HashMap<>(2);
        mapAuditData.put("foo", "bar");

        try {
            auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, null, "1234",
                    mapAuditData, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("auditActionType must not be null.");
            return;
        }
        fail("Expected an Exception to be thrown.");
    }

    @Test
    void populateAndSaveAuditRecordUsingMapDataWithNullAuditOperationType() {
        final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);

        final Map<String, Object> mapAuditData = new HashMap<>(2);
        mapAuditData.put("foo", "bar");

        try {
            auditRecordService.populateAndSaveAuditRecordUsingMapData(null, AuditActionType.CREATE, "1234",
                    mapAuditData, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("auditOperationType must not be null.");
            return;
        }
        fail("Expected an Exception to be thrown.");
    }

    @Test
    void populateAndSaveAuditRecordUsingMapDataThrowingJsonProcessingException()
                                                  throws JsonProcessingException {
        final ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any(Object.class))).thenThrow(new JsonProcessingException("Error") {
            private static final long serialVersionUID = 1L;
        });

        final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository,
                objectMapper);

        final Map<String, Object> mapAuditData = new HashMap<>(2);
        mapAuditData.put("foo", "bar");

        auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE,
                "1234", mapAuditData, "test-platform");

        final ArgumentCaptor<AuditRecord> argument = ArgumentCaptor.forClass(AuditRecord.class);
        verify(this.auditRecordRepository, times(1)).save(argument.capture());
        verifyNoMoreInteractions(this.auditRecordRepository);

        AuditRecord auditRecord = argument.getValue();

        assertThat(auditRecord.getAuditAction()).isEqualTo(AuditActionType.CREATE);
        assertThat(auditRecord.getAuditOperation()).isEqualTo(AuditOperationType.SCHEDULE);
        assertThat(auditRecord.getCorrelationId()).isEqualTo("1234");
        assertThat(auditRecord.getPlatformName()).isEqualTo("test-platform");
        assertThat(auditRecord.getAuditData()).isEqualTo("Error serializing audit record data.  Data = {foo=bar}");


    }

    @Test
    void populateAndSaveAuditRecordUsingSensitiveMapData() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, objectMapper);

        final Map<String, Object> mapAuditData = new HashMap<>(2);
        mapAuditData.put("foo", "bar");
        mapAuditData.put("spring.cloud.config.password", "12345");
        final Map<String, String> child = new HashMap<>();
        child.put("password", "54321");
        child.put("bar1", "foo2");
        mapAuditData.put("spring.child", child);
        mapAuditData.put("spring.empty", Collections.emptyMap());

        auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE,
                "1234", mapAuditData, "test-platform");

        final ArgumentCaptor<AuditRecord> argument = ArgumentCaptor.forClass(AuditRecord.class);
        verify(this.auditRecordRepository, times(1)).save(argument.capture());
        verifyNoMoreInteractions(this.auditRecordRepository);

        AuditRecord auditRecord = argument.getValue();

        assertThat(auditRecord.getAuditAction()).isEqualTo(AuditActionType.CREATE);
        assertThat(auditRecord.getAuditOperation()).isEqualTo(AuditOperationType.SCHEDULE);
        assertThat(auditRecord.getCorrelationId()).isEqualTo("1234");

        assertThat(auditRecord.getPlatformName()).isEqualTo("test-platform");
        System.out.println("auditData=" + auditRecord.getAuditData());
        assertThat(auditRecord.getAuditData()).contains("\"******\"");
        assertThat(auditRecord.getAuditData()).contains("\"bar\"");
        assertThat(auditRecord.getAuditData()).contains("\"foo\"");
        assertThat(auditRecord.getAuditData()).contains("\"spring.cloud.config.password\"");
        assertThat(auditRecord.getAuditData()).contains("\"password\"");
        assertThat(auditRecord.getAuditData()).doesNotContain("54321");
        assertThat(auditRecord.getAuditData()).doesNotContain("12345");
    }

    @Test
    void findAuditRecordByAuditOperationTypeAndAuditActionType() {
        AuditRecordService auditRecordService = new DefaultAuditRecordService(auditRecordRepository);

        AuditActionType[] auditActionTypes = {AuditActionType.CREATE};
        AuditOperationType[] auditOperationTypes = {AuditOperationType.STREAM};
        PageRequest pageRequest = PageRequest.of(0, 1);
        auditRecordService.findAuditRecordByAuditOperationTypeAndAuditActionTypeAndDate(pageRequest, auditActionTypes,
                auditOperationTypes, null, null);

        verify(this.auditRecordRepository, times(1)).findByActionTypeAndOperationTypeAndDate(eq(auditOperationTypes),
                eq(auditActionTypes), isNull(), isNull(), eq(pageRequest));
        verifyNoMoreInteractions(this.auditRecordRepository);
    }

    @Test
    void findAuditRecordByAuditOperationTypeAndAuditActionTypeWithNullAuditActionType() {
        AuditRecordService auditRecordService = new DefaultAuditRecordService(auditRecordRepository);

        AuditOperationType[] auditOperationTypes = {AuditOperationType.STREAM};
        PageRequest pageRequest = PageRequest.of(0, 1);
        auditRecordService.findAuditRecordByAuditOperationTypeAndAuditActionTypeAndDate(pageRequest, null,
                auditOperationTypes, null, null);

        verify(this.auditRecordRepository, times(1)).findByActionTypeAndOperationTypeAndDate(eq(auditOperationTypes),
                isNull(), isNull(), isNull(), eq(pageRequest));
        verifyNoMoreInteractions(this.auditRecordRepository);
    }

    @Test
    void findAuditRecordByAuditOperationTypeAndAuditActionTypeWithNullOperationType() {
        AuditRecordService auditRecordService = new DefaultAuditRecordService(auditRecordRepository);

        AuditActionType[] auditActionTypes = {AuditActionType.CREATE};
        PageRequest pageRequest = PageRequest.of(0, 1);
        auditRecordService.findAuditRecordByAuditOperationTypeAndAuditActionTypeAndDate(pageRequest, auditActionTypes,
                null, null, null);

        verify(this.auditRecordRepository, times(1)).findByActionTypeAndOperationTypeAndDate(isNull(),
                eq(auditActionTypes), isNull(), isNull(), eq(pageRequest));
        verifyNoMoreInteractions(this.auditRecordRepository);
    }

    @Test
    void findAuditRecordByAuditOperationTypeAndAuditActionTypeWithNullActionAndOperationType() {
        AuditRecordService auditRecordService = new DefaultAuditRecordService(auditRecordRepository);

        PageRequest pageRequest = PageRequest.of(0, 1);
        auditRecordService.findAuditRecordByAuditOperationTypeAndAuditActionTypeAndDate(pageRequest, null, null, null,
                null);

        verify(this.auditRecordRepository, times(1)).findByActionTypeAndOperationTypeAndDate(isNull(), isNull(),
                isNull(), isNull(), eq(pageRequest));
        verifyNoMoreInteractions(this.auditRecordRepository);
    }
}
