/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose.connection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.function.Function;

import javax.net.ssl.SSLHandshakeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailure;

public class DockerPort {

    private static final Logger log = LoggerFactory.getLogger(DockerPort.class);

    private final String ip;
    private final PortMapping portMapping;

    public DockerPort(String ip, int externalPort, int internalPort) {
        this(ip, new PortMapping(externalPort, internalPort));
    }

    public DockerPort(String ip, PortMapping portMapping) {
        this.ip = ip;
        this.portMapping = portMapping;
    }

    public String getIp() {
        return ip;
    }

    public int getExternalPort() {
        return portMapping.getExternalPort();
    }

    public int getInternalPort() {
        return portMapping.getInternalPort();
    }

    public boolean isListeningNow() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, getExternalPort()), 500);
            log.trace("External Port '{}' on ip '{}' was open", getExternalPort(), ip);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isHttpResponding(Function<DockerPort, String> urlFunction, boolean andCheckStatus) {
        return isHttpRespondingSuccessfully(urlFunction, andCheckStatus).succeeded();
    }

    public SuccessOrFailure isHttpRespondingSuccessfully(Function<DockerPort, String> urlFunction, boolean andCheckStatus) {
        URL url;
        try {
            String urlString = urlFunction.apply(this);
            log.trace("Trying to connect to {}", urlString);
            url = URI.create(urlString).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not create URL for connecting to localhost", e);
        }
        try {
            url.openConnection().connect();
            url.openStream().read();
            log.debug("Http connection acquired, assuming port active");
            return SuccessOrFailure.success();
        } catch (SocketException e) {
            return SuccessOrFailure.failureWithCondensedException("Failed to acquire http connection, assuming port inactive", e);
        } catch (FileNotFoundException e) {
            return SuccessOrFailure.fromBoolean(!andCheckStatus, "Received 404, assuming port inactive: " + e.getMessage());
        } catch (SSLHandshakeException e) {
            return SuccessOrFailure.failureWithCondensedException("Received bad SSL response, assuming port inactive", e);
        } catch (IOException e) {
            return SuccessOrFailure.failureWithCondensedException("Error acquiring http connection, assuming port open but inactive", e);
        }
    }

    /**
     * Formats the docker port into a particular form.
     * <p>
     *     Example: dockerPort.inFormat("https://$HOST:$EXTERNAL_PORT/api")
     * </p>
     * Available options are:
     * <ul>
     *     <li>$HOST - the hostname/ip address of the docker port</li>
     *     <li>$EXTERNAL_PORT - the external version of the docker port</li>
     *     <li>$INTERNAL_PORT - the internal version of the docker port</li>
     * </ul>
     *
     * @param format a format string using the substitutions listed above
     * @return formattedDockerPort the details of the {@link DockerPort} in the specified format
     */
    public String inFormat(String format) {
        return format
                .replaceAll("\\$HOST", getIp())
                .replaceAll("\\$EXTERNAL_PORT", String.valueOf(getExternalPort()))
                .replaceAll("\\$INTERNAL_PORT", String.valueOf(getInternalPort()));

    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, portMapping);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DockerPort other = (DockerPort) obj;
        return Objects.equals(ip, other.ip)
                && Objects.equals(portMapping, other.portMapping);
    }

    @Override
    public String toString() {
        return "DockerPort [ip=" + ip + ", portMapping=" + portMapping + "]";
    }
}
