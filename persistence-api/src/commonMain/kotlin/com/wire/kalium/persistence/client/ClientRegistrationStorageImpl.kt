/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.kalium.persistence.client

import kotlinx.coroutines.flow.Flow

@Suppress("LongParameterList", "TooManyFunctions")
interface ClientRegistrationStorage {
    suspend fun getRegisteredClientId(): String?
    suspend fun setRegisteredClientId(registeredClientId: String)
    suspend fun observeRegisteredClientId(): Flow<String?>
    suspend fun setRetainedClientId(retainedClientId: String)
    suspend fun getRetainedClientId(): String?
    suspend fun clearRegisteredClientId()
    suspend fun clearRetainedClientId()
    suspend fun hasRegisteredMLSClient(): Boolean
    suspend fun setHasRegisteredMLSClient()
    suspend fun observeIsClientRegistrationBlockedByE2EI(): Flow<Boolean>
    suspend fun setClientRegistrationBlockedByE2EI()
    suspend fun clearClientRegistrationBlockedByE2EI()
    suspend fun clearHasRegisteredMLSClient()
    suspend fun isBlockedByE2EI(): Boolean

    companion object {
        private const val REGISTERED_CLIENT_ID_KEY = "registered_client_id"
        const val RETAINED_CLIENT_ID_KEY = "retained_client_id"
        private const val HAS_REGISTERED_MLS_CLIENT_KEY = "has_registered_mls_client"
        private const val CLIENT_REGISTRATION_BLOCKED_BY_E2EI = "client_registration_blocked_by_e2ei"
    }
}

