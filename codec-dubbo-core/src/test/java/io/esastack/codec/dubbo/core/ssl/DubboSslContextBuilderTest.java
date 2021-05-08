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

import io.netty.handler.ssl.SslContext;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class DubboSslContextBuilderTest {

    @Test
    public void buildTest() {
        DubboSslContextBuilder builder = null;
        try {
            builder = getBuilder();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        assertNotNull(builder.getTrustCertificates());
        assertNotNull(builder.getEnabledProtocols());
        assertEquals(1000, builder.getHandshakeTimeoutMillis());
        assertNull(builder.getKeyPassword());
        try {
            SslContext clientCtx = builder.buildClient();
            assertNotNull(clientCtx);
        } catch (Exception e) {
            fail();
        }

        try {
            builder = getBuilder();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        try {
            SslContext serverCtx = builder.buildServer();
            assertNotNull(serverCtx);
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    private DubboSslContextBuilder getBuilder()  throws IOException {
        DubboSslContextBuilder builder = new DubboSslContextBuilder();
        builder.setPrivateKey(loadPrivateKeyInputStream());
        builder.setCertificate(loadCertificateInputStream());
        builder.setTrustCertificates(loadTrustCertificatesInputStream());
        builder.setCiphers(new String[0]);
        builder.setEnabledProtocols(new String[0]);
        builder.setHandshakeTimeoutMillis(1000);
        builder.setKeyPassword(null);
        return builder;
    }

    private InputStream loadPrivateKeyInputStream() throws IOException {
        return new FileInputStream("src/test/resources/tls/private-key.pem");
    }

    private InputStream loadCertificateInputStream() throws IOException {
        return new FileInputStream("src/test/resources/tls/certificate.pem");
    }

    private InputStream loadTrustCertificatesInputStream() throws IOException {
        return new FileInputStream("src/test/resources/tls/trust-certificates.pem");
    }
}
