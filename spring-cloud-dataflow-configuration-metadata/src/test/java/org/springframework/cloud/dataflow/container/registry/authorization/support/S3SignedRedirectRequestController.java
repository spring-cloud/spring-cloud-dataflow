/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.container.registry.authorization.support;

import java.util.Collections;
import java.util.Map;

import org.springframework.cloud.dataflow.container.registry.ContainerRegistryProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Adam J. Weigold
 * @author Corneil du Plessis
 */
@RestController
public class S3SignedRedirectRequestController {

	@RequestMapping("/service/token")
	public ResponseEntity<Map<String, String>> getToken() {
		return new ResponseEntity<>(Collections.singletonMap("token", "my_token_999"), HttpStatus.OK);
	}

	@RequestMapping("/v2/test/s3-redirect-image/manifests/1.0.0")
	public ResponseEntity<Resource> getManifests(@RequestHeader("Authorization") String token) {
		if (!"bearer my_token_999".equals(token.trim().toLowerCase())) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		return buildFromString("{\"config\": {\"digest\": \"signed_redirect_digest\"} }");
	}

	@RequestMapping("/v2/test/s3-redirect-image/blobs/signed_redirect_digest")
	public ResponseEntity<Map<String, String>> getBlobRedirect(@RequestHeader("Authorization") String token) {
		if (!"bearer my_token_999".equals(token.trim().toLowerCase())) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		HttpHeaders redirectHeaders = new HttpHeaders();
		redirectHeaders.add(HttpHeaders.LOCATION, "/test/docker/registry/v2/blobs/test/data" +
				"?X-Amz-Algorithm=AWS4-HMAC-SHA256" +
				"&X-Amz-Credential=test" +
				"&X-Amz-Date=test" +
				"&X-Amz-Expires=1200" +
				"&X-Amz-SignedHeaders=host" +
				"&X-Amz-Signature=test");

		return new ResponseEntity<>(redirectHeaders, HttpStatus.TEMPORARY_REDIRECT);
	}

	@RequestMapping("/test/docker/registry/v2/blobs/test/data")
	public ResponseEntity<Resource> getSignedBlob(@RequestHeader Map<String, String> headers) {
		if (!headers.containsKey("authorization")) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		return buildFromString("{\"config\": {\"Labels\": {\"foo\": \"bar\"} } }");
	}

	private ResponseEntity<Resource> buildFromString(String body) {
		ByteArrayResource resource = new ByteArrayResource(body.getBytes());
		return ResponseEntity.ok()
				.contentLength(body.length())
				.contentType(MediaType.valueOf(ContainerRegistryProperties.DOCKER_IMAGE_MANIFEST_MEDIA_TYPE))
				.body(resource);
	}
}
