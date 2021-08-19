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
package io.esastack.codec.common.ssl;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.AttributeKey;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.security.cert.Certificate;

public class SslUtils {

    public static final AttributeKey<Certificate> TLS_PEER_CERTIFICATE_KEY =
            AttributeKey.newInstance("TLS_PEER_CERTIFICATE");
    private static final Logger LOGGER = LoggerFactory.getLogger(SslUtils.class);

    public static void extractSslPeerCertificate(final Channel channel) {
        final SslHandler handler = channel.pipeline().get(SslHandler.class);
        if (handler == null) {
            LOGGER.info("Could not get SslHandler in TLS channel!");
        } else {
            try {
                Certificate[] certificates = handler.engine().getSession().getPeerCertificates();
                if (certificates != null && certificates.length != 0) {
                    channel.attr(TLS_PEER_CERTIFICATE_KEY).set(certificates[0]);
                }
            } catch (SSLPeerUnverifiedException e) {
                LOGGER.info("There is no client certificate in the tsl session, " +
                        "client may not enable clientAuth: " + e.getMessage());
            }
        }
    }

    public static void extractSslPeerCertificate(final Channel channel,
                                                 final SslHandshakeCompletionEvent evt) {
        final SslHandler handler = channel.pipeline().get(SslHandler.class);
        if (handler == null) {
            LOGGER.info("Could not get SslHandler in TLS channel!");
        } else if (evt.isSuccess()) {
            try {
                Certificate[] certificates = handler.engine().getSession().getPeerCertificates();
                if (certificates != null && certificates.length != 0) {
                    channel.attr(TLS_PEER_CERTIFICATE_KEY).set(certificates[0]);
                }
            } catch (SSLPeerUnverifiedException e) {
                LOGGER.info("There is no client certificate in the tsl session, " +
                        "client may not enable clientAuth: " + e.getMessage());
            }
        }
    }
}

