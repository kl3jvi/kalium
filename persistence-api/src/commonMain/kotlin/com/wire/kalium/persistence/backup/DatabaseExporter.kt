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

package com.wire.kalium.persistence.backup

import com.wire.kalium.persistence.db.UserDBSecret

interface DatabaseExporter {

    /**
     * Export the user DB to a plain DB
     * @return the path to the plain DB file, null if the file was not created
     */
    fun exportToPlainDB(localDBPassphrase: UserDBSecret?): String?

    /**
     * Delete the backup file and any temp data was created during the backup process
     * need to be called after the backup is done wether the user exported the file or not
     * even if the backup failed
     * @return true if the file was deleted, false otherwise
     */
    fun deleteBackupDBFile(): Boolean
}

// THIS MUST MATCH THE PLAIN DATABASE ALIAS IN DumpContent.sq DO NOT CHANGE
const val MAIN_DB_ALIAS = "local_db"

