/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.data.rest.config.AdminConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @author Mark Fisher
 */
@SpringBootApplication
@Import(AdminConfiguration.class)
public class AdminApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(AdminApplication.class, "--server.port=9393", "--management.contextPath=/management");
	}

}
