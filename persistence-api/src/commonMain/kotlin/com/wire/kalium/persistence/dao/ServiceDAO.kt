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
package com.wire.kalium.persistence.dao

import kotlinx.coroutines.flow.Flow

data class ServiceEntity(
    val id: BotIdEntity,
    val name: String,
    val description: String,
    val summary: String,
    val enabled: Boolean,
    val tags: List<String>,
    val previewAssetId: UserAssetIdEntity?,
    val completeAssetId: UserAssetIdEntity?
)

data class ServiceViewEntity(
    val service: ServiceEntity,
    val isMember: Boolean
)

interface ServiceDAO {
    suspend fun byId(id: BotIdEntity): ServiceEntity?
    suspend fun observeIsServiceMember(id: BotIdEntity, conversationId: ConversationIDEntity): Flow<QualifiedIDEntity?>
    suspend fun getAllServices(): Flow<List<ServiceEntity>>
    suspend fun searchServicesByName(query: String): Flow<List<ServiceEntity>>
    suspend fun insert(service: ServiceEntity)
    suspend fun insertMultiple(serviceList: List<ServiceEntity>)

}


