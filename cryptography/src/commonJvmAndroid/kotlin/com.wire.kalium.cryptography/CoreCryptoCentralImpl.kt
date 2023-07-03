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

import com.wire.crypto.*
import java.io.File


private class Callbacks : CoreCryptoCallbacks {

    override fun authorize(conversationId: List<UByte>, clientId: List<UByte>): Boolean = true

    override fun userAuthorize(
        conversationId: ConversationId,
        externalClientId: ClientId,
        existingClients: List<ClientId>
    ): Boolean = true

    override fun clientIsExistingGroupUser(
        conversationId: ConversationId,
        clientId: ClientId,
        existingClients: List<ClientId>,
        parentConversationClients: List<ClientId>?
    ): Boolean = true
}

@Suppress("TooManyFunctions")
actual class CoreCryptoCentralImpl actual constructor(
    rootDir: String, databaseKey: String
) : CoreCryptoCentral {

    private val path: String = "$rootDir/$KEYSTORE_NAME"
    val cc: CoreCrypto

    init {
        File(rootDir).mkdirs()
        cc = CoreCrypto.deferredInit(path, databaseKey, DEFAULT_CIPHERSUITES)
        cc.setCallbacks(Callbacks())
    }

    override fun mlsClient(clientId: String): MLSClient = MLSClientImpl(this).apply { mlsInit(clientId) }

    override fun e2eiNewEnrollment(
        clientId: String,
        displayName: String,
        handle: String,
    ): E2EIClient {
        return E2EIClientImpl(cc.e2eiNewEnrollment(clientId, displayName, handle, DEFAULT_E2EI_EXPIRY_DATE, DEFAULT_CIPHERSUITE))
    }

    override fun e2eiMlsClient(enrollment: E2EIClient, certificateChain: String): MLSClient {
        cc.e2eiMlsInit((enrollment as E2EIClientImpl).wireE2eIdentity, certificateChain)
        return MLSClientImpl(this)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun e2eiEnrollmentStash(enrollment: E2EIClient): EnrollmentHandle {
        return cc.e2eiEnrollmentStash((enrollment as E2EIClientImpl).wireE2eIdentity).toUByteArray().asByteArray()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun e2eiEnrollmentStashPop(handle: EnrollmentHandle): E2EIClient {
        return E2EIClientImpl(cc.e2eiEnrollmentStashPop(handle.asUByteArray().asList()))
    }

    companion object {
        const val KEYSTORE_NAME = "keystore"
        fun CiphersuiteName.lower() = (ordinal + 1).toUShort()
        val DEFAULT_CIPHERSUITE = CiphersuiteName.MLS_128_DHKEMX25519_AES128GCM_SHA256_ED25519.lower()
        val DEFAULT_CIPHERSUITES = listOf(DEFAULT_CIPHERSUITE)
        val DEFAULT_E2EI_EXPIRY_DATE: UInt = 90U
    }
}

