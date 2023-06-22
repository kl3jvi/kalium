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

import com.wire.kalium.cryptography.*
import com.wire.kalium.cryptography.utils.generateRandomAES256Key
import com.wire.kalium.logic.data.client.E2EClientProvider
import com.wire.kalium.logic.data.client.MLSClientProvider
import com.wire.kalium.logic.data.e2ei.E2EIRepositoryTest.Arrangement.Companion.ACME_CHALLENGE
import com.wire.kalium.logic.data.e2ei.E2EIRepositoryTest.Arrangement.Companion.ACME_DIRECTORIES
import com.wire.kalium.logic.data.e2ei.E2EIRepositoryTest.Arrangement.Companion.ACME_DIRECTORIES_RESPONSE
import com.wire.kalium.logic.data.e2ei.E2EIRepositoryTest.Arrangement.Companion.RANDOM_ACCESS_TOKEN
import com.wire.kalium.logic.data.e2ei.E2EIRepositoryTest.Arrangement.Companion.RANDOM_ID_TOKEN
import com.wire.kalium.logic.data.e2ei.E2EIRepositoryTest.Arrangement.Companion.RANDOM_NONCE
import com.wire.kalium.logic.data.e2ei.E2EIRepositoryTest.Arrangement.Companion.RANDOM_URL
import com.wire.kalium.logic.data.featureConfig.*
import com.wire.kalium.logic.feature.CurrentClientIdProvider
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.util.shouldFail
import com.wire.kalium.logic.util.shouldSucceed
import com.wire.kalium.network.api.base.authenticated.e2ei.E2EIApi
import com.wire.kalium.network.api.base.authenticated.featureConfigs.*
import com.wire.kalium.network.api.base.model.ErrorResponse
import com.wire.kalium.network.api.base.unbound.acme.ACMEApi
import com.wire.kalium.network.api.base.unbound.acme.ACMEResponse
import com.wire.kalium.network.api.base.unbound.acme.AcmeDirectoriesResponse
import com.wire.kalium.network.api.base.unbound.acme.ChallengeResponse
import com.wire.kalium.network.exceptions.KaliumException
import com.wire.kalium.network.utils.NetworkResponse
import com.wire.kalium.util.serialization.toJsonElement
import io.ktor.util.*
import io.mockative.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class E2EIRepositoryTest {
    @Test
    fun givenACMEDirectoriesApiSucceed_whenLoadAcmeDirectories_thenItSucceed() = runTest {
        // Given

        val (arrangement, e2eiRepository) = Arrangement()
            .withAcmeDirectoriesApiSucceed()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withE2EIClientLoadDirectoriesSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.loadACMEDirectories()

        // Then
        result.shouldSucceed()

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::getACMEDirectories)
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::directoryResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasInvoked(once)
    }

    @Test
    fun givenACMEDirectoriesApiFails_whenLoadAcmeDirectories_thenItFail() = runTest {
        // Given

        val (arrangement, e2eiRepository) = Arrangement()
            .withAcmeDirectoriesApiFails()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withE2EIClientLoadDirectoriesSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.loadACMEDirectories()

        // Then
        result.shouldFail()

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::getACMEDirectories)
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::directoryResponse)
            .with(any())
            .wasNotInvoked()
    }

    @Test
    fun givenSendAcmeRequestSucceed_whenCallingCreateNewAccount_thenItSucceed() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiSucceed()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewAccountSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.createNewAccount(RANDOM_NONCE, RANDOM_URL)

        // Then
        result.shouldSucceed()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewAccountRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setAccountResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasInvoked(once)
    }

    @Test
    fun givenSendAcmeRequestFails_whenCallingCreateNewAccount_thenItFail() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiFails()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewAccountSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.createNewAccount(RANDOM_NONCE, RANDOM_URL)

        // Then
        result.shouldFail()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewAccountRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setAccountResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasNotInvoked()
    }

    @Test
    fun givenSendAcmeRequestSucceed_whenCallingCreateNewOrder_thenItSucceed() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiSucceed()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewOrderSuccessful()
            .withSetOrderResponseSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.createNewOrder(RANDOM_NONCE, RANDOM_URL)

        // Then
        result.shouldSucceed()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewOrderRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setOrderResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasInvoked(once)
    }

    @Test
    fun givenSendAcmeRequestFails_whenCallingCreateNewOrder_thenItFail() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiFails()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewOrderSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.createNewOrder(RANDOM_NONCE, RANDOM_URL)

        // Then
        result.shouldFail()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewOrderRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setOrderResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasNotInvoked()
    }

    @Test
    fun givenSendAcmeRequestSucceed_whenCallingCreateAuthz_thenItSucceed() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiSucceed()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewAuthzRequestSuccessful()
            .withSetAuthzResponseSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.createAuthz(RANDOM_NONCE, RANDOM_URL)

        // Then
        result.shouldSucceed()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewAuthzRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setAuthzResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasInvoked(once)
    }

    @Test
    fun givenSendAcmeRequestFails_whenCallingCreateAuthz_thenItFail() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiFails()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewAuthzRequestSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.createAuthz(RANDOM_NONCE, RANDOM_URL)

        // Then
        result.shouldFail()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewAuthzRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setAuthzResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasNotInvoked()
    }

    @Test
    fun givenDpopChallengeRequestSucceed_whenCallingValidateDPoPChallenge_thenItSucceed() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendChallengeRequestApiSucceed()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewDpopChallengeRequest()
            .arrange()

        // When
        val result = e2eiRepository.validateDPoPChallenge(RANDOM_ACCESS_TOKEN, RANDOM_NONCE, ACME_CHALLENGE)

        // Then
        result.shouldSucceed()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewDpopChallengeRequest)
            .with(anyInstanceOf(String::class), anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendChallengeRequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setChallengeResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasInvoked(once)
    }

    @Test
    fun givenDpopChallengeRequestFails_whenCallingValidateDPoPChallenge_thenItFail() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendChallengeRequestApiFails()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewDpopChallengeRequest()
            .arrange()

        // When
        val result = e2eiRepository.validateDPoPChallenge(RANDOM_ACCESS_TOKEN, RANDOM_NONCE, ACME_CHALLENGE)

        // Then
        result.shouldFail()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewDpopChallengeRequest)
            .with(anyInstanceOf(String::class), anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendChallengeRequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setChallengeResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasNotInvoked()
    }

    @Test
    fun givenOIDCChallengeRequestSucceed_whenCallingValidateDPoPChallenge_thenItSucceed() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendChallengeRequestApiSucceed()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewOidcChallengeRequest()
            .arrange()

        // When
        val result = e2eiRepository.validateOIDCChallenge(RANDOM_ID_TOKEN, RANDOM_NONCE, ACME_CHALLENGE)

        // Then
        result.shouldSucceed()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewOidcChallengeRequest)
            .with(anyInstanceOf(String::class), anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendChallengeRequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setChallengeResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasInvoked(once)
    }

    @Test
    fun givenOIDCChallengeRequestFails_whenCallingValidateDPoPChallenge_thenItFail() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendChallengeRequestApiFails()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withGetNewOidcChallengeRequest()
            .arrange()

        // When
        val result = e2eiRepository.validateOIDCChallenge(RANDOM_ID_TOKEN, RANDOM_NONCE, ACME_CHALLENGE)

        // Then
        result.shouldFail()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::getNewOidcChallengeRequest)
            .with(anyInstanceOf(String::class), anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendChallengeRequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::setChallengeResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasNotInvoked()
    }

    @Test
    fun givenSendAcmeRequestSucceed_whenCallingCheckOrderRequest_thenItSucceed() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiSucceed()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withCheckOrderRequestSuccessful()
            .withCheckOrderResponseSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.checkOrderRequest(RANDOM_URL, RANDOM_NONCE)

        // Then
        result.shouldSucceed()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::checkOrderRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::checkOrderResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasInvoked(once)
    }

    @Test
    fun givenSendAcmeRequestFails_whenCallingCheckOrderRequest_thenItFail() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiFails()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withCheckOrderRequestSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.checkOrderRequest(RANDOM_URL, RANDOM_NONCE)

        // Then
        result.shouldFail()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::checkOrderRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::checkOrderResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasNotInvoked()
    }

    @Test
    fun givenSendAcmeRequestSucceed_whenCallingFinalize_thenItSucceed() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiSucceed()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withFinalizeRequestSuccessful()
            .withFinalizeResponseSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.finalize(RANDOM_URL, RANDOM_NONCE)

        // Then
        result.shouldSucceed()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::finalizeRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::finalizeResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasInvoked(once)
    }

    @Test
    fun givenSendAcmeRequestFails_whenCallingFinalize_thenItFail() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiFails()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withFinalizeRequestSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.finalize(RANDOM_URL, RANDOM_NONCE)

        // Then
        result.shouldFail()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::finalizeRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::finalizeResponse)
            .with(anyInstanceOf(ByteArray::class))
            .wasNotInvoked()
    }

    @Test
    fun givenSendAcmeRequestSucceed_whenCallingCertificateRequest_thenItSucceed() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiSucceed()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withCertificateRequestSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.certificateRequest(RANDOM_URL, RANDOM_NONCE)

        // Then
        result.shouldSucceed()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::certificateRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)
    }

    @Test
    fun givenSendAcmeRequestFails_whenCallingCertificateRequest_thenItFail() = runTest {
        // Given
        val (arrangement, e2eiRepository) = Arrangement()
            .withSendAcmeRequestApiFails()
            .withGetE2EIClientSuccessful()
            .withGetMLSClientSuccessful()
            .withCertificateRequestSuccessful()
            .arrange()

        // When
        val result = e2eiRepository.certificateRequest(RANDOM_URL, RANDOM_NONCE)

        // Then
        result.shouldFail()

        verify(arrangement.e2eiClient)
            .function(arrangement.e2eiClient::certificateRequest)
            .with(anyInstanceOf(String::class))
            .wasInvoked(once)

        verify(arrangement.acmeApi)
            .suspendFunction(arrangement.acmeApi::sendACMERequest)
            .with(anyInstanceOf(String::class), any())
            .wasInvoked(once)
    }

    private class Arrangement {

        fun withGetE2EIClientSuccessful() = apply {
            given(e2eiClientProvider)
                .suspendFunction(e2eiClientProvider::getE2EIClient)
                .whenInvokedWith(anything())
                .thenReturn(Either.Right(e2eiClient))
        }

        fun withE2EIClientLoadDirectoriesSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::directoryResponse)
                .whenInvokedWith(anything())
                .thenReturn(ACME_DIRECTORIES)
        }

        fun withGetNewAccountSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::getNewAccountRequest)
                .whenInvokedWith(anything())
                .thenReturn(RANDOM_BYTE_ARRAY)
        }

        fun withGetNewOrderSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::getNewOrderRequest)
                .whenInvokedWith(anything())
                .thenReturn(RANDOM_BYTE_ARRAY)
        }

        fun withGetNewAuthzRequestSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::getNewAuthzRequest)
                .whenInvokedWith(anything(), anything())
                .thenReturn(RANDOM_BYTE_ARRAY)
        }

        fun withCheckOrderRequestSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::checkOrderRequest)
                .whenInvokedWith(anything(), anything())
                .thenReturn(RANDOM_BYTE_ARRAY)
        }

        fun withFinalizeRequestSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::finalizeRequest)
                .whenInvokedWith(anything())
                .thenReturn(RANDOM_BYTE_ARRAY)
        }
        fun withCertificateRequestSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::certificateRequest)
                .whenInvokedWith(anything())
                .thenReturn(RANDOM_BYTE_ARRAY)
        }

        fun withFinalizeResponseSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::finalizeResponse)
                .whenInvokedWith(anything())
                .thenReturn("")
        }

        fun withCheckOrderResponseSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::checkOrderResponse)
                .whenInvokedWith(anything())
                .thenReturn("")
        }

        fun withGetNewDpopChallengeRequest() = apply {
            given(e2eiClient)
                .function(e2eiClient::getNewDpopChallengeRequest)
                .whenInvokedWith(anything(), anything())
                .thenReturn(RANDOM_BYTE_ARRAY)
        }

        fun withGetNewOidcChallengeRequest() = apply {
            given(e2eiClient)
                .function(e2eiClient::getNewOidcChallengeRequest)
                .whenInvokedWith(anything(), anything())
                .thenReturn(RANDOM_BYTE_ARRAY)
        }

        fun withSetOrderResponseSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::setOrderResponse)
                .whenInvokedWith(anything())
                .thenReturn(ACME_ORDER)
        }

        fun withSetAuthzResponseSuccessful() = apply {
            given(e2eiClient)
                .function(e2eiClient::setAuthzResponse)
                .whenInvokedWith(anything())
                .thenReturn(ACME_AUTHZ)
        }

        fun withGetMLSClientSuccessful() = apply {
            given(mlsClientProvider)
                .suspendFunction(mlsClientProvider::getMLSClient)
                .whenInvokedWith(anything())
                .thenReturn(Either.Right(mlsClient))
        }

        fun withAcmeDirectoriesApiSucceed(): Arrangement {
            given(acmeApi)
                .suspendFunction(acmeApi::getACMEDirectories)
                .whenInvoked()
                .thenReturn(NetworkResponse.Success(ACME_DIRECTORIES_RESPONSE, mapOf(), 200))
            return this
        }

        fun withAcmeDirectoriesApiFails(): Arrangement {
            given(acmeApi)
                .suspendFunction(acmeApi::getACMEDirectories).whenInvoked()
                .thenReturn(NetworkResponse.Error(INVALID_REQUEST_ERROR))
            return this
        }


        fun withSendAcmeRequestApiSucceed(): Arrangement {
            given(acmeApi)
                .suspendFunction(acmeApi::sendACMERequest)
                .whenInvokedWith(any(), any())
                .thenReturn(NetworkResponse.Success(ACME_REQUEST_RESPONSE, mapOf(), 200))
            return this
        }

        fun withSendAcmeRequestApiFails(): Arrangement {
            given(acmeApi)
                .suspendFunction(acmeApi::sendACMERequest)
                .whenInvokedWith(any(), any())
                .thenReturn(NetworkResponse.Error(INVALID_REQUEST_ERROR))
            return this
        }

        fun withSendChallengeRequestApiSucceed(): Arrangement {
            given(acmeApi)
                .suspendFunction(acmeApi::sendChallengeRequest)
                .whenInvokedWith(any(), any())
                .thenReturn(NetworkResponse.Success(ACME_CHALLENGE_RESPONSE, mapOf(), 200))
            return this
        }

        fun withSendChallengeRequestApiFails(): Arrangement {
            given(acmeApi)
                .suspendFunction(acmeApi::sendChallengeRequest)
                .whenInvokedWith(any(), any())
                .thenReturn(NetworkResponse.Error(INVALID_REQUEST_ERROR))
            return this
        }

        //    suspend fun getACMENonce(endpoint: String): Either<CoreFailure, String>
        //    suspend fun createNewAccount(prevNonce: String, createAccountEndpoint: String): Either<CoreFailure, String>
        //    suspend fun createNewOrder(prevNonce: String, createOrderEndpoint: String): Either<CoreFailure, Triple<NewAcmeOrder, String, String>>
        //    suspend fun createAuthz(prevNonce: String, authzEndpoint: String): Either<CoreFailure, Triple<NewAcmeAuthz, String, String>>
        //    suspend fun getWireNonce(): Either<CoreFailure, String>
        //    suspend fun getWireAccessToken(wireNonce: String): Either<CoreFailure, AccessTokenResponse>
        //    suspend fun getDPoPToken(wireNonce: String): Either<CoreFailure, String>
        //    suspend fun validateDPoPChallenge(accessToken: String, prevNonce: String, acmeChallenge: AcmeChallenge)
        //            : Either<CoreFailure, ChallengeResponse>
        //
        //    suspend fun validateOIDCChallenge(idToken: String, prevNonce: String, acmeChallenge: AcmeChallenge)
        //            : Either<CoreFailure, ChallengeResponse>
        //
        //    suspend fun validateChallenge(challengeResponse: ChallengeResponse): Either<CoreFailure, Unit>
        //    suspend fun finalize(location: String, prevNonce: String): Either<CoreFailure, Pair<ACMEResponse, String>>
        //    suspend fun checkOrderRequest(location: String, prevNonce: String): Either<CoreFailure, Pair<ACMEResponse, String>>
        //    suspend fun certificateRequest(location: String, prevNonce: String): Either<CoreFailure, ACMEResponse>
        //    suspend fun initMLSClientWithCertificate(certificateChain: String)

        @Mock
        val e2eiApi: E2EIApi = mock(classOf<E2EIApi>())

        @Mock
        val acmeApi: ACMEApi = mock(classOf<ACMEApi>())

        @Mock
        val e2eiClientProvider: E2EClientProvider = mock(classOf<E2EClientProvider>())

        @Mock
        val e2eiClient = mock(classOf<E2EIClient>())

        @Mock
        val mlsClientProvider: MLSClientProvider = mock(classOf<MLSClientProvider>())

        @Mock
        val mlsClient = mock(classOf<MLSClient>())

        @Mock
        val currentClientIdProvider: CurrentClientIdProvider = mock(classOf<CurrentClientIdProvider>())

        fun arrange() =
            this to E2EIRepositoryImpl(e2eiApi, acmeApi, e2eiClientProvider, mlsClientProvider, currentClientIdProvider)

        companion object {
            val INVALID_REQUEST_ERROR = KaliumException.InvalidRequestError(ErrorResponse(405, "", ""))
            val RANDOM_BYTE_ARRAY = "random-value".encodeToByteArray()
            val RANDOM_NONCE = "xxxxx"
            val RANDOM_ACCESS_TOKEN = "xxxxx"
            val RANDOM_ID_TOKEN = "xxxxx"
            val RANDOM_URL = "https://random.rn"

            val ACME_BASE_URL = "https://balderdash.hogwash.work:9000"


            val ACME_DIRECTORIES_RESPONSE = AcmeDirectoriesResponse(
                newNonce = "$ACME_BASE_URL/acme/wire/new-nonce",
                newAccount = "$ACME_BASE_URL/acme/wire/new-account",
                newOrder = "$ACME_BASE_URL/acme/wire/new-order",
                revokeCert = "$ACME_BASE_URL/acme/wire/revoke-cert",
                keyChange = "$ACME_BASE_URL/acme/wire/key-change"
            )

            val ACME_DIRECTORIES = AcmeDirectory(
                newNonce = "$ACME_BASE_URL/acme/wire/new-nonce",
                newAccount = "$ACME_BASE_URL/acme/wire/new-account",
                newOrder = "$ACME_BASE_URL/acme/wire/new-order"
            )

            val ACME_REQUEST_RESPONSE = ACMEResponse(
                nonce = RANDOM_NONCE,
                location = RANDOM_URL,
                response = RANDOM_BYTE_ARRAY
            )

            val ACME_ORDER = NewAcmeOrder(
                delegate = RANDOM_BYTE_ARRAY,
                authorizations = emptyList()
            )

            val ACME_CHALLENGE = AcmeChallenge(
                delegate = RANDOM_BYTE_ARRAY,
                url = RANDOM_URL
            )

            val ACME_AUTHZ = NewAcmeAuthz(
                identifier = "identifier",
                wireOidcChallenge = ACME_CHALLENGE,
                wireDpopChallenge = ACME_CHALLENGE
            )

            val ACME_CHALLENGE_RESPONSE = ChallengeResponse(
                type = "type",
                url = "url",
                status = "status",
                token = "token",
                nonce = "nonce"
            )

        }
    }
}
