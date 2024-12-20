/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.controller.docs;

import org.junit.jupiter.api.Disabled;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Documentation tests suite.
 *
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@Disabled("we don't need to run suites for docs")
@Suite
@SelectClasses({ AboutDocumentation.class, InstallDocumentation.class, ListDocumentation.class,
		CancelDocumentation.class, DeleteDocumentation.class, HistoryDocumentation.class, ManifestDocumentation.class,
		RollbackDocumentation.class, StatusDocumentation.class, UpgradeDocumentation.class, UploadDocumentation.class })
public class DocumentationTests {

}
