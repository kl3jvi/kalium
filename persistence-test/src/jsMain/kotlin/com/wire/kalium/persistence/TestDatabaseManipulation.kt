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

package com.wire.kalium.persistence

import com.wire.kalium.persistence.dao.UserIDEntity
import com.wire.kalium.persistence.db.GlobalDatabaseProvider
import com.wire.kalium.persistence.db.UserDatabaseBuilder
import kotlinx.coroutines.test.TestDispatcher

internal actual fun createTestDatabase(
    userId: UserIDEntity,
    dispatcher: TestDispatcher
): UserDatabaseBuilder = TODO("JavaScript Database not yet supported")

internal actual fun deleteTestDatabase(userId: UserIDEntity) { TODO("JavaScript Database not yet supported") }

internal actual fun createTestGlobalDatabase(): GlobalDatabaseProvider = TODO("JavaScript Database not yet supported")

internal actual fun deleteTestGlobalDatabase() { TODO("JavaScript Database not yet supported") }
