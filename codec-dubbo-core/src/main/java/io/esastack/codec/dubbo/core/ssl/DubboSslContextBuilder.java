/*
 * Copyright 2021 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.esastack.codec.dubbo.core.ssl;

import esa.commons.StringUtils;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

/**
 * TSL启动参数
 */
public class DubboSslContextBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DubboSslContextBuilder.class);

    /**
     * X.509 certificate chain InputStream in PEM format
     */
    private InputStream certificate;

    /**
     * PKCS#8 private key InputStream in PEM format
     */
    private InputStream privateKey;

    /**
     * cert password
     */
    private String keyPassword;

    /**
     * X.509 trusted certificate collections InputStream in PEM format
     */
    private InputStream trustCertificates;

    /**
     * SSL CipherSuites
     */
    private String[] ciphers;

    /**
     * SSL Protocol version
     */
    private String[] enabledProtocols;

    /**
     * SSL handshake Timeout
     */
    private long handshakeTimeoutMillis = 3000;

    /**
     * Returns OpenSSL if available, otherwise returns the JDK provider.
     */
    private static SslProvider findSslProvider() {
        if (OpenSsl.isAvailable()) {
            logger.info("Using OPENSSL provider.");
            return SslProvider.OPENSSL;
        } else if (checkJdkProvider()) {
            logger.info("Using JDK provider.");
            return SslProvider.JDK;
        }
        throw new IllegalStateException(
                "Could not find any valid TLS provider, please check your dependency or deployment environment, " +
                        "usually netty-tcnative, Conscrypt, or Jetty NPN/ALPN is needed.");
    }

    private static boolean checkJdkProvider() {
        Provider[] jdkProviders = Security.getProviders("SSLContext.TLS");
        return (jdkProviders != null && jdkProviders.length > 0);
    }

    public SslContext buildServer() throws IOException {

        if (certificate == null) {
            throw new IllegalArgumentException("[TLS]certificate must be set， please check builder code");
        }

        if (privateKey == null) {
            throw new IllegalArgumentException("[TLS]privateKey must be set.please check builder code");
        }

        SslContextBuilder sslContextBuilder;
        if (StringUtils.isBlank(keyPassword)) {
            sslContextBuilder = SslContextBuilder.forServer(certificate, privateKey);
        } else {
            sslContextBuilder = SslContextBuilder.forServer(certificate, privateKey, keyPassword);
        }
        sslContextBuilder.sslProvider(findSslProvider());
        if (trustCertificates != null) {
            sslContextBuilder.trustManager(trustCertificates);
        }
        sslContextBuilder.clientAuth(ClientAuth.OPTIONAL);

        if (ciphers != null && ciphers.length > 0) {
            sslContextBuilder.ciphers(Arrays.asList(ciphers));
        }

        return sslContextBuilder.build();
    }

    public SslContext buildClient() throws Exception {

        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        if (certificate != null && privateKey != null) {
            if (StringUtils.isBlank(keyPassword)) {
                sslContextBuilder.keyManager(certificate, privateKey);
            } else {
                sslContextBuilder.keyManager(certificate, privateKey, keyPassword);
            }
        }

        if (trustCertificates != null) {
            sslContextBuilder.trustManager(trustCertificates);
        }

        sslContextBuilder.sslProvider(findSslProvider());

        return sslContextBuilder.build();
    }

    public DubboSslContextBuilder setCertificate(InputStream certificate) {
        this.certificate = certificate;
        return this;
    }

    public DubboSslContextBuilder setPrivateKey(InputStream privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public DubboSslContextBuilder setCiphers(String[] ciphers) {
        this.ciphers = ciphers;
        return this;
    }

    public long getHandshakeTimeoutMillis() {
        return handshakeTimeoutMillis;
    }

    public DubboSslContextBuilder setHandshakeTimeoutMillis(long handshakeTimeoutMillis) {
        this.handshakeTimeoutMillis = handshakeTimeoutMillis;
        return this;
    }

    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }

    public DubboSslContextBuilder setEnabledProtocols(String[] enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
        return this;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public DubboSslContextBuilder setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
        return this;
    }

    public InputStream getTrustCertificates() {
        return trustCertificates;
    }

    public DubboSslContextBuilder setTrustCertificates(InputStream trustCertificates) {
        this.trustCertificates = trustCertificates;
        return this;
    }
}
