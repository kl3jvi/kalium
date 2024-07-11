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
package com.wire.backup

import com.wire.backup.data.BackupData
import com.wire.backup.data.BackupMetadata
import com.wire.backup.export.MPBackupExporter
import com.wire.backup.import.MPBackupImporter
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertContentEquals

class BackupEndToEndTest {

    private val rootPath = createTempDirectory().toAbsolutePath().pathString

    @Test
    fun givenBackedUpMessages_whenRestoring_thenShouldReadTheSameContent() = runTest {
        val targetFile = File(rootPath, "backup.wbu")

        val expectedMessage = BackupData.Message.Text(
            "messageId",
            ConversationId("value", "domain"),
            UserId("user", "domain"),
            Clock.System.now(),
            "clientId",
            "Hello from the backup!"
        )
        MPBackupExporter(targetFile.path, metaBackupData).apply {
            add(expectedMessage)
            flushToFile()
        }

        val restoredMessages = mutableListOf<BackupData.Message>()
        MPBackupImporter(targetFile.path).import { importedData ->
            when (importedData) {
                is BackupData.Message -> restoredMessages.add(importedData)
                is BackupData.Conversation -> {}
            }
        }
        assertContentEquals(listOf(expectedMessage), restoredMessages)
    }


    companion object {
        private val metaBackupData = BackupMetadata(
            platform = "Android",
            version = "21",
            "userId",
            creationTime = "2024-07-10T14:46:21.723Z",
            clientId = "clientId"
        )
    }
}
