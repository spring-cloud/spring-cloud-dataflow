/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.support;

import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;

/**
 * Event Listener class to log Login audit events .
 *
 * @author Deepak Gunasekaran
 * 
 */
public class AuthenticationSuccessEventListener implements ApplicationListener<InteractiveAuthenticationSuccessEvent> {

	private AuditRecordService auditRecordService;

	public AuthenticationSuccessEventListener(AuditRecordService auditRecordService) {
		this.auditRecordService = auditRecordService;
	}

	@Override
	public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
		this.auditRecordService.populateAndSaveAuditRecord(AuditOperationType.LOGIN, AuditActionType.LOGIN_SUCCESS,
				"Login",
				"Successful login", null);

	}

}
