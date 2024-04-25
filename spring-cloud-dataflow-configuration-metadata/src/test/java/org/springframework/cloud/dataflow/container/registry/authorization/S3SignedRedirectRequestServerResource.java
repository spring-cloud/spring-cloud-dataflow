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

package org.springframework.cloud.dataflow.container.registry.authorization;


import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.dataflow.container.registry.authorization.support.S3SignedRedirectRequestServerApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.SocketUtils;

/**
 * @author Adam J. Weigold
 */
public class S3SignedRedirectRequestServerResource implements BeforeEachCallback, AfterEachCallback {

    private static final Logger logger = LoggerFactory.getLogger(S3SignedRedirectRequestServerResource.class);

    private int s3SignedRedirectServerPort;

    private ConfigurableApplicationContext application;


	@Override
	public void beforeEach(ExtensionContext context) throws Exception {

        this.s3SignedRedirectServerPort = SocketUtils.findAvailableTcpPort();

        logger.info("Setting S3 Signed Redirect Server port to " + this.s3SignedRedirectServerPort);

        // Docker requires HTTPS.  Generated ssl keypair as follows:
        // `keytool -genkeypair -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore s3redirectrequestserver.p12 -validity 1000000`
        this.application = new SpringApplicationBuilder(S3SignedRedirectRequestServerApplication.class).build()
                .run("--server.port=" + s3SignedRedirectServerPort,
                        "--server.ssl.key-store=classpath:s3redirectrequestserver.p12",
                        "--server.ssl.key-store-password=foobar");
        logger.info("S3 Signed Redirect Server Server is UP!");
    }

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		application.stop();
    }

    public int getS3SignedRedirectServerPort() {
        return s3SignedRedirectServerPort;
    }


}
