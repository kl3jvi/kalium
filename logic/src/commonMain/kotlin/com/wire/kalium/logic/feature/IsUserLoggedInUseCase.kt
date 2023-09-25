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
package com.wire.kalium.logic.feature

import com.wire.kalium.logic.data.AccessRepository
import com.wire.kalium.logic.functional.fold

interface IsUserLoggedInUseCase {
    suspend operator fun invoke(): Boolean?
}

class IsUserLoggedInUseCaseImpl(
    private val accessRepository: AccessRepository
) : IsUserLoggedInUseCase {
    override suspend fun invoke(): Boolean? {
        accessRepository.loggedInUsers().fold({
            return null
        }, {
            return it?.let { it > 0 }
        })
    }
}

