SPI implementation for deploying [Spring Cloud Stream](https://github.com/spring-cloud/spring-cloud-stream) modules to [Cloud Foundry](http://cloudfoundry.org)

The external dependencies are environment variables declared (but not all
defined) in `resources/application.yml`:

```
  cloudfoundry.api.endpoint
  cloudfoundry.moduleApplication = path/to/moduleLauncher.jar  // on classpath
  cloudfoundry.organization
  cloudfoundry.space
  security.oauth2.client.access-token-uri
  security.oauth2.client.client-id
  security.oauth2.client.client-secret
  security.oauth2.client.grantType=password
  security.oauth2.client.id=cf
  security.oauth2.client.password
  security.oauth2.client.user-authorization-uri
  security.oauth2.client.username
  security.oauth2.resource.userInfoUri
```

there is also a Java system variable that needs to be defined
(`javax.net.ssl.trustStore`) which identifies a truststore for certificates
used in the authentication protocol.
