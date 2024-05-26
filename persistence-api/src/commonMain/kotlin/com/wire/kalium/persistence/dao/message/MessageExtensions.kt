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

package com.wire.kalium.persistence.dao.message

import app.cash.paging.PagingConfig
import com.wire.kalium.persistence.dao.ConversationIDEntity
import com.wire.kalium.persistence.dao.asset.AssetMessageEntity

interface MessageExtensions {
    fun getPagerForConversation(
        conversationId: ConversationIDEntity,
        visibilities: Collection<MessageEntity.Visibility>,
        pagingConfig: PagingConfig,
        startingOffset: Long
    ): KaliumPager<MessageEntity>

    fun getPagerForMessagesSearch(
        searchQuery: String,
        conversationId: ConversationIDEntity,
        pagingConfig: PagingConfig,
        startingOffset: Long
    ): KaliumPager<MessageEntity>

    fun getPagerForMessageAssetsWithoutImage(
        conversationId: ConversationIDEntity,
        mimeTypes: Set<String>,
        pagingConfig: PagingConfig,
        startingOffset: Long
    ): KaliumPager<MessageEntity>

    fun getPagerForMessageAssetImage(
        conversationId: ConversationIDEntity,
        mimeTypes: Set<String>,
        pagingConfig: PagingConfig,
        startingOffset: Long
    ): KaliumPager<AssetMessageEntity>
}
