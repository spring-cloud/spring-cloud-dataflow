/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.cloud.dataflow.singlestepbatchjob;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.test.AssertFile;
import org.springframework.boot.SpringApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

/**
 * Test the flow of the  {@link SingleStepBatchJobApplication}
 *
 * @author Glenn Renfro
 * @since 2.9.0
 */
class SingleStepJobTests {

	private File outputFile;

	@BeforeEach
	void setup() {
		outputFile = new File("result.txt");
	}

	@AfterEach
	void tearDown() throws Exception {
		Files.deleteIfExists(Paths.get(outputFile.getAbsolutePath()));
	}

	@Test
	void fileReaderFileWriter() throws Exception {
		getSpringApplication().run(SingleStepBatchJobApplication.class,
				"--spring.application.name=Single Step Batch Job",
				"foo=testFileReaderJdbcWriter");
		validateFileResult();
	}

	private void validateFileResult() throws Exception{
		AssertFile.assertLineCount(6, new FileSystemResource("./result.txt"));
		AssertFile.assertFileEquals(new ClassPathResource("testresult.txt"),
				new FileSystemResource(this.outputFile));
	}

	private SpringApplication getSpringApplication() {
		SpringApplication springApplication = new SpringApplication();
		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.application.name", "Single Step Batch Job");
		properties.put("spring.batch.job.jobName", "job");
		properties.put("spring.batch.job.stepName", "step1");
		properties.put("spring.batch.job.chunkSize", "5");
		springApplication.setDefaultProperties(properties);
		return springApplication;
	}
}
