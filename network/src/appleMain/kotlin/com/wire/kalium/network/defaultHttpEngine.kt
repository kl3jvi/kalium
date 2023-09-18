/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.kalium.network

import com.wire.kalium.network.api.base.model.ProxyCredentialsDTO
import com.wire.kalium.network.session.CertificatePinning
import com.wire.kalium.network.tools.ServerConfigDTO
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.certificates.CertificatePinner

actual fun defaultHttpEngine(
    serverConfigDTOApiProxy: ServerConfigDTO.ApiProxy?,
    proxyCredentials: ProxyCredentialsDTO?,
    ignoreSSLCertificates: Boolean,
    certificatePinning: CertificatePinning
): HttpClientEngine {
    if (serverConfigDTOApiProxy != null) {
        throw IllegalArgumentException("Proxy is not supported on iOS")
    }
    if (proxyCredentials != null) {
        throw IllegalArgumentException("Proxy is not supported on iOS")
    }

    return Darwin.create {
        pipelining = true

        if (certificatePinning.isNotEmpty()) {
            val certPinner: CertificatePinner = CertificatePinner.Builder().apply {
                certificatePinning.forEach { (cert, hosts) ->
                    hosts.forEach { host ->
                        add(host, cert)
                    }
                }
            }.build()
            handleChallenge(certPinner)
        }

    }
}
