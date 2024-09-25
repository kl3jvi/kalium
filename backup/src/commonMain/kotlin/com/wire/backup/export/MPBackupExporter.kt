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
package com.wire.backup.export

import com.wire.backup.data.BackupData
import com.wire.backup.data.BackupMetadata
import com.wire.backup.data.Conversation
import com.wire.backup.data.Message
import com.wire.backup.data.User
import com.wire.kalium.logic.data.user.UserId
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class MPBackupExporter(val userId: UserId) {
    private val allUsers = mutableListOf<User>()
    private val allConversations = mutableListOf<Conversation>()
    private val allMessages = mutableListOf<Message>()

    fun add(user: User) {
        allUsers.add(user)
    }

    fun add(conversation: Conversation) {
        allConversations.add(conversation)
    }

    fun add(message: Message) {
        allMessages.add(message)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun serialize(): ByteArray {
        val backupData = BackupData(
            BackupMetadata(
                platform = "Common",
                version = "1.0",
                userId = "${userId.value}@${userId.domain}",
                creationTime = Clock.System.now(),
                clientId = null
            ),
            allUsers,
            allConversations,
            allMessages
        )
        return ProtoBuf.encodeToByteArray(backupData)
    }
}
