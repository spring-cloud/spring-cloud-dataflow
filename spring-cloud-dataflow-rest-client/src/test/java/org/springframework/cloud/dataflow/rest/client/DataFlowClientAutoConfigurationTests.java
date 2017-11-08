package org.springframework.cloud.dataflow.rest.client;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Vinicius Carvalho
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class DataFlowClientAutoConfigurationTests {

	@Test
	public void contextLoads() throws Exception {

	}

	@SpringBootApplication
	static class TestApplication {

	}
}
