/*
 * Copyright 2018-2022 the original author or authors.
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

import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.cloud.dataflow.shell.command.support.ShellUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
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
 * @author Chris Bono
 */
@ShellComponent
public class HttpCommands {

	private static final String DEFAULT_MEDIA_TYPE = MediaType.TEXT_PLAIN_VALUE;

	private static final String POST_HTTPSOURCE = "http post";

	private static final String GET_HTTPSOURCE = "http get";

	@ShellMethod(key = POST_HTTPSOURCE, value = "POST data to http endpoint")
	public String postHttp(
			@ShellOption(value = { "", "--target" }, help = "the location to post to", defaultValue = Target.DEFAULT_TARGET) String target,
			@ShellOption(help = "the text payload to post. exclusive with file. "
					+ "embedded double quotes are not supported if next to a space character", defaultValue = ShellOption.NULL) String data,
			@ShellOption(help = "filename to read data from. exclusive with data", defaultValue = ShellOption.NULL) File file,
			@ShellOption(value = "--contentType", help = "the content-type to use. file is also read using the specified charset",
					defaultValue = DEFAULT_MEDIA_TYPE) MediaType mediaType,
			@ShellOption(help = "the username for calls that require basic "
					+ "authentication", defaultValue = Target.DEFAULT_USERNAME) String username,
			@ShellOption(help = "the password for calls that require basic authentication", 
					defaultValue = Target.DEFAULT_PASSWORD) String password,
			@ShellOption(help = "accept any SSL certificate (even \"self-signed)\"", defaultValue = "false") boolean skipSslValidation)
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

			restTemplate.setRequestFactory(HttpClientConfigurer.create(requestURI)
					.basicAuthCredentials(username, password)
					.skipTlsCertificateVerification(skipSslValidation)
					.buildClientHttpRequestFactory());

			ResponseEntity<String> response = restTemplate.postForEntity(requestURI, request, String.class);
			outputResponse(response, buffer);
			if (!response.getStatusCode().is2xxSuccessful()) {
				buffer.append(System.lineSeparator())
						.append(String.format("Error sending data '%s' to '%s'", data, target));
			}
			return buffer.toString();
		}
		catch (ResourceAccessException e) {
			return String.format(buffer + "Failed to access http endpoint %s", target);
		}
		catch (Exception e) {
			return String.format(buffer + "Failed to send data to http endpoint %s", target);
		}
	}

	@ShellMethod(key = GET_HTTPSOURCE, value = "Make GET request to http endpoint")
	public String getHttp(
			@ShellOption(value = { "", "--target" }, help = "the URL to make the request to",
					defaultValue = Target.DEFAULT_TARGET) String target,
			@ShellOption(help = "the username for calls that require basic "
					+ "authentication", defaultValue = Target.DEFAULT_USERNAME) String username,
			@ShellOption(help = "the password for calls that require basic authentication",
					defaultValue = Target.DEFAULT_PASSWORD) String password,
			@ShellOption(help = "accept any SSL certificate (even \"self-signed)\"", defaultValue = "false") boolean skipSslValidation) {
		
		final StringBuilder buffer = new StringBuilder();
		URI requestURI = URI.create(target);

		try {
			outputRequest("GET", requestURI, null, "", buffer);

			final RestTemplate restTemplate = createRestTemplate(buffer);

			restTemplate.setRequestFactory(HttpClientConfigurer.create(requestURI)
					.basicAuthCredentials(username, password)
					.skipTlsCertificateVerification(skipSslValidation)
					.buildClientHttpRequestFactory());

			ResponseEntity<String> response = restTemplate.getForEntity(requestURI, String.class);
			outputResponse(response, buffer);
			if (!response.getStatusCode().is2xxSuccessful()) {
				buffer.append(System.lineSeparator()).append(String.format("Error sending request to '%s'", target));
			}
			return buffer.toString();
		}
		catch (ResourceAccessException e) {
			return String.format(buffer + "Failed to access http endpoint %s", target);
		}
		catch (Exception e) {
			return String.format(buffer + "Failed to get data from http endpoint %s", target);
		}
	}

	private RestTemplate createRestTemplate(final StringBuilder buffer) {
		RestTemplate restTemplate = new RestTemplate();

		restTemplate.setErrorHandler(new ResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				HttpStatus status = (HttpStatus) response.getStatusCode();
				return (status == HttpStatus.BAD_GATEWAY || status == HttpStatus.GATEWAY_TIMEOUT
						|| status == HttpStatus.INTERNAL_SERVER_ERROR);
			}

			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
				outputError((HttpStatus)response.getStatusCode(), buffer);
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
		buffer.append(requestUri.toString()).append(" ").append(requestData).append(System.lineSeparator());
	}

	private void outputResponse(ResponseEntity<String> response, StringBuilder buffer) {
		buffer.append("> ").append(response.getStatusCode().value()).append(" ").append(((HttpStatus)response.getStatusCode()).name())
				.append(System.lineSeparator());
		String maybeJson = response.getBody();
		if (maybeJson != null) {
			buffer.append(ShellUtils.prettyPrintIfJson(maybeJson));
		}
	}

	private void outputError(HttpStatus status, StringBuilder buffer) {
		buffer.append("> ").append(status.value()).append(" ").append(status.name()).append(System.lineSeparator());
	}

}
