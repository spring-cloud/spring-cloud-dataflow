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
package org.springframework.cloud.dataflow.server.config.cloudfoundry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @author Mark Pollack
 */
public class RedisConnectionFactoryBeanPostProcessor implements BeanFactoryPostProcessor {

	private final Logger logger = LoggerFactory
			.getLogger(RedisConnectionFactoryBeanPostProcessor.class);

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
		String[] namesForType = configurableListableBeanFactory.getBeanNamesForType(RedisConnectionFactory.class);
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) configurableListableBeanFactory;
		for (String beanName : namesForType) {
			if (!beanName.equalsIgnoreCase("scdfRedisConnectionFactory"))	 {
				logger.info("Removing Redis Connection Factory bean defintion named " + beanName);
				registry.removeBeanDefinition(beanName);
			} else {
				logger.info("Keeping Redis Connection Factory bean definition named " + beanName);
			}
		}
	}



}
