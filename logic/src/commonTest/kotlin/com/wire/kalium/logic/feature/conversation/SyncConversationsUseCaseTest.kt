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

import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SystemMessageInserter
import com.wire.kalium.logic.framework.TestConversation
import com.wire.kalium.logic.functional.Either
import io.mockative.any
import io.mockative.eq
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.thenDoNothing
import io.mockative.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SyncConversationsUseCaseTest {
    @Test
    fun givenUseCase_whenInvoked_thenFetchConversations() = runTest {

        val (arrangement, useCase) = Arrangement()
            .withGetConversationsIdsReturning(emptyList())
            .withFetchConversationsSuccessful()
            .arrange()

        useCase.invoke()

        verify(arrangement.conversationRepository)
            .suspendFunction(arrangement.conversationRepository::fetchConversations)
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenProtocolChanges_whenInvoked_thenInsertHistoryLostSystemMessage() = runTest {
        val conversationId = TestConversation.ID
        val (arrangement, useCase) = Arrangement()
            .withGetConversationsIdsReturning(listOf(conversationId), protocol = Conversation.Protocol.PROTEUS)
            .withFetchConversationsSuccessful()
            .withGetConversationsIdsReturning(listOf(conversationId), protocol = Conversation.Protocol.MLS)
            .withInsertHistoryLostProtocolChangedSystemMessageSuccessful()
            .arrange()

        useCase.invoke()

        verify(arrangement.systemMessageInserter)
            .suspendFunction(arrangement.systemMessageInserter::insertHistoryLostProtocolChangedSystemMessage)
            .with(eq(conversationId))
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenProtocolIsUnchanged_whenInvoked_thenDoNotInsertHistoryLostSystemMessage() = runTest {
        val conversationId = TestConversation.ID
        val (arrangement, useCase) = Arrangement()
            .withGetConversationsIdsReturning(listOf(conversationId), protocol = Conversation.Protocol.PROTEUS)
            .withFetchConversationsSuccessful()
            .withGetConversationsIdsReturning(emptyList(), protocol = Conversation.Protocol.MLS)
            .withInsertHistoryLostProtocolChangedSystemMessageSuccessful()
            .arrange()

        useCase.invoke()

        verify(arrangement.systemMessageInserter)
            .suspendFunction(arrangement.systemMessageInserter::insertHistoryLostProtocolChangedSystemMessage)
            .with(eq(conversationId))
            .wasNotInvoked()
    }

    private class Arrangement {

        val conversationRepository = mock(ConversationRepository::class)
        val systemMessageInserter = mock(SystemMessageInserter::class)

        fun withFetchConversationsSuccessful() = apply {
            given(conversationRepository)
                .suspendFunction(conversationRepository::fetchConversations)
                .whenInvoked()
                .thenReturn(Either.Right(Unit))
        }

        fun withGetConversationsIdsReturning(
            conversationIds: List<ConversationId>,
            protocol: Conversation.Protocol? = null
        ) = apply {
            given(conversationRepository)
                .suspendFunction(conversationRepository::getConversationIds)
                .whenInvokedWith(eq(Conversation.Type.GROUP), protocol?.let { eq(it) } ?: any(), eq(null))
                .thenReturn(Either.Right(conversationIds))
        }

        fun withInsertHistoryLostProtocolChangedSystemMessageSuccessful() = apply {
            given(systemMessageInserter)
                .suspendFunction(systemMessageInserter::insertHistoryLostProtocolChangedSystemMessage)
                .whenInvokedWith()
                .thenDoNothing()
        }

        fun arrange() = this to SyncConversationsUseCaseImpl(
            conversationRepository,
            systemMessageInserter
        )
    }
}
