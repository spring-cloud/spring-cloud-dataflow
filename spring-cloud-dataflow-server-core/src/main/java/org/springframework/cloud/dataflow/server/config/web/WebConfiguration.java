package org.springframework.cloud.dataflow.server.config.web;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.cloud.dataflow.rest.job.support.ISO8601DateFormatWithMilliSeconds;
import org.springframework.cloud.dataflow.server.job.support.ExecutionContextJacksonMixIn;
import org.springframework.cloud.dataflow.server.job.support.StepExecutionJacksonMixIn;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.core.DefaultRelProvider;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Patrick Peralta
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@ConditionalOnWebApplication
public class WebConfiguration {

    private static final String SPRING_HATEOAS_OBJECT_MAPPER = "_halObjectMapper";

    private static final String REL_PROVIDER_BEAN_NAME = "defaultRelProvider";

    /**
     * Obtains the Spring Hateos Object Mapper so that we can apply SCDF Batch Mixins
     * to ignore the JobExecution in StepExecution to prevent infinite loop.
     * {@see https://github.com/spring-projects/spring-hateoas/issues/333}
     */
    @Autowired
    @Qualifier(SPRING_HATEOAS_OBJECT_MAPPER)
    private ObjectMapper springHateoasObjectMapper;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = springHateoasObjectMapper;
        setupObjectMapper(objectMapper);
        return objectMapper;
    }

    @Bean
    public HttpMessageConverters messageConverters() {
        final ObjectMapper objectMapper = new ObjectMapper();
        setupObjectMapper(objectMapper);
        return new HttpMessageConverters(
                // Prevent default converters
                false,
                // Have Jackson2 converter as the sole converter
                Arrays.<HttpMessageConverter<?>>asList(new MappingJackson2HttpMessageConverter(objectMapper)));
    }

    @Bean
    public WebMvcConfigurer configurer() {
        return new WebMvcConfigurerAdapter() {

            @Override
            public void configurePathMatch(PathMatchConfigurer configurer) {
                configurer.setUseSuffixPatternMatch(false);
            }
        };
    }

    private void setupObjectMapper(ObjectMapper objectMapper) {
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(new ISO8601DateFormatWithMilliSeconds());
        objectMapper.addMixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
        objectMapper.addMixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
    }

    @Bean
    public BeanPostProcessor relProviderOverridingBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                // Override the RelProvider to DefaultRelProvider
                // Since DataFlow UI expects DefaultRelProvider to be used, override any other instance of
                // DefaultRelProvider (EvoInflectorRelProvider for instance) with the DefaultRelProvider.
                if (beanName != null && beanName.equals(REL_PROVIDER_BEAN_NAME)) {
                    return new DefaultRelProvider();
                }
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String s) throws BeansException {
                return bean;
            }
        };
    }

	@Bean
	public MethodValidationPostProcessor methodValidationPostProcessor() {
		return new MethodValidationPostProcessor();
	}

}
