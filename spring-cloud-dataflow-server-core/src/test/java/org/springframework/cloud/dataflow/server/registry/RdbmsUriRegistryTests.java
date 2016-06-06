/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.registry;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.dataflow.server.repository.support.DefinitionRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EmbeddedDataSourceConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class, RdbmsUriRegistryTests.TestConfig.class})
public class RdbmsUriRegistryTests {

	@Autowired
	private DataSource dataSource;

	private RdbmsUriRegistry registry;

	private JdbcTemplate template;

	@Before
	public void setup() throws Exception{
		registry = new RdbmsUriRegistry(dataSource);
		template = new JdbcTemplate(dataSource);
		template.execute("DELETE FROM URI_APP_REGISTRY");
	}
	
	@Test
	public void testFind() throws Exception {
		URI test1URI = new URI("test1URI");
		URI test2URI = new URI("test2URI");
		registry.register("source.test1", test1URI);
		registry.register("sink.test2", test2URI);
		assertEquals(test1URI, registry.find("source.test1"));
		assertEquals(test2URI, registry.find("sink.test2"));
	}

	@Test
	public void testFindAll() throws Exception {
		URI test1URI = new URI("test1URI");
		URI test2URI = new URI("test2URI");
		Map<String, URI> map = new HashMap<>();
		map.put("source.test1", test1URI);
		map.put("sink.test2", test2URI);
		registry.register("source.test1", test1URI);
		registry.register("sink.test2", test2URI);
		assertEquals(map, registry.findAll());
	}

	@Test
	public void testRegisterOverwrite() throws Exception {
		URI test1URI = new URI("test1URI");
		URI test2URI = new URI("test2URI");
		registry.register("source.test1", test1URI);
		assertEquals(test1URI, registry.find("source.test1"));
		registry.register("source.test1", test2URI);
		assertEquals(test2URI, registry.find("source.test1"));
	}


	@Test
	public void testUnregister() throws Exception {
		URI test1URI = new URI("test1URI");
		URI test2URI = new URI("test2URI");
		registry.register("source.test1", test1URI);
		registry.register("sink.test2", test2URI);
		assertEquals(test1URI, registry.find("source.test1"));
		registry.unregister("source.test1");
		assertNull(registry.find("source.test1"));
		assertEquals(test2URI, registry.find("sink.test2"));
	}

	@Configuration
	protected static class TestConfig {

		@Bean
		public DefinitionRepositoryInitializer definitionRepositoryInitializer(DataSource dataSource) {
			DefinitionRepositoryInitializer definitionRepositoryInitializer = new  DefinitionRepositoryInitializer();
			definitionRepositoryInitializer.setDataSource(dataSource);
			return definitionRepositoryInitializer;
		}
	}
}
