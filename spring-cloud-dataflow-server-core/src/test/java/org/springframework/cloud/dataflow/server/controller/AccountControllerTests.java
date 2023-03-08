/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.Account;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.AccountRepository;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Michael Minella
 * @author Mark Fisher
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class AccountControllerTests {

	@Autowired
	AccountRepository accountRepository;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();

	}

	@Test
	public void testGet() throws Exception {
		assertThat(accountRepository.count()).isZero();
		insertAccountInDatabase("accountName1");
		insertAccountInDatabase("accountName2");
		mockMvc.perform(get("/accounts").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		cleanRepository();
	}

	@Test
	public void testSave() throws Exception {
		assertThat(accountRepository.count()).isZero();
		mockMvc.perform(post("/accounts").content("{\"accountName\":\"accountName\"}").contentType("application/json")
				.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());

		assertThat(accountRepository.count()).isEqualTo(1);
		cleanRepository();
	}

	@Test
	public void testDelete() throws Exception {
		assertThat(accountRepository.count()).isZero();
		mockMvc.perform(delete("/accounts/accountName").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isInternalServerError());
		insertAccountInDatabase("accountName");
		assertThat(accountRepository.count()).isEqualTo(1);
		mockMvc.perform(delete("/accounts/accountName").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		assertThat(accountRepository.count()).isZero();
		cleanRepository();
	}

	@Test
	public void testDetail() throws Exception {
		assertThat(accountRepository.count()).isZero();
		mockMvc.perform(get("/accounts/accountName").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isInternalServerError());
		insertAccountInDatabase("accountName");
		mockMvc.perform(get("/accounts/accountName").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		assertThat(accountRepository.count()).isEqualTo(1);
		cleanRepository();
	}

	private void insertAccountInDatabase(String accountName) {
		this.accountRepository.save(new Account(accountName, ""));
	}

	private void cleanRepository() {
		this.accountRepository.deleteAll();
	}

}
