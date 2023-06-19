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
package com.wire.kalium.logic.data.conversation

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.IdMapper
import com.wire.kalium.logic.di.MapperProvider
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.wrapStorageRequest
import com.wire.kalium.network.api.base.authenticated.conversation.ConversationResponse
import com.wire.kalium.persistence.dao.ConversationDAO
import com.wire.kalium.persistence.dao.ConversationIDEntity

/**
 * Handles the addition of members to a new conversation and the related system messages when a conversation is started.
 * Either all users are added or some of them could fail to be added.
 *
 * TODO(offline backend branch): And add failed members handling in api v4
 */
internal interface NewConversationMembersRepository {
    suspend fun persistMembersAdditionToTheConversation(
        conversationId: ConversationIDEntity,
        conversationResponse: ConversationResponse,
    ): Either<CoreFailure, Unit>
}

internal class NewConversationMembersRepositoryImpl(
    private val conversationDAO: ConversationDAO,
    private val newGroupConversationSystemMessagesCreator: Lazy<NewGroupConversationSystemMessagesCreator>,
    private val idMapper: IdMapper = MapperProvider.idMapper(),
    private val memberMapper: MemberMapper = MapperProvider.memberMapper()
) : NewConversationMembersRepository {

    override suspend fun persistMembersAdditionToTheConversation(
        conversationId: ConversationIDEntity,
        conversationResponse: ConversationResponse,
    ) = wrapStorageRequest {
        conversationDAO.insertMembersWithQualifiedId(
            memberMapper.fromApiModelToDaoModel(conversationResponse.members),
            idMapper.fromApiToDao(conversationResponse.id)
        )
    }.flatMap {
        newGroupConversationSystemMessagesCreator.value.conversationResolvedMembersAddedAndFailed(
            conversationId,
            conversationResponse
        )
    }

}