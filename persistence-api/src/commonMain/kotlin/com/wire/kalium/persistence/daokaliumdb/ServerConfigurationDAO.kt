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

package com.wire.kalium.persistence.daokaliumdb

import com.wire.kalium.persistence.dao.UserIDEntity
import com.wire.kalium.persistence.model.ServerConfigEntity
import com.wire.kalium.persistence.model.ServerConfigWithUserIdEntity
import kotlinx.coroutines.flow.Flow

interface ServerConfigurationDAO {
    suspend fun deleteById(id: String)
    suspend fun insert(insertData: InsertData)
    suspend fun allConfigFlow(): Flow<List<ServerConfigEntity>>
    suspend fun allConfig(): List<ServerConfigEntity>
    fun configById(id: String): ServerConfigEntity?
    suspend fun configByLinks(links: ServerConfigEntity.Links): ServerConfigEntity?
    suspend fun updateApiVersion(id: String, commonApiVersion: Int)
    suspend fun updateApiVersionAndDomain(id: String, domain: String, commonApiVersion: Int)
    suspend fun configForUser(userId: UserIDEntity): ServerConfigEntity?
    suspend fun setFederationToTrue(id: String)
    suspend fun getServerConfigsWithAccIdWithLastCheckBeforeDate(date: String): Flow<List<ServerConfigWithUserIdEntity>>
    suspend fun updateBlackListCheckDate(configIds: Set<String>, date: String)

    data class InsertData(
        val id: String,
        val apiBaseUrl: String,
        val accountBaseUrl: String,
        val webSocketBaseUrl: String,
        val blackListUrl: String,
        val teamsUrl: String,
        val websiteUrl: String,
        val title: String,
        val isOnPremises: Boolean,
        val federation: Boolean,
        val domain: String?,
        val commonApiVersion: Int,
        val apiProxyHost: String?,
        val apiProxyNeedsAuthentication: Boolean?,
        val apiProxyPort: Int?
    )
}
