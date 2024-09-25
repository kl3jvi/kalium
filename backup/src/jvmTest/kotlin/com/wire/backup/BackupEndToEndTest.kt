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

import com.wire.backup.data.Message
import com.wire.backup.export.MPBackupExporter
import com.wire.backup.import.BackupImportResult
import com.wire.backup.import.MPBackupImporter
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

class BackupEndToEndTest {

    @Test
    fun givenBackedUpMessages_whenRestoring_thenShouldReadTheSameContent() = runTest {
        val expectedMessage = Message.Text(
            "messageId",
            ConversationId("value", "domain"),
            UserId("user", "domain"),
            Clock.System.now(),
            "clientId",
            "Hello from the backup!"
        )
        val exporter = MPBackupExporter(UserId("eghyue", "potato"))
        exporter.add(expectedMessage)
        val encoded = exporter.serialize()

        val result = MPBackupImporter("potato").import(encoded)
        assertIs<BackupImportResult.Success>(result)
        assertContentEquals(listOf(expectedMessage), result.backupData.messages)
    }

    @Test
    fun givenBackUpDataIsUnrecognisable_whenRestoring_thenShouldReturnParsingError() = runTest {
        val result = MPBackupImporter("potato").import(byteArrayOf(0x42, 0x42, 0x42))
        assertIs<BackupImportResult.ParsingFailure>(result)
    }
}
