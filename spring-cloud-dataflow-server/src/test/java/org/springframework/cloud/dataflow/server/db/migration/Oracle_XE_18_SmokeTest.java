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
package org.springframework.cloud.dataflow.server.db.migration;

import org.junit.jupiter.api.Disabled;
import org.springframework.cloud.dataflow.server.db.Oracle_XE_18_ContainerSupport;

/**
 * Basic database schema and JPA tests for Oracle XE.
 *
 * @author Corneil du Plessis
 * @author Chris Bono
 */
//TODO: Boot3x followup Looks like we are trying to access Java 8 code in some of the DB libraries with Java 17 in
// and is causing the problem below
// java.lang.reflect.InaccessibleObjectException: Unable to make field private final java.util.Map java.util.Collections$UnmodifiableMap.m accessible: module java.base does not "opens java.util" to unnamed module
@Disabled("TODO: Boot3x followup  followup Looks like we are trying to access Java 8 code in some of the DB libraries with Java 17 in")
public class Oracle_XE_18_SmokeTest extends AbstractSmokeTest implements Oracle_XE_18_ContainerSupport {
}
