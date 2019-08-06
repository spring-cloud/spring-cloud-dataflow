/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.single.security.support;

// import java.security.Principal;
// import java.util.Collections;
// import java.util.HashSet;
// import java.util.Map;
// import java.util.Set;

// import org.springframework.beans.factory.ObjectProvider;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
// import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
// import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
// import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
// import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
// import org.springframework.boot.autoconfigure.security.SecurityProperties;
// import org.springframework.boot.autoconfigure.security.oauth2.authserver.AuthorizationServerProperties;
// import org.springframework.boot.autoconfigure.security.oauth2.authserver.OAuth2AuthorizationServerConfiguration;
// import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
// import org.springframework.boot.builder.SpringApplicationBuilder;
// import org.springframework.cloud.dataflow.autoconfigure.local.LocalDataFlowServerAutoConfiguration;
// import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolverAutoConfiguration;
// import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
// import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
// import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Import;
// import org.springframework.core.annotation.Order;
// import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.core.userdetails.User;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
// import org.springframework.security.oauth2.common.OAuth2AccessToken;
// import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
// import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
// import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
// import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
// import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
// import org.springframework.security.oauth2.provider.OAuth2Authentication;
// import org.springframework.security.oauth2.provider.client.BaseClientDetails;
// import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
// import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
// import org.springframework.security.oauth2.provider.token.TokenEnhancer;
// import org.springframework.security.oauth2.provider.token.TokenStore;
// import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
// import org.springframework.security.provisioning.InMemoryUserDetailsManager;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

/**
 * @author Gunnar Hillert
 */
// @RestController
// @SpringBootApplication(
// 	excludeName = {
// 		"org.springframework.cloud.dataflow.shell.autoconfigure.BaseShellAutoConfiguration" },
// 	exclude = {
// 		SessionAutoConfiguration.class,
// 		ManagementWebSecurityAutoConfiguration.class,
// 		LocalDeployerAutoConfiguration.class,
// 		CloudFoundryDeployerAutoConfiguration.class,
// 		KubernetesAutoConfiguration.class,
// 		org.springframework.cloud.kubernetes.KubernetesAutoConfiguration.class,
// 		DataSourceAutoConfiguration.class,
// 		DataSourceTransactionManagerAutoConfiguration.class,
// 		JmxAutoConfiguration.class,
// 		HibernateJpaAutoConfiguration.class,
// 		LocalDataFlowServerAutoConfiguration.class,
// 		ApplicationConfigurationMetadataResolverAutoConfiguration.class,
// 		LocalDeployerAutoConfiguration.class,
// 		IntegrationAutoConfiguration.class })
public class OAuth2TestServer {

	// public static void main(String[] args) {
	// 	new SpringApplicationBuilder(OAuth2TestServer.class)
	// 			.run(
	// 				"--spring.cloud.common.security.enabled=false",
	// 				"--server.port=9999",
	// 				"--logging.level.org.springframework=debug",
	// 				"--spring.cloud.kubernetes.enabled=false",
	// 				"--spring.config.location=classpath:/org/springframework/cloud/dataflow/server/local"
	// 							+ "/security/support/oauth2TestServerConfig.yml");
	// 	new SpringApplicationBuilder(OAuth2TestServer.class)
	// 	.properties("server.port:" + SocketUtils.findAvailableTcpPort()).build()
	// 	.run("--debug --spring.config.location=classpath:/org/springframework/cloud/dataflow/server/local"
	// 	+ "/security/support/oauth2TestServerConfig.yml");
	// }

	// @Autowired
	// ConsumerTokenServices tokenServices;

	// @Bean
	// public TokenStore tokenStore() {
	//     return new InMemoryTokenStore();
	// }

	// @RequestMapping({ "/user", "/me" })
	// public Map<String, String> user(Principal principal) {
	// 	return Collections.singletonMap("name", principal.getName());
	// }

	// @RequestMapping("/revoke_token")
	// public boolean revokeToken() {
	// 	final OAuth2Authentication auth = (OAuth2Authentication) SecurityContextHolder
    //             .getContext().getAuthentication();
    //     final String token = tokenStore().getAccessToken(auth).getValue();
    //     return tokenServices.revokeToken(token);
	// }

	// @Configuration
	// @EnableAuthorizationServer
	// protected static class MyOAuth2AuthorizationServerConfiguration extends OAuth2AuthorizationServerConfiguration {
	// 	public MyOAuth2AuthorizationServerConfiguration(
	// 			BaseClientDetails details,
	// 			AuthenticationConfiguration authenticationConfiguration,
	// 			ObjectProvider<TokenStore> tokenStore,
	// 			ObjectProvider<AccessTokenConverter> tokenConverter,
	// 			AuthorizationServerProperties properties) throws Exception {
	// 		super(details, authenticationConfiguration, tokenStore, tokenConverter, properties);
	// 	}

	// 	@Override
	// 	public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
	// 		super.configure(security);
	// 		security.allowFormAuthenticationForClients();;
	// 	}

	// 	@Override
	// 	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
	// 		super.configure(endpoints);
	// 		endpoints.tokenEnhancer(new TokenEnhancer() {

	// 			@Override
	// 			public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
	// 				if (authentication.getPrincipal() instanceof User) {
	// 					User user = (User) authentication.getPrincipal();

	// 					final Set<String> scopes = new HashSet<String>();
	// 					for (GrantedAuthority authority : user.getAuthorities()) {
	// 						String role = authority.getAuthority();

	// 						if (role.startsWith("ROLE_")) {
	// 							scopes.add(role.substring(5).toLowerCase());
	// 						}
	// 						else {
	// 							scopes.add(role.toLowerCase());
	// 						}
	// 					}
	// 					((DefaultOAuth2AccessToken) accessToken).setScope(scopes);

	// 				}
	// 				return accessToken;
	// 			}
	// 		});
	// 	}
	// }

	// @Configuration
	// @Import(FileSecurityProperties.class)
	// @Order(SecurityProperties.BASIC_AUTH_ORDER)
	// protected static class BasicSecurityConfig extends WebSecurityConfigurerAdapter {
	// 	@Autowired
	// 	private FileSecurityProperties fileSecurityProperties;

	// 	@Bean
	// 	@Override
	// 	public UserDetailsService userDetailsService () {
	// 		final InMemoryUserDetailsManager inMemory =
	// 				new InMemoryUserDetailsManager(this.fileSecurityProperties.getUsers());
	// 		 return inMemory;
	// 	}
	// }

	// @Configuration
	// @EnableResourceServer
	// protected static class ResourceServerConfiguration
	// 		extends ResourceServerConfigurerAdapter {
	// }

}
