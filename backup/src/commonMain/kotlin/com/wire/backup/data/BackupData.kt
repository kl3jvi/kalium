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
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val metadata: BackupMetadata,
    val users: List<User>,
    val conversations: List<Conversation>,
    val messages: List<Message>
)

@Serializable
data class QualifiedID(
    val domain: String,
    val value: String,
)

@Serializable
data class User(
    val id: QualifiedID,
    val name: String,
    val handle: String,
)

@Serializable
data class Conversation(val conversationId: ConversationId, val name: String)

@Serializable
sealed interface Message {
    val messageId: String
    val senderUserId: UserId
    val conversationId: ConversationId
    val time: Instant
    val senderClientId: String

    @Serializable
    data class Text(
        override val messageId: String,
        override val conversationId: ConversationId,
        override val senderUserId: UserId,
        override val time: Instant,
        override val senderClientId: String,
        val textValue: String,
    ) : Message
}
