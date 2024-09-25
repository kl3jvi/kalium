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

import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.protobuf.backup.BackupData
import com.wire.kalium.protobuf.backup.BackupInfo
import com.wire.kalium.protobuf.backup.ExportUser
import com.wire.kalium.protobuf.backup.ExportedConversation
import com.wire.kalium.protobuf.backup.ExportedMessage
import pbandk.encodeToByteArray

class MPBackupExporter(val userId: UserId) {
    private val allUsers = mutableListOf<ExportUser>()
    private val allConversations = mutableListOf<ExportedConversation>()
    private val allMessages = mutableListOf<ExportedMessage>()

    fun add(user: ExportUser) {
        allUsers.add(user)
    }

    fun add(conversation: ExportedConversation) {
        allConversations.add(conversation)
    }

    fun add(message: ExportedMessage) {
        allMessages.add(message)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun serialize(): ByteArray {
        val backupData = BackupData(
            BackupInfo(
                platform = "Common",
                version = "1.0",
                userId = "${userId.value}@${userId.domain}",
                creationTime = "Clock.System.now()",
                clientId = "lol"
            ),
            allConversations,
            allMessages,
            allUsers,
        )
        return backupData.encodeToByteArray().also {
            println("!!!BACKUP: ${it.toHexString()}")
        }
    }
}
