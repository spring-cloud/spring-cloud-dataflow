/*
 * Copyright 2023-2023 the original author or authors.
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
package org.springframework.cloud.skipper.server.db.migration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.cloud.dataflow.server.db.Oracle_XE_18_ContainerSupport;

/**
 * Basic database schema and JPA tests for Oracle XE.
 *
 * @author Corneil du Plessis
 * @author Chris Bono
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_ORACLE", matches = "true", disabledReason = "Container is too big")
@Tag("ORACLE")
public class Oracle_XE_18_SkipperSmokeTest extends AbstractSkipperSmokeTest implements Oracle_XE_18_ContainerSupport {
}
