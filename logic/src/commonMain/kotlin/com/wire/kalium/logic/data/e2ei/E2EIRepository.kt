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
package com.wire.kalium.logic.data.e2ei

import com.wire.kalium.cryptography.AcmeChallenge
import com.wire.kalium.cryptography.AcmeDirectory
import com.wire.kalium.cryptography.NewAcmeAuthz
import com.wire.kalium.cryptography.NewAcmeOrder
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.client.E2EClientProvider
import com.wire.kalium.logic.data.client.MLSClientProvider
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.wrapApiRequest
import com.wire.kalium.logic.wrapE2EIRequest
import com.wire.kalium.network.api.base.authenticated.e2ei.AccessTokenResponse
import com.wire.kalium.network.api.base.authenticated.e2ei.E2EIApi
import com.wire.kalium.network.api.base.unbound.acme.ACMEApi
import com.wire.kalium.network.api.base.unbound.acme.ACMEResponse
import com.wire.kalium.network.api.base.unbound.acme.ChallengeResponse
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

interface E2EIRepository {
    suspend fun loadACMEDirectories(): Either<CoreFailure, AcmeDirectory>
    suspend fun getACMENonce(endpoint: String): Either<CoreFailure, String>
    suspend fun createNewAccount(prevNonce: String, createAccountEndpoint: String): Either<CoreFailure, String>
    suspend fun createNewOrder(prevNonce: String, createOrderEndpoint: String): Either<CoreFailure, Triple<NewAcmeOrder, String, String>>
    suspend fun createAuthz(prevNonce: String, authzEndpoint: String): Either<CoreFailure, Triple<NewAcmeAuthz, String, String>>
    suspend fun getWireNonce(): Either<CoreFailure, String>
    suspend fun getWireAccessToken(wireNonce: String): Either<CoreFailure, AccessTokenResponse>
    suspend fun getDPoPToken(wireNonce: String): Either<CoreFailure, String>
    suspend fun validateDPoPChallenge(accessToken: String, prevNonce: String, acmeChallenge: AcmeChallenge)
            : Either<CoreFailure, ChallengeResponse>

    suspend fun validateOIDCChallenge(idToken: String, prevNonce: String, acmeChallenge: AcmeChallenge)
            : Either<CoreFailure, ChallengeResponse>

    suspend fun validateChallenge(challengeResponse: ChallengeResponse): Either<CoreFailure, Unit>
    suspend fun finalize(location: String, prevNonce: String): Either<CoreFailure, Pair<ACMEResponse, String>>
    suspend fun checkOrderRequest(location: String, prevNonce: String): Either<CoreFailure, Pair<ACMEResponse, String>>
    suspend fun certificateRequest(location: String, prevNonce: String): Either<CoreFailure, ACMEResponse>
    suspend fun initMLSClientWithCertificate(certificateChain: String)
}

class E2EIRepositoryImpl(
    private val e2EIApi: E2EIApi,
    private val acmeApi: ACMEApi,
    private val e2EClientProvider: E2EClientProvider,
    private val mlsClientProvider: MLSClientProvider,
    private val currentClientIdProvider: CurrentClientIdProvider
) : E2EIRepository {

    override suspend fun loadACMEDirectories(): Either<CoreFailure, AcmeDirectory> = wrapApiRequest {
        acmeApi.getACMEDirectories()
    }.flatMap { directories ->
        e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
            wrapE2EIRequest {
                e2eiClient.directoryResponse(Json.encodeToString(directories).encodeToByteArray())
            }
        }
    }

    override suspend fun getACMENonce(endpoint: String) = wrapApiRequest {
        acmeApi.getACMENonce(endpoint)
    }

    override suspend fun createNewAccount(prevNonce: String, createAccountEndpoint: String) =
        e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
            val accountRequest = e2eiClient.getNewAccountRequest(prevNonce)
            wrapApiRequest {
                acmeApi.sendACMERequest(createAccountEndpoint, accountRequest)
            }.map { apiResponse ->
                e2eiClient.setAccountResponse(apiResponse.response)
                apiResponse.nonce
            }
        }

    override suspend fun createNewOrder(prevNonce: String, createOrderEndpoint: String) =
        e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
            val orderRequest = e2eiClient.getNewOrderRequest(prevNonce)
            wrapApiRequest {
                acmeApi.sendACMERequest(createOrderEndpoint, orderRequest)
            }.flatMap { apiResponse ->
                val orderRespone = e2eiClient.setOrderResponse(apiResponse.response)
                Either.Right(Triple(orderRespone, apiResponse.nonce, apiResponse.location))
            }
        }

    override suspend fun createAuthz(prevNonce: String, authzEndpoint: String) = e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
        val authzRequest = e2eiClient.getNewAuthzRequest(authzEndpoint, prevNonce)
        wrapApiRequest {
            acmeApi.sendACMERequest(authzEndpoint, authzRequest)
        }.flatMap { apiResponse ->
            val authzResponse = e2eiClient.setAuthzResponse(modifyDpopHtuLink(apiResponse.response))
            Either.Right(Triple(authzResponse, apiResponse.nonce, apiResponse.location))
        }
    }

    //todo: Will remove this after the backend fix it
    private fun modifyDpopHtuLink(apiResponse: ByteArray): ByteArray{
        val apiResponseString = apiResponse.joinToString("") {
            it.toByte().toChar().toString()
        }
        val response = (Json.decodeFromString(apiResponseString) as JsonObject)
        val challenges = response["challenges"] as JsonArray
        val dpopChallenge = challenges[1] as JsonObject

        val modifiedDpopChallenge = dpopChallenge.toMutableMap().apply {
            put("target", JsonPrimitive("https://anta.wire.link/v4/clients/89f1c4056c99edcb/access-token"))
        }
        val modifiedChallenges = challenges.toMutableList().apply {
            removeAt(1)
            add(1, JsonObject(modifiedDpopChallenge))
        }.let { JsonArray(it) }
        val modifiedResp = response.toMutableMap().apply {
            put("challenges", modifiedChallenges)
        }
        return Json.encodeToString(modifiedResp).toByteArray()
    }

    override suspend fun getWireNonce() = currentClientIdProvider().flatMap { clientId ->
        wrapApiRequest {
            e2EIApi.getWireNonce(clientId.value)
        }
    }

    override suspend fun getWireAccessToken(dpopToken: String) = currentClientIdProvider().flatMap { clientId ->
        wrapApiRequest {
            e2EIApi.getAccessToken(clientId.value, dpopToken)
        }
    }

    override suspend fun getDPoPToken(wireNonce: String) = e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
        Either.Right(e2eiClient.createDpopToken(wireNonce))
    }

    override suspend fun validateDPoPChallenge(accessToken: String, prevNonce: String, acmeChallenge: AcmeChallenge) =
        e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
            val challengeRequest = e2eiClient.getNewDpopChallengeRequest(accessToken, prevNonce)
            wrapApiRequest {
                acmeApi.sendChallengeRequest(acmeChallenge.url, challengeRequest)
            }.map { apiResponse ->
                validateChallenge(apiResponse)
                apiResponse
            }
        }

    override suspend fun validateOIDCChallenge(idToken: String, prevNonce: String, acmeChallenge: AcmeChallenge) =
        e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
            val challengeRequest = e2eiClient.getNewOidcChallengeRequest(idToken, prevNonce)
            wrapApiRequest {
                acmeApi.sendChallengeRequest(acmeChallenge.url, challengeRequest)
            }.map { apiResponse ->
                validateChallenge(apiResponse)
                apiResponse
            }
        }

    override suspend fun validateChallenge(challengeResponse: ChallengeResponse) = e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
        e2eiClient.setChallengeResponse(Json.encodeToString(challengeResponse).encodeToByteArray())
        Either.Right(Unit)
    }

    override suspend fun checkOrderRequest(location: String, prevNonce: String) =
        e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
            val checkOrderRequest = e2eiClient.checkOrderRequest(location, prevNonce)
            wrapApiRequest {
                acmeApi.sendACMERequest(location, checkOrderRequest)
            }.map { apiResponse ->
                val finalizeOrderUrl = e2eiClient.checkOrderResponse(apiResponse.response)
                Pair(apiResponse, finalizeOrderUrl)
            }
        }

    override suspend fun finalize(location: String, prevNonce: String) =
        e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
            val finalizeRequest = e2eiClient.finalizeRequest(prevNonce)
            wrapApiRequest {
                acmeApi.sendACMERequest(location, finalizeRequest)
            }.map { apiResponse ->
                val certificateChain = e2eiClient.finalizeResponse(apiResponse.response)
                Pair(apiResponse, certificateChain)
            }
        }

    override suspend fun certificateRequest(location: String, prevNonce: String) =
        e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
            val certificateRequest = e2eiClient.certificateRequest(prevNonce)
            wrapApiRequest {
                acmeApi.sendACMERequest(location, certificateRequest)
            }.map { it }
        }


    override suspend fun initMLSClientWithCertificate(certificateChain: String) {
        e2EClientProvider.getE2EIClient().flatMap { e2eiClient ->
            mlsClientProvider.getMLSClient().map {
                it.initMLSWithE2EI(e2eiClient, certificateChain)
            }
        }
    }
}
