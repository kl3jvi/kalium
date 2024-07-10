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
package com.wire.backup.import

import com.oldguy.common.io.File
import com.oldguy.common.io.FileMode
import com.oldguy.common.io.ZipFile
import com.wire.backup.data.BackupData
import com.wire.backup.zip.ZipEntries
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.web.KtxWebSerializer
import com.wire.kalium.logic.data.web.WebEventContent
import kotlinx.datetime.Instant
import okio.Buffer

class MPBackupImporter(pathToFile: String) {

    private val zipFile = ZipFile(
        File(pathToFile),
        FileMode.Read
    )

    private suspend fun getWebEventsFromBackup(): List<WebEventContent> {
        // TODO: Read other backed up files
        val buffer = Buffer()
        zipFile.readEntry(ZipEntries.EVENTS.entryName) { entry, content, count, isLast ->
            buffer.write(content)
        }
        val fileString = buffer.readUtf8()
        return KtxWebSerializer.json.decodeFromString<List<WebEventContent>>(fileString)
    }

    suspend fun import(onDataImported: (BackupData) -> Unit) {
        val webStuff = getWebEventsFromBackup()
        webStuff.forEach { webEvent ->
            // webEvent -> BackupData
            if (webEvent !is WebEventContent.Conversation) return@forEach

            val backupData = when (webEvent) {
                is WebEventContent.Conversation.AssetMessage -> null
                is WebEventContent.Conversation.KnockMessage -> null
                is WebEventContent.Conversation.NewGroup -> null
                is WebEventContent.Conversation.TextMessage -> BackupData.Message.Text(
                    messageId = webEvent.id,
                    conversationId = webEvent.qualifiedConversation,
                    senderUserId = webEvent.qualifiedFrom!!, // TODO: Bang bang!
                    time = Instant.parse(webEvent.time),
                    senderClientId = webEvent.fromClientId!!, // TODO: Bang bang!
                    textValue = webEvent.data.text
                )

                WebEventContent.Unknown -> null
            }
            backupData?.let { onDataImported(it) }
        }
    }
}