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
package com.wire.kalium.logic.feature.featureConfig

import com.wire.kalium.logic.data.session.SessionRepository
import com.wire.kalium.logic.feature.UserSessionScopeProvider
import com.wire.kalium.logic.functional.fold

/**
 * Checks if the app lock is editable.
 * The app lock is editable if there is no enforced app lock on any of the user's accounts.
 * If there is an enforced app lock on any of the user's accounts, the app lock is not editable.
 */
class IsAppLockEditableUseCase internal constructor(
    private val userSessionScopeProvider: UserSessionScopeProvider,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): Boolean =
            sessionRepository.allValidSessions().fold({
                true
            }) { accounts ->
                accounts.map { session ->
                    userSessionScopeProvider.getOrCreate(session.userId).let { userSessionScope ->
                        userSessionScope.userConfigRepository.isTeamAppLockEnabled().fold({
                            true
                        }, { appLockConfig ->
                            appLockConfig.isEnforced
                        })
                    }
                }.contains(true).not()
            }
}