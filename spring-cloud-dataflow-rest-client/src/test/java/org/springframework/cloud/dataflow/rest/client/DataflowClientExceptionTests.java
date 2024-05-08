/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.client;

import org.junit.jupiter.api.Test;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.vnderrors.VndErrors;
import org.springframework.hateoas.mediatype.vnderrors.VndErrors.VndError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
public class DataflowClientExceptionTests {

	@Test
	public void testCreationOfDataflowClientExceptionWithNullError() {

		try {
			new DataFlowClientException(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The provided vndErrors parameter must not be null.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testCreationOfDataflowClientExceptionWithSingleError() {
		final VndErrors errors = new VndErrors("foo", "bar message", Link.of("somewhere"));
		final DataFlowClientException dataFlowClientException = new DataFlowClientException(errors);
		assertThat(dataFlowClientException.getMessage()).isEqualTo("bar message");
	}

	@Test
	public void testCreationOfDataflowClientExceptionWithMultipleErrors() {
		final VndError vndError1 = new VndError("foo logref", "foo message", Link.of("foo link"));
		final VndError vndError2 = new VndError("bar logref", "bar message", Link.of("bar link"));

		final VndErrors errors = new VndErrors(vndError1, vndError2);
		final DataFlowClientException dataFlowClientException = new DataFlowClientException(errors);
		assertThat(dataFlowClientException.getMessage()).isEqualTo("foo message\nbar message");
	}
}
