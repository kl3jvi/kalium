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

package com.wire.kalium.logic.featureFlags

import com.wire.kalium.logic.feature.applock.AppLockTeamFeatureConfigObserverImpl.Companion.DEFAULT_TIMEOUT
import com.wire.kalium.logic.util.KaliumMockEngine
import com.wire.kalium.network.NetworkStateObserver
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

data class KaliumConfigs(
    val forceConstantBitrateCalls: Boolean = false,
    val fileRestrictionState: BuildFileRestrictionState = BuildFileRestrictionState.NoRestriction,
    var isMLSSupportEnabled: Boolean = true,
    // Disabling db-encryption will crash on android-api level below 30
    val shouldEncryptData: Boolean = true,
    val encryptProteusStorage: Boolean = false,
    val lowerKeyPackageLimits: Boolean = false,
    val lowerKeyingMaterialsUpdateThreshold: Boolean = false,
    val developmentApiEnabled: Boolean = false,
    val ignoreSSLCertificatesForUnboundCalls: Boolean = true,
    val guestRoomLink: Boolean = true,
    val selfDeletingMessages: Boolean = true,
    val wipeOnCookieInvalid: Boolean = false,
    val wipeOnDeviceRemoval: Boolean = false,
    val wipeOnRootedDevice: Boolean = false,
    val isWebSocketEnabledByDefault: Boolean = false,
    val certPinningConfig: Map<String, List<String>> = emptyMap(),
    val kaliumMockEngine: KaliumMockEngine? = null,
    val mockNetworkStateObserver: NetworkStateObserver? = null,
    // Interval between attempts to advance the proteus to MLS migration
    val mlsMigrationInterval: Duration = 24.hours,
    val teamAppLock: Boolean = false,
    val teamAppLockTimeout: Int = DEFAULT_TIMEOUT.toInt(DurationUnit.SECONDS),
)

sealed interface BuildFileRestrictionState {
    data object NoRestriction : BuildFileRestrictionState
    data class AllowSome(val allowedType: List<String>) : BuildFileRestrictionState
}
