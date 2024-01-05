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
package com.wire.kalium.logic.feature.scenario

import com.sun.jna.Memory
import com.wire.kalium.calling.Calling
import com.wire.kalium.calling.types.Size_t
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.cache.SelfConversationIdProvider
import com.wire.kalium.logic.data.call.mapper.CallMapperImpl
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.feature.call.CallManagerImpl
import com.wire.kalium.logic.feature.call.scenario.OnSendOTR
import com.wire.kalium.logic.feature.message.MessageSender
import com.wire.kalium.logic.data.message.MessageTarget
import com.wire.kalium.logic.framework.TestConversation
import com.wire.kalium.logic.framework.TestUser
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.test_util.TestKaliumDispatcher
import io.mockative.Mock
import io.mockative.any
import io.mockative.classOf
import io.mockative.given
import io.mockative.matching
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

class OnSendOTRTest {

    @Test
    fun givenMyClientsOnlyIsTrue_whenSending_messageIsSentInSelfConversations() = runTest(TestKaliumDispatcher.main) {
        val (arrangement, onSendOTR) = Arrangement()
            .givenSelfConversationIdProviderReturns(Either.Right(listOf(Arrangement.selfConversationId)))
            .givenSendMessageSuccessful()
            .arrange()

        val content = "calling_content"
        val memory = Memory((content.length + 1).toLong())
        memory.setString(0, content, CallManagerImpl.UTF8_ENCODING)

        onSendOTR.onSend(
            context = null,
            remoteConversationId = Arrangement.conversationId.toString(),
            remoteSelfUserId = Arrangement.selfUserId.toString(),
            remoteClientIdSelf = Arrangement.selfUserClientId.value,
            targetRecipientsJson = null,
            clientIdDestination = null,
            data = memory.share(0),
            length = Size_t(memory.size()),
            isTransient = true,
            myClientsOnly = true,
            arg = null
        )
        yield()

        verify(arrangement.messageSender)
            .suspendFunction(arrangement.messageSender::sendMessage)
            .with(
                matching { it.conversationId == Arrangement.selfConversationId },
                matching { it is MessageTarget.Conversation },
            )
            .wasInvoked(exactly = once)
    }

    @Test
    fun givenMyClientsOnlyIsFalse_whenSending_messageIsSentInTargetConversation() = runTest(TestKaliumDispatcher.main) {
        val (arrangement, onSendOTR) = Arrangement()
            .givenSendMessageSuccessful()
            .arrange()

        val content = "calling_content"
        val memory = Memory((content.length + 1).toLong())
        memory.setString(0, content, CallManagerImpl.UTF8_ENCODING)

        onSendOTR.onSend(
            context = null,
            remoteConversationId = Arrangement.conversationId.toString(),
            remoteSelfUserId = Arrangement.selfUserId.toString(),
            remoteClientIdSelf = Arrangement.selfUserClientId.value,
            targetRecipientsJson = null,
            clientIdDestination = null,
            data = memory.share(0),
            length = Size_t(memory.size()),
            isTransient = true,
            myClientsOnly = false,
            arg = null
        )
        yield()

        verify(arrangement.messageSender)
            .suspendFunction(arrangement.messageSender::sendMessage)
            .with(
                matching { it.conversationId == Arrangement.conversationId },
                matching { it is MessageTarget.Conversation },
            )
            .wasInvoked(exactly = once)
    }

    internal class Arrangement {

        @Mock
        val calling = mock(classOf<Calling>())

        @Mock
        val messageSender = mock(classOf<MessageSender>())

        @Mock
        val selfConversationIdProvider = mock(classOf<SelfConversationIdProvider>())

        val qualifiedIdMapper = QualifiedIdMapperImpl(TestUser.SELF.id)

        val callMapper = CallMapperImpl(qualifiedIdMapper)

        fun arrange() = this to OnSendOTR(
            CompletableDeferred(),
            calling,
            qualifiedIdMapper,
            TestUser.SELF.id.toString(),
                    "self_client_id",
            messageSender,
            selfConversationIdProvider,
            CoroutineScope(TestKaliumDispatcher.main),
            callMapper
        )

        companion object {
            val conversationId = TestConversation.GROUP().id
            val selfConversationId = TestConversation.SELF().id
            val selfUserId = TestUser.SELF.id
            val selfUserClientId = ClientId("self_client")
        }

        fun givenSelfConversationIdProviderReturns(result: Either<StorageFailure, List<ConversationId>>) = apply {
            given(selfConversationIdProvider)
                .suspendFunction(selfConversationIdProvider::invoke)
                .whenInvoked()
                .thenReturn(result)
        }

        fun givenSendMessageSuccessful() = apply {
            given(messageSender)
                .suspendFunction(messageSender::sendMessage)
                .whenInvokedWith(any(), any())
                .thenReturn(Either.Right(Unit))
        }
    }

}
