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

package com.wire.kalium.persistence.config

import com.wire.kalium.persistence.dao.SupportedProtocolEntity
import com.wire.kalium.util.time.Second
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Suppress("TooManyFunctions")
interface UserConfigStorage {

    /**
     * save flag from the user settings to enforce and disable App Lock
     */
    fun persistAppLockStatus(
        isEnforced: Boolean,
        inactivityTimeoutSecs: Second,
        isStatusChanged: Boolean?
    )

    /**
     * get the saved flag to know if App Lock is enforced or not
     */
    fun appLockStatus(): AppLockConfigEntity?

    /**
     * returns a Flow of the saved App Lock status
     */
    fun appLockFlow(): Flow<AppLockConfigEntity?>

    fun setTeamAppLockAsNotified()

    /**
     * Save flag from the file sharing api, and if the status changes
     */
    fun persistFileSharingStatus(status: Boolean, isStatusChanged: Boolean?)

    /**
     * Get the saved flag that been saved to know if the file sharing is enabled or not with the flag
     * to know if there was a status change
     */
    fun isFileSharingEnabled(): IsFileSharingEnabledEntity?

    /**
     * Returns the Flow of file sharing status
     */
    fun isFileSharingEnabledFlow(): Flow<IsFileSharingEnabledEntity?>

    fun setFileSharingAsNotified()

    /**
     * Returns a Flow containing the status and list of classified domains
     */
    fun isClassifiedDomainsEnabledFlow(): Flow<ClassifiedDomainsEntity>

    /**
     *Save the flag and list of trusted domains
     */
    fun persistClassifiedDomainsStatus(status: Boolean, classifiedDomains: List<String>)

    /**
     * Saves the flag that indicates whether a 2FA challenge is
     * required for some operations such as:
     * Login, Create Account, Register Client, etc.
     * @see isSecondFactorPasswordChallengeRequired
     */
    fun persistSecondFactorPasswordChallengeStatus(isRequired: Boolean)

    /**
     * Checks if the 2FA challenge is
     * required for some operations such as:
     * Login, Create Account, Register Client, etc.
     * @see persistSecondFactorPasswordChallengeStatus
     */
    fun isSecondFactorPasswordChallengeRequired(): Boolean

    /**
     * Save default protocol to use
     */
    fun persistDefaultProtocol(protocol: SupportedProtocolEntity)

    /**
     * Gets default protocol to use. Defaults to PROTEUS if not default protocol has been saved.
     */
    fun defaultProtocol(): SupportedProtocolEntity

    /**
     * Save flag from the user settings to enable and disable MLS
     */
    fun enableMLS(enabled: Boolean)

    /**
     * Get the saved flag to know if MLS enabled or not
     */
    fun isMLSEnabled(): Boolean

    /**
     * Save MLSE2EISetting
     */
    fun setE2EISettings(settingEntity: E2EISettingsEntity?)

    /**
     * Get MLSE2EISetting
     */
    fun getE2EISettings(): E2EISettingsEntity?

    /**
     * Get Flow of the saved MLSE2EISetting
     */
    fun e2EISettingsFlow(): Flow<E2EISettingsEntity?>

    /**
     * Save flag from user settings to enable or disable Conference Calling
     */
    fun persistConferenceCalling(enabled: Boolean)

    /**
     * Get the saved flag to know if Conference Calling is enabled or not
     */
    fun isConferenceCallingEnabled(): Boolean

    /**
     * Get the saved flag to know whether user's Read Receipts are currently enabled or not
     */
    fun areReadReceiptsEnabled(): Flow<Boolean>

    /**
     * Persist the flag to indicate if user's Read Receipts are enabled or not.
     */
    fun persistReadReceipts(enabled: Boolean)

    /**
     * Get the saved global flag to know whether user's typing indicator is currently enabled or not.
     */
    fun isTypingIndicatorEnabled(): Flow<Boolean>

    /**
     * Persist the flag to indicate whether user's typing indicator global flag is enabled or not.
     */
    fun persistTypingIndicator(enabled: Boolean)

    fun persistGuestRoomLinkFeatureFlag(status: Boolean, isStatusChanged: Boolean?)
    fun isGuestRoomLinkEnabled(): IsGuestRoomLinkEnabledEntity?
    fun isGuestRoomLinkEnabledFlow(): Flow<IsGuestRoomLinkEnabledEntity?>
    fun isScreenshotCensoringEnabledFlow(): Flow<Boolean>
    fun persistScreenshotCensoring(enabled: Boolean)
    fun setIfAbsentE2EINotificationTime(timeStamp: Long)
    fun getE2EINotificationTime(): Long?
    fun e2EINotificationTimeFlow(): Flow<Long?>
    fun updateE2EINotificationTime(timeStamp: Long)
    fun setShouldFetchE2EITrustAnchors(shouldFetch: Boolean)
    fun getShouldFetchE2EITrustAnchorHasRun(): Boolean

    companion object {
        const val FILE_SHARING = "file_sharing"
        const val GUEST_ROOM_LINK = "guest_room_link"
        const val ENABLE_CLASSIFIED_DOMAINS = "enable_classified_domains"
        const val ENABLE_MLS = "enable_mls"
        const val E2EI_SETTINGS = "end_to_end_identity_settings"
        const val E2EI_NOTIFICATION_TIME = "end_to_end_identity_notification_time"
        const val ENABLE_CONFERENCE_CALLING = "enable_conference_calling"
        const val ENABLE_READ_RECEIPTS = "enable_read_receipts"
        const val DEFAULT_CONFERENCE_CALLING_ENABLED_VALUE = false
        const val REQUIRE_SECOND_FACTOR_PASSWORD_CHALLENGE =
            "require_second_factor_password_challenge"
        const val ENABLE_SCREENSHOT_CENSORING = "enable_screenshot_censoring"
        const val ENABLE_TYPING_INDICATOR = "enable_typing_indicator"
        const val APP_LOCK = "app_lock"
        const val DEFAULT_PROTOCOL = "default_protocol"
        const val SHOULD_FETCH_E2EI_GET_TRUST_ANCHORS = "should_fetch_e2ei_trust_anchors"
    }
}

@Serializable
data class IsFileSharingEnabledEntity(
    @SerialName("status") val status: Boolean,
    @SerialName("isStatusChanged") val isStatusChanged: Boolean?
)

@Serializable
data class ClassifiedDomainsEntity(
    @SerialName("status") val status: Boolean,
    @SerialName("trustedDomains") val trustedDomains: List<String>,
)

@Serializable
data class IsGuestRoomLinkEnabledEntity(
    @SerialName("status") val status: Boolean,
    @SerialName("isStatusChanged") val isStatusChanged: Boolean?
)

@Serializable
data class TeamSettingsSelfDeletionStatusEntity(
    @SerialName("selfDeletionTimer") val selfDeletionTimerEntity: SelfDeletionTimerEntity,
    @SerialName("isStatusChanged") val isStatusChanged: Boolean?
)

@Serializable
data class E2EISettingsEntity(
    @SerialName("status") val status: Boolean,
    @SerialName("discoverUrl") val discoverUrl: String?,
    @SerialName("gracePeriodEndMs") val gracePeriodEndMs: Long?,
)

@Serializable
data class AppLockConfigEntity(
    @SerialName("inactivityTimeoutSecs") val inactivityTimeoutSecs: Second,
    @SerialName("enforceAppLock") val enforceAppLock: Boolean,
    @SerialName("isStatusChanged") val isStatusChanged: Boolean?
)

@Serializable
data class LegalHoldRequestEntity(
    @SerialName("clientId") val clientId: String,
    @SerialName("lastPrekey") val lastPreKey: LastPreKey,
)

@Serializable
data class LastPreKey(
    @SerialName("id") val id: Int,
    @SerialName("key") val key: String,
)

@Serializable
data class CRLUrlExpirationList(
    @SerialName("crl_with_expiration_list") val cRLWithExpirationList: List<CRLWithExpiration>
)

@Serializable
data class CRLWithExpiration(
    @SerialName("url") val url: String,
    @SerialName("expiration") val expiration: ULong
)

@Serializable
sealed class SelfDeletionTimerEntity {

    @Serializable
    @SerialName("disabled")
    data object Disabled : SelfDeletionTimerEntity()

    @Serializable
    @SerialName("enabled")
    data object Enabled : SelfDeletionTimerEntity()

    @Serializable
    @SerialName("enforced")
    data class Enforced(val enforcedDuration: Duration) : SelfDeletionTimerEntity()
}

@Serializable
data class MLSMigrationEntity(
    @Serializable val status: Boolean,
    @Serializable val startTime: Instant?,
    @Serializable val endTime: Instant?,
)
