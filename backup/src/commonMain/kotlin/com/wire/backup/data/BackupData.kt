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
package com.wire.backup.data

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.datetime.Instant

sealed interface BackupData {
//
//     data class User(
//         val id: QualifiedID,
//         val name: String,
//         val handle: String,
//     ) : BackupData
//
//     data class Conversation(val conversationId: ConversationId, val name: String) : BackupData

    sealed interface Message : BackupData {
        val messageId: String
        val senderUserId: UserId
        val conversationId: ConversationId
        val time: Instant
        val senderClientId: String

        data class Text(
            override val messageId: String,
            override val conversationId: ConversationId,
            override val senderUserId: UserId,
            override val time: Instant,
            override val senderClientId: String,
            val textValue: String,
        ) : Message
    }
}