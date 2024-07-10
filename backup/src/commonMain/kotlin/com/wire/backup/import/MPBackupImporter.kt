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

class MPBackupImporter(pathToFile: String) {

    private val zipFile = ZipFile(
        File(pathToFile),
        FileMode.Read
    )

    suspend fun import(onDataImported: (BackupData) -> Unit) {

        zipFile.readEntry(ZipEntries.INFO.entryName) { entry, content, count, isLast ->

        }
        zipFile.useEntries { entry ->
            when (entry.name) {
                ZipEntries.MESSAGES.entryName -> {

                }

                ZipEntries.INFO.entryName -> {

                }

                ZipEntries.CONVERSATIONS.entryName -> {

                }
            }
        }
    }
}
