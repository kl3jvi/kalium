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

import com.oldguy.common.io.File
import com.oldguy.common.io.FileMode
import com.oldguy.common.io.ZipEntry
import com.oldguy.common.io.ZipFile
import com.wire.backup.data.BackupData

class MPBackupExporter(exportPath: String) {

    private val zipFile = ZipFile(
        File(exportPath),
        FileMode.Write
    )

    private val allUsers = ArrayList<BackupData.User>()
    private val allConversations = ArrayList<BackupData.Conversation>()
    private val allMessages = ArrayList<BackupData.Message>()

    fun add(user: BackupData.User) {
        allUsers.add(user)
    }

    fun add(conversation: BackupData.Conversation) {
        allConversations.add(conversation)
    }

    fun add(message: BackupData.Message) {
        allMessages.add(message)
    }

    suspend fun flushToFile() {
        // TODO: maybe perform BackupData -> storage format in parallel, instead of one entry at a time.
        zipFile.use { file ->
            file.addEntry(
                ZipEntry("messages.json"),
            ) {
                // TODO: avoid parsing to String and then to ByteArray
                //                    Maybe we can just output in another format like Protobuf
                //                    And buffer it so we avoid having all of this in memory
//                     KtxWebSerializer.json.encodeToString().encodeToByteArray()
                TODO("Map all messages to a JSON and output as a ByteArray")
            }
        }
    }
}
