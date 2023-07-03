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
package com.wire.kalium.cryptography

import com.wire.crypto.CiphersuiteName
import com.wire.crypto.ConversationId
import com.wire.crypto.CoreCrypto
import com.wire.crypto.CoreCryptoCallbacks


@Suppress("TooManyFunctions")
actual class CoreCryptoCentralImpl actual constructor(
    rootDir: String, databaseKey: String
) : CoreCryptoCentral {

    private val path: String = "$rootDir/$KEYSTORE_NAME"
    val cc: CoreCrypto = TODO()


    override fun mlsClient(clientId: String): MLSClient = MLSClientImpl(this).apply { mlsInit(clientId) }

    override fun e2eiNewEnrollment(
        clientId: String,
        displayName: String,
        handle: String,
    ): E2EIClient {
        TODO()
    }

    override fun e2eiMlsClient(enrollment: E2EIClient, certificateChain: String): MLSClient {
        TODO()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun e2eiEnrollmentStash(enrollment: E2EIClient): EnrollmentHandle {
        TODO()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun e2eiEnrollmentStashPop(handle: EnrollmentHandle): E2EIClient {
        TODO()
    }

    companion object {
        const val KEYSTORE_NAME = "keystore"
        fun CiphersuiteName.lower() = (ordinal + 1).toUShort()
        val DEFAULT_CIPHERSUITE = CiphersuiteName.MLS_128_DHKEMX25519_AES128GCM_SHA256_ED25519.lower()
        val DEFAULT_CIPHERSUITES = listOf(DEFAULT_CIPHERSUITE)
        val DEFAULT_E2EI_EXPIRY_DATE: UInt = 90U
    }
}

