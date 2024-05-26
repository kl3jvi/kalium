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

package com.wire.kalium.persistence.dao

interface PrekeyDAO {
    suspend fun updateMostRecentPreKeyId(newKeyId: Int)
    suspend fun forceInsertMostRecentPreKeyId(newKeyId: Int)
    suspend fun mostRecentPreKeyId(): Int?

    companion object {
        /**
         * Key used to store the most recent prekey ID in the metadata table.
         * In order to not be confused with "last prekey", which is the "last resort" permanent prekey
         * used when all prekeys are consumed, the variable was renamed to "most recent prekey".
         */
        const val MOST_RECENT_PREKEY_ID = "otr_last_pre_key_id"
    }
}
