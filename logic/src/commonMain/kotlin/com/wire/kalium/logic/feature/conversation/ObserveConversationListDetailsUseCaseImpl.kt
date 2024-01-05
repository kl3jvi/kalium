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

package com.wire.kalium.logic.feature.conversation

import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationRepository
import kotlinx.coroutines.flow.Flow

/**
 * This use case will observe and return the list of conversation details for the current user.
 * @see ConversationDetails
 */
fun interface ObserveConversationListDetailsUseCase {
    suspend operator fun invoke(fromArchive: Boolean): Flow<List<ConversationDetails>>
}

internal class ObserveConversationListDetailsUseCaseImpl(
    private val conversationRepository: ConversationRepository,
) : ObserveConversationListDetailsUseCase {

    override suspend operator fun invoke(fromArchive: Boolean): Flow<List<ConversationDetails>> {
        return conversationRepository.observeConversationListDetails(fromArchive)
    }
}
