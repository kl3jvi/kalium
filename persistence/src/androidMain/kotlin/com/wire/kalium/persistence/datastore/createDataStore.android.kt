/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.kalium.persistence.datastore

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import okio.Path.Companion.toPath


private lateinit var dataStore: MultiplatformDataStore

private val lock = SynchronizedObject()

/**
 * Gets the singleton DataStore instance, creating it if necessary.
 */
fun getDataStore(context: Context): MultiplatformDataStore =
    synchronized(lock) {
        if (::dataStore.isInitialized) {
            dataStore
        } else {
            MultiplatformDataStore(
                PreferenceDataStoreFactory.createWithPath(
                    produceFile = { producePath(context).toPath() }
                )
            ).also {
                dataStore = it
            }
        }
    }

fun producePath(context: Context): String =
    context.filesDir.resolve(DATA_STORE_FILENAME).absolutePath
