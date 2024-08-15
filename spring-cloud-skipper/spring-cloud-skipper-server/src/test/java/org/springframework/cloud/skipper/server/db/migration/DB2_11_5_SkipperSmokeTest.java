/*
 * Copyright 2023 the original author or authors.
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

import org.springframework.cloud.dataflow.server.db.DB2_11_5_ContainerSupport;


/**
 * Basic database schema and JPA tests for DB2.
 *
 * @author Corneil du Plessis
 * @author Corneil du Plessis
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_DB2", matches = "true", disabledReason = "Container is too big")
@Tag("DB2")
public class DB2_11_5_SkipperSmokeTest extends AbstractSkipperSmokeTest implements DB2_11_5_ContainerSupport {
}
