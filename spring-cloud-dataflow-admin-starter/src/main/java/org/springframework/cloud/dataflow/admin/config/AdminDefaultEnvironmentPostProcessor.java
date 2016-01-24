package org.springframework.cloud.dataflow.admin.config;


import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Contributes the values from {@code admin.yml} if it exists, before any of Spring Boot's normal
 * configuration contributions apply. This has the effect of supplying overridable defaults
 * to the various Spring Cloud Dataflow Admin SPI implementations
 * that in turn override the defaults provided by Spring Boot.
 *
 * @author Josh Long
 */
public class AdminDefaultEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    protected Resource resource = new ClassPathResource("/admin.yml");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        Map<String, Object> defaults = new HashMap<>();
        MutablePropertySources existingPropertySources = environment.getPropertySources();

        this.contributeAdminDefaults(defaults);

        String defaultProperties = "defaultProperties";

        if (!existingPropertySources.contains(defaultProperties) ||
                existingPropertySources.get(defaultProperties) == null) {
            existingPropertySources.addLast(new MapPropertySource(defaultProperties, defaults));
        } else {
            PropertySource<?> propertySource = existingPropertySources.get(defaultProperties);
            Map mapOfProperties = Map.class.cast(propertySource.getSource());
            for (String k : defaults.keySet()) {
                if (!mapOfProperties.containsKey(k)) {
                    mapOfProperties.put(k, defaults.get(k));
                }
            }
        }

    }

    @Override
    public int getOrder() {
        return 0;
    }

    protected void contributeAdminDefaults(
            Map<String, Object> defaults) {
        if (this.resource.exists()) {
            YamlPropertiesFactoryBean yamlPropertiesFactoryBean =
                    new YamlPropertiesFactoryBean();
            yamlPropertiesFactoryBean.setResources(this.resource);
            yamlPropertiesFactoryBean.afterPropertiesSet();
            Properties p = yamlPropertiesFactoryBean.getObject();
            for (Object k : p.keySet()) {
                String key = k.toString();
                defaults.put(key, p.getProperty(key));
            }
        }
    }
}
