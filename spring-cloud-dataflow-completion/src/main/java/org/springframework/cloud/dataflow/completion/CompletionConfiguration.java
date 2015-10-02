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

package org.springframework.cloud.dataflow.completion;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by ericbottard on 02/10/15.
 */
@Configuration
@ComponentScan
public class CompletionConfiguration {

	@Bean
	public StreamCompletionProvider streamCompletionProvider() {
		return new StreamCompletionProvider();
	}

	@Bean
	public StreamDefinitionResolver streamDefinitionResolver() {
		return new StreamDefinitionResolver();
	}

}
