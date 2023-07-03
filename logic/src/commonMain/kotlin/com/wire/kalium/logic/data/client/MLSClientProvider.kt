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

package com.wire.kalium.logic.data.client

import com.wire.kalium.cryptography.*
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.util.SecurityHelperImpl
import com.wire.kalium.persistence.dbPassphrase.PassphraseStorage
import com.wire.kalium.util.FileUtil
import com.wire.kalium.util.KaliumDispatcher
import com.wire.kalium.util.KaliumDispatcherImpl
import kotlinx.coroutines.withContext

interface MLSClientProvider {
    suspend fun getMLSClient(clientId: ClientId? = null): Either<CoreFailure, MLSClient>
    suspend fun getMLSClient(e2EIClient: E2EIClient, certificateChain: CertificateChain): Either<CoreFailure, MLSClient>
}

class MLSClientProviderImpl(
    private val userId: UserId,
    private val currentClientIdProvider: CurrentClientIdProvider,
    private val coreCryptoCentralProvider: CoreCryptoCentralProvider,
    private val dispatchers: KaliumDispatcher = KaliumDispatcherImpl
) : MLSClientProvider {

    private var mlsClient: MLSClient? = null

    override suspend fun getMLSClient(clientId: ClientId?): Either<CoreFailure, MLSClient> =
        withContext(dispatchers.io) {
            val currentClientId =
                clientId ?: currentClientIdProvider().fold({ return@withContext Either.Left(it) }, { it })
            val cryptoUserId = CryptoUserID(value = userId.value, domain = userId.domain)

            return@withContext mlsClient?.let {
                Either.Right(it)
            } ?: run {
//                //init mls client
//                coreCryptoCentralProvider.getCoreCrypto().flatMap {
//                    val newClient = it.mlsClient(CryptoQualifiedClientId(currentClientId.value, cryptoUserId).toString())
//                    mlsClient = newClient
//                    Either.Right(newClient)
//                }
                Either.Left(CoreFailure.MissingClientRegistration)
            }
        }

    override suspend fun getMLSClient(e2EIClient: E2EIClient,e2eiCertificateChain: CertificateChain): Either<CoreFailure, MLSClient> =
        withContext(dispatchers.io) {
            return@withContext mlsClient?.let {
                Either.Right(it)
            } ?: run {
                coreCryptoCentralProvider.getCoreCrypto().flatMap {
                    val newClient = it.e2eiMlsClient(e2EIClient,e2eiCertificateChain)
                    mlsClient = newClient
                    Either.Right(newClient)
                }
            }
        }
}
