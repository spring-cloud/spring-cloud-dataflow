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
package org.springframework.cloud.common.security.support;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.FixedAuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.FixedPrincipalExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import java.util.Base64;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extension of {@link DefaultTokenServices} that provides the functionality of the {@link RemoteTokenServices}
 * to validate a passed Access Token from a remote OAuth Server (introspection).
 *
 * @author Gunnar Hillert
 *
 */
public class TokenValidatingUserInfoTokenServices extends DefaultTokenServices {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Need access to {@link TokenStore}, unfortunately it is private in {@link DefaultTokenServices}.
	 */
	private TokenStore tokenStore;

	/**
	 * Need access to {@link ClientDetailsService}, unfortunately it is private in {@link DefaultTokenServices}.
	 */
	private ClientDetailsService clientDetailsService;

	private final String userInfoEndpointUrl;
	private final String tokenInfoUri;

	private final String clientId;
	private final String clientSecret;

	private OAuth2RestOperations restTemplate;

	/**
	 * See also {@link RemoteTokenServices}. Property name to set on a post request to
	 * introspect an OAuth Token. Defaults to {@code token}.
	 *
	 * For UAA see: https://docs.cloudfoundry.org/api/uaa/version/4.26.0/index.html#introspect-token
	 */
	private String tokenName = "token";

	private AuthoritiesExtractor authoritiesExtractor = new FixedAuthoritiesExtractor();

	private PrincipalExtractor principalExtractor = new FixedPrincipalExtractor();

	private RestOperations remoteTokenRestTemplate;
	private AccessTokenConverter tokenConverter = new DefaultAccessTokenConverter();

	/**
	 * Initialize the DataFlowUserInfoTokenServices.
	 *
	 * @param userInfoEndpointUrl Must not be empty
	 * @param tokenInfoUri Must not be empty
	 * @param clientId Must not be empty
	 * @param clientSecret Must not be empty
	 */
	public TokenValidatingUserInfoTokenServices(
			String userInfoEndpointUrl, String tokenInfoUri,
			String clientId, String clientSecret) {

		Assert.hasText(userInfoEndpointUrl, "The userInfoEndpointUrl must be set.");
		Assert.hasText(tokenInfoUri, "The tokenInfoUri must be set.");
		Assert.hasText(clientId, "The clientId must be set.");
		Assert.hasText(clientSecret, "The clientSecret must be set.");

		this.userInfoEndpointUrl = userInfoEndpointUrl;
		this.tokenInfoUri = tokenInfoUri;
		this.clientId = clientId;
		this.clientSecret = clientSecret;

		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.USE_LONG_FOR_INTS, true);
		final RestTemplate localRestTemplate = new RestTemplate();

		final MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
		messageConverter.setPrettyPrint(false);

		messageConverter.setObjectMapper(objectMapper);
		localRestTemplate.getMessageConverters().removeIf(
			m -> m.getClass().getName().equals(MappingJackson2HttpMessageConverter.class.getName()));
		localRestTemplate.getMessageConverters().add(messageConverter);

		localRestTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
				if (response.getRawStatusCode() != 400) {
					super.handleError(response);
				}
			}
		});
		this.remoteTokenRestTemplate = localRestTemplate;
	}

	public void setRestTemplate(OAuth2RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void setAuthoritiesExtractor(AuthoritiesExtractor authoritiesExtractor) {
		Assert.notNull(authoritiesExtractor, "AuthoritiesExtractor must not be null");
		this.authoritiesExtractor = authoritiesExtractor;
	}

	public void setPrincipalExtractor(PrincipalExtractor principalExtractor) {
		Assert.notNull(principalExtractor, "PrincipalExtractor must not be null");
		this.principalExtractor = principalExtractor;
	}

	@Override
	public OAuth2Authentication loadAuthentication(String accessTokenValue)
			throws AuthenticationException, InvalidTokenException {

		boolean needToreload = false;

		final OAuth2AccessToken accessToken = this.tokenStore.readAccessToken(accessTokenValue);
		if (accessToken == null) {
			needToreload = true;
		}
		else if (accessToken.isExpired()) {
			tokenStore.removeAccessToken(accessToken);
			throw new InvalidTokenException("Access token expired: " + accessTokenValue);
		}

		final OAuth2Authentication authenticationToreturn;

		if (needToreload) {
			final OAuth2Authentication authentication = retrieveOAuth2AuthenticationFromOAuthServer(accessTokenValue);
			authenticationToreturn = authentication;
		}
		else {
			final OAuth2Authentication authenticationFromTokenStore = tokenStore.readAuthentication(accessToken);
			if (authenticationFromTokenStore == null) {
				// in case of race condition
				throw new InvalidTokenException("Invalid access token: " + accessTokenValue);
			}
			if (clientDetailsService != null) {
				String clientId = authenticationFromTokenStore.getOAuth2Request().getClientId();
				try {
					clientDetailsService.loadClientByClientId(clientId);
				}
				catch (ClientRegistrationException e) {
					throw new InvalidTokenException("Client not valid: " + clientId, e);
				}
			}
			authenticationToreturn = authenticationFromTokenStore;
		}

		return authenticationToreturn;
	}

	private OAuth2Authentication retrieveOAuth2AuthenticationFromOAuthServer(String accessTokenValue) {

		// First we need to validate the token

		final OAuth2AccessToken remoteOAuth2AccessToken = retrieveAccessTokenFromOAuthServer(accessTokenValue);
		this.restTemplate.getOAuth2ClientContext().setAccessToken(remoteOAuth2AccessToken);

		// Now let's update the User Information
		final Map<String, Object> map = getUserInfoMap(this.userInfoEndpointUrl);
		if (map.containsKey("error")) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("userinfo returned error: " + map.get("error"));
			}
			throw new InvalidTokenException(accessTokenValue);
		}
		final OAuth2Authentication authentication = extractAuthentication(map);
		this.tokenStore.storeAccessToken(remoteOAuth2AccessToken, authentication);
		return authentication;
	}

	private OAuth2Authentication extractAuthentication(Map<String, Object> map) {
		Object principal = getPrincipal(map);
		List<GrantedAuthority> authorities = this.authoritiesExtractor
				.extractAuthorities(map);
		OAuth2Request request = new OAuth2Request(null, this.clientId, null, true, null,
				null, null, null, null);
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
				principal, "N/A", authorities);
		token.setDetails(map);
		return new OAuth2Authentication(request, token);
	}

	/**
	 * Return the principal that should be used for the token. The default implementation
	 * delegates to the {@link PrincipalExtractor}.
	 * @param map the source map
	 * @return the principal or {@literal "unknown"}
	 */
	protected Object getPrincipal(Map<String, Object> map) {
		Object principal = this.principalExtractor.extractPrincipal(map);
		return (principal == null ? "unknown" : principal);
	}

	@Override
	public OAuth2AccessToken readAccessToken(String accessToken) {
		throw new UnsupportedOperationException("Not supported: read access token");
	}

	@SuppressWarnings({ "unchecked" })
	private Map<String, Object> getUserInfoMap(String userInfoEndpointUrl) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Getting user info from: " + userInfoEndpointUrl);
		}
		try {
			return restTemplate.getForEntity(userInfoEndpointUrl, Map.class).getBody();
		}
		catch (Exception ex) {
			this.logger.warn("Could not fetch user details: " + ex.getClass() + ", "
					+ ex.getMessage());
			return Collections.<String, Object>singletonMap("error",
					"Could not fetch user details");
		}
	}

	/**
	 *
	 * This method will take a received accessToken and call the introspection endpoint of the
	 * OAuth provider to validate the token and retrieved the associated scopes associated with the token.
	 *
	 * Keep in mind that introspection is not standardized by the specs:
	 *
	 * https://stackoverflow.com/questions/12296017/how-to-validate-an-oauth-2-0-access-token-for-a-resource-server
	 *
	 * <ul>
	 * <li>https://tools.ietf.org/html/rfc7662
	 * <li>https://docs.cloudfoundry.org/api/uaa/version/4.26.0/index.html#introspect-token
	 * <li>https://github.com/spring-projects/spring-security-oauth/issues/1558
	 * </ul>
	 *
	 * In your OAuth Configuration, please make sure to provide the following properties:
	 *
	 * security.oauth2.resource.userInfoUri=http://localhost:8080/uaa/userinfo
     * security.oauth2.resource.tokenInfoUri: http://localhost:8080/uaa/check_token
     *
     * see also ResourceServerTokenServicesConfiguration
     *
	 * @param accessToken
	 * @return
	 */
	protected OAuth2AccessToken retrieveAccessTokenFromOAuthServer(String accessToken) {
		Assert.hasText(accessToken, "accessToken must not be null or empty.");
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
		formData.add(tokenName, accessToken);
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", getAuthorizationHeader(this.clientId, this.clientSecret));

		Map<String, Object> map = postForMap(this.tokenInfoUri, formData, headers);

		if (map.containsKey("error")) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("userinfo returned error: " + map.get("error"));
			}
			throw new InvalidTokenException(accessToken);
		}

		return this.tokenConverter.extractAccessToken(accessToken, map);
	}

	/**
	 *
	 * Copied from {@link RemoteTokenServices}.
	 *
	 * @param clientId
	 * @param clientSecret
	 * @return
	 *
	 */
	protected String getAuthorizationHeader(String clientId, String clientSecret) {

		if(clientId == null || clientSecret == null) {
			logger.warn("Null Client ID or Client Secret detected. Endpoint that requires authentication will reject request with 401 error.");
		}

		String creds = String.format("%s:%s", clientId, clientSecret);
		try {
			return "Basic " + new String(Base64.getEncoder().encode(creds.getBytes("UTF-8")));
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Could not convert String");
		}
	}

	/**
	 *
	 * Copied from {@link RemoteTokenServices}.
	 *
	 * @param path
	 * @param formData
	 * @param headers
	 *
	 */
	protected Map<String, Object> postForMap(String path, MultiValueMap<String, String> formData, HttpHeaders headers) {
		if (headers.getContentType() == null) {
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> result = this.remoteTokenRestTemplate.exchange(path, HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, String>>(formData, headers), Map.class).getBody();
		return result;
	}

	/**
	 * The persistence strategy for token storage.
	 *
	 * @param tokenStore the store for access and refresh tokens.
	 */
	public void setTokenStore(TokenStore tokenStore) {
		Assert.notNull(tokenStore, "tokenStore cannot be null.");
		super.setTokenStore(tokenStore);
		this.tokenStore = tokenStore;
	}

	/**
	 * The client details service to use for looking up clients (if necessary). Optional if the access token expiration is
	 * set globally via {@link #setAccessTokenValiditySeconds(int)}.
	 *
	 * @param clientDetailsService the client details service
	 */
	public void setClientDetailsService(ClientDetailsService clientDetailsService) {
		Assert.notNull(clientDetailsService, "clientDetailsService cannot be null.");
		super.setClientDetailsService(clientDetailsService);
		this.clientDetailsService = clientDetailsService;
	}

	/**
	 * See also {@link RemoteTokenServices}. Property name to set on a post request to
	 * introspect an OAuth Token. Defaults to {@code token}.
	 *
	 * For UAA see: https://docs.cloudfoundry.org/api/uaa/version/4.26.0/index.html#introspect-token
	 */
	public void setTokenName(String tokenName) {
		Assert.hasText(tokenName, "tokenName cannot be null nor empty.");
		this.tokenName = tokenName;
	}

}
