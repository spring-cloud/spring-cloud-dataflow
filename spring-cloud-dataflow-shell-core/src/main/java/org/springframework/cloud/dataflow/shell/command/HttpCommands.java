/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Http commands.
 *
 * @author Jon Brisbin
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Eric Bottard
 * @author David Turanski
 */
@Component
public class HttpCommands implements CommandMarker {

	private static final String DEFAULT_MEDIA_TYPE = MediaType.TEXT_PLAIN_VALUE;

	private static final String POST_HTTPSOURCE = "http post";

	private static final String GET_HTTPSOURCE = "http get";

	@CliCommand(value = { POST_HTTPSOURCE }, help = "POST data to http endpoint")
	public String postHttp(
			@CliOption(mandatory = false, key = { "",
					"target" }, help = "the location to post to", unspecifiedDefaultValue = "http://localhost:9393") String target,
			@CliOption(mandatory = false, key = "data", help = "the text payload to post. exclusive with file. "
					+ "embedded double quotes are not supported if next to a space character") String data,
			@CliOption(mandatory = false, key = "file", help = "filename to read data from. exclusive with data") File file,
			@CliOption(mandatory = false, key = "contentType", help = "the content-type to use. file is also read "
					+ "using the specified charset", unspecifiedDefaultValue = DEFAULT_MEDIA_TYPE) MediaType mediaType,
			@CliOption(mandatory = false, key = { "username" }, help = "the username for calls that require basic "
					+ "authentication", unspecifiedDefaultValue = Target.DEFAULT_USERNAME) String targetUsername,
			@CliOption(mandatory = false, key = { "password" }, help = "the password for calls that require basic "
					+ "authentication", specifiedDefaultValue = Target.DEFAULT_SPECIFIED_PASSWORD, unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_PASSWORD) String targetPassword,
			@CliOption(mandatory = false, key = "skip-ssl-validation", help = "accept any SSL certificate (even "
					+ "self-signed)", specifiedDefaultValue = Target.DEFAULT_SPECIFIED_SKIP_SSL_VALIDATION, unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_SKIP_SSL_VALIDATION) boolean skipSslValidation)
			throws IOException {
		Assert.isTrue(file != null || data != null, "One of 'file' or 'data' must be set");
		Assert.isTrue(file == null || data == null, "Only one of 'file' or 'data' must be set");

		if (file != null) {
			InputStreamReader isr = new InputStreamReader(new FileInputStream(file), mediaType.getCharset());
			data = FileCopyUtils.copyToString(isr);
		}

		final StringBuilder buffer = new StringBuilder();
		URI requestURI = URI.create(target);

		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		final HttpEntity<String> request = new HttpEntity<String>(data, headers);

		try {
			outputRequest("POST", requestURI, mediaType, data, buffer);
			final RestTemplate restTemplate = createRestTemplate(buffer);

			restTemplate.setRequestFactory(HttpClientConfigurer.create()
					.targetHost(requestURI)
					.basicAuthCredentials(targetUsername, targetPassword)
					.skipTlsCertificateVerification(skipSslValidation)
					.buildClientHttpRequestFactory());

			ResponseEntity<String> response = restTemplate.postForEntity(requestURI, request, String.class);
			outputResponse(response, buffer);
			if (!response.getStatusCode().is2xxSuccessful()) {
				buffer.append(OsUtils.LINE_SEPARATOR)
						.append(String.format("Error sending data '%s' to '%s'", data, target));
			}
			return buffer.toString();
		}
		catch (ResourceAccessException e) {
			return String.format(buffer.toString() + "Failed to access http endpoint %s", target);
		}
		catch (Exception e) {
			return String.format(buffer.toString() + "Failed to send data to http endpoint %s", target);
		}
	}

	@CliCommand(value = { GET_HTTPSOURCE }, help = "Make GET request to http endpoint")
	public String getHttp(
			@CliOption(mandatory = false, key = { "",
					"target" }, help = "the URL to make the request to", unspecifiedDefaultValue = "http://localhost:9393") String target,
			@CliOption(mandatory = false, key = { "username" }, help = "the username for calls that require basic "
					+ "authentication", unspecifiedDefaultValue = Target.DEFAULT_USERNAME) String targetUsername,
			@CliOption(mandatory = false, key = { "password" }, help = "the password for calls that require basic "
					+ "authentication", specifiedDefaultValue = Target.DEFAULT_SPECIFIED_PASSWORD, unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_PASSWORD) String targetPassword,
			@CliOption(mandatory = false, key = "skip-ssl-validation", help = "accept any SSL certificate (even "
					+ "self-signed)", specifiedDefaultValue = Target.DEFAULT_SPECIFIED_SKIP_SSL_VALIDATION, unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_SKIP_SSL_VALIDATION) boolean skipSslValidation)
			throws IOException {

		final StringBuilder buffer = new StringBuilder();
		URI requestURI = URI.create(target);

		try {
			outputRequest("GET", requestURI, null, "", buffer);

			final RestTemplate restTemplate = createRestTemplate(buffer);

			restTemplate.setRequestFactory(HttpClientConfigurer.create()
					.targetHost(requestURI)
					.basicAuthCredentials(targetUsername, targetPassword)
					.skipTlsCertificateVerification(skipSslValidation)
					.buildClientHttpRequestFactory());

			ResponseEntity<String> response = restTemplate.getForEntity(requestURI, String.class);
			outputResponse(response, buffer);
			if (!response.getStatusCode().is2xxSuccessful()) {
				buffer.append(OsUtils.LINE_SEPARATOR).append(String.format("Error sending request to '%s'", target));
			}
			return buffer.toString();
		}
		catch (ResourceAccessException e) {
			return String.format(buffer.toString() + "Failed to access http endpoint %s", target);
		}
		catch (Exception e) {
			return String.format(buffer.toString() + "Failed to get data from http endpoint %s", target);
		}
	}

	private RestTemplate createRestTemplate(final StringBuilder buffer) {
		RestTemplate restTemplate = new RestTemplate();

		restTemplate.setErrorHandler(new ResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				HttpStatus status = response.getStatusCode();
				return (status == HttpStatus.BAD_GATEWAY || status == HttpStatus.GATEWAY_TIMEOUT
						|| status == HttpStatus.INTERNAL_SERVER_ERROR);
			}

			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
				outputError(response.getStatusCode(), buffer);
			}
		});

		return restTemplate;
	}

	private void outputRequest(String method, URI requestUri, MediaType mediaType, String requestData,
			StringBuilder buffer) {
		buffer.append("> ").append(method).append(' ');
		if (mediaType != null) {
			buffer.append("(").append(mediaType.toString()).append(") ");
		}
		buffer.append(requestUri.toString()).append(" ").append(requestData).append(OsUtils.LINE_SEPARATOR);
	}

	private void outputResponse(ResponseEntity<String> response, StringBuilder buffer) {
		buffer.append("> ").append(response.getStatusCode().value()).append(" ").append(response.getStatusCode().name())
				.append(OsUtils.LINE_SEPARATOR);
		String maybeJson = response.getBody();
		if (maybeJson != null) {
			buffer.append(prettyPrintIfJson(maybeJson));
		}
	}

	private String prettyPrintIfJson(String maybeJson) {
		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(maybeJson, typeRef));
		}
		catch (IOException e) {
			// Not JSON? Return unchanged
			return maybeJson;
		}
	}

	private void outputError(HttpStatus status, StringBuilder buffer) {
		buffer.append("> ").append(status.value()).append(" ").append(status.name()).append(OsUtils.LINE_SEPARATOR);
	}

}
