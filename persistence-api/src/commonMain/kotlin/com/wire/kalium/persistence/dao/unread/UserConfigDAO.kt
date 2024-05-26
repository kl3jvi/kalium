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

@file:Suppress("TooManyFunctions")

package com.wire.kalium.persistence.dao.unread

import com.wire.kalium.persistence.config.LegalHoldRequestEntity
import com.wire.kalium.persistence.config.MLSMigrationEntity
import com.wire.kalium.persistence.config.TeamSettingsSelfDeletionStatusEntity
import com.wire.kalium.persistence.dao.SupportedProtocolEntity
import kotlinx.coroutines.flow.Flow

interface UserConfigDAO {

    suspend fun getTeamSettingsSelfDeletionStatus(): TeamSettingsSelfDeletionStatusEntity?
    suspend fun setTeamSettingsSelfDeletionStatus(
        teamSettingsSelfDeletionStatusEntity: TeamSettingsSelfDeletionStatusEntity
    )

    suspend fun markTeamSettingsSelfDeletingMessagesStatusAsNotified()
    suspend fun observeTeamSettingsSelfDeletingStatus(): Flow<TeamSettingsSelfDeletionStatusEntity?>

    suspend fun getMigrationConfiguration(): MLSMigrationEntity?
    suspend fun setMigrationConfiguration(configuration: MLSMigrationEntity)

    suspend fun getSupportedProtocols(): Set<SupportedProtocolEntity>?
    suspend fun setSupportedProtocols(protocols: Set<SupportedProtocolEntity>)
    suspend fun persistLegalHoldRequest(clientId: String, lastPreKeyId: Int, lastPreKey: String)
    suspend fun clearLegalHoldRequest()
    fun observeLegalHoldRequest(): Flow<LegalHoldRequestEntity?>
    suspend fun setLegalHoldChangeNotified(isNotified: Boolean)
    suspend fun observeLegalHoldChangeNotified(): Flow<Boolean?>
    suspend fun setShouldUpdateClientLegalHoldCapability(shouldUpdate: Boolean)
    suspend fun shouldUpdateClientLegalHoldCapability(): Boolean
    suspend fun setCRLExpirationTime(url: String, timestamp: ULong)
    suspend fun getCRLsPerDomain(url: String): ULong?
    suspend fun observeCertificateExpirationTime(url: String): Flow<ULong?>
    suspend fun setShouldNotifyForRevokedCertificate(shouldNotify: Boolean)
    suspend fun observeShouldNotifyForRevokedCertificate(): Flow<Boolean?>

    companion object {
        private const val SELF_DELETING_MESSAGES_KEY = "SELF_DELETING_MESSAGES"
        private const val SHOULD_NOTIFY_FOR_REVOKED_CERTIFICATE = "should_notify_for_revoked_certificate"
        private const val MLS_MIGRATION_KEY = "MLS_MIGRATION"
        private const val SUPPORTED_PROTOCOLS_KEY = "SUPPORTED_PROTOCOLS"
        const val LEGAL_HOLD_REQUEST = "legal_hold_request"
        const val LEGAL_HOLD_CHANGE_NOTIFIED = "legal_hold_change_notified"
        const val SHOULD_UPDATE_CLIENT_LEGAL_HOLD_CAPABILITY =
            "should_update_client_legal_hold_capability"
    }
}
