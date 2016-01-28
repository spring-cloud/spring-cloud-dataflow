package org.springframework.cloud.dataflow.admin.config;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;

/**
 * @author Josh Long
 */
public class AdminDefaultEnvironmentPostProcessorTest {
    public static final String MANAGEMENT_CONTEXT_PATH = "management.contextPath";

    public static final String CONTRIBUTED_PATH = "/bar";

    @Configuration
    @EnableAutoConfiguration
    public static class EmptyDefaultApp {
    }

    @Test
    public void testDefaultsBeingContributedByAdminModule() throws Exception {
        try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultApp.class, "--server.port=0")) {
            String cp = ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH);
            assertEquals(CONTRIBUTED_PATH, cp);
        }
    }

    @Test
    public void testOverridingDefaultsWithAConfigFile() {
        try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultApp.class,
                "--spring.config.name=test", "--server.port=0")) {
            String cp = ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH);
            assertEquals(cp, "/foo");
        }
    }
}