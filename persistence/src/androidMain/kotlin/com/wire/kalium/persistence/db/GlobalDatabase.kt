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

package com.wire.kalium.persistence.db

import com.wire.kalium.persistence.GlobalDatabase
import com.wire.kalium.persistence.util.FileNameUtil
import kotlinx.coroutines.CoroutineDispatcher

actual fun globalDatabaseProvider(
    platformDatabaseData: PlatformDatabaseData,
    queriesContext: CoroutineDispatcher,
    passphrase: GlobalDatabaseSecret?,
    enableWAL: Boolean
): GlobalDatabaseBuilder {
    val schema = GlobalDatabase.Schema
    val dbName = FileNameUtil.globalDBName()
    val driver = databaseDriver(
        platformDatabaseData.context,
        dbName,
        passphrase?.value,
        schema
    ) {
        isWALEnabled = enableWAL
    }
    return GlobalDatabaseBuilder(driver, platformDatabaseData, queriesContext)
}

actual fun nuke(platformDatabaseData: PlatformDatabaseData): Boolean {
    // driver.close()
    return platformDatabaseData.context.deleteDatabase(FileNameUtil.globalDBName())
}
