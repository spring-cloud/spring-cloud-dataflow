package org.springframework.cloud.dataflow.server.controller;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.module.metrics.AggregateCounterRepository;
import org.springframework.cloud.stream.module.metrics.memory.InMemoryAggregateCounterRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Alex Boyko
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AggregateCounterControllerTests.Config.class})
@WebAppConfiguration
public class AggregateCounterControllerTests {


    @Autowired
    private AggregateCounterRepository repository;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setupMockMVC() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
                get("/").accept(MediaType.APPLICATION_JSON)).build();
    }

    @After
    public void cleanUp() {
        List<String> counters = new ArrayList<>();
        for (String s : repository.list()) {
            counters.add(s);
        }
        for (String counter : counters) {
            repository.reset(counter);
        }
    }

    @Test
    public void testAggregateCountersListing() throws Exception {
        repository.increment("foo", 1L, DateTime.now());
        repository.increment("bar", 1L, DateTime.now());
        mockMvc.perform(
                get("/metrics/aggregate-counters").accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(2)))
                .andExpect(jsonPath("$.content.*.name", containsInAnyOrder("foo", "bar")))
        ;
    }

    @Test
    public void testGetAndDelete() throws Exception {
        repository.increment("foo", 1L, DateTime.now());
        mockMvc.perform(
                get("/metrics/aggregate-counters/foo").accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.name", is("foo")))
                .andExpect(jsonPath("$.values").isMap())
        ;

        mockMvc.perform(
                delete("/metrics/aggregate-counters/foo").accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
        ;

//        mockMvc.perform(
//                get("/metrics/aggregate-counters/foo").accept(MediaType.APPLICATION_JSON)
//        )
//                .andExpect(status().isNotFound())
//        ;
        mockMvc.perform(
                get("/metrics/aggregate-counters").accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
        ;

    }


    @Configuration
    @EnableSpringDataWebSupport
    @EnableHypermediaSupport(type = HAL)
    @EnableWebMvc
    public static class Config {

        @Bean
        public AggregateCounterRepository aggregateCounterRepository() {
            return new InMemoryAggregateCounterRepository();
        }

        @Bean
        public AggregateCounterController aggregateCounterController() {
            return new AggregateCounterController(aggregateCounterRepository());
        }

    }

}
