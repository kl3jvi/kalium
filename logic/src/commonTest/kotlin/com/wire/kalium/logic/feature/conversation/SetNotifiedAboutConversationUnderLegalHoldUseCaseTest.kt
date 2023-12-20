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
package com.wire.kalium.logic.feature.conversation

import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.functional.Either
import io.mockative.Mock
import io.mockative.any
import io.mockative.eq
import io.mockative.given
import io.mockative.mock
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SetNotifiedAboutConversationUnderLegalHoldUseCaseTest {

    @Test
    fun givenConversationId_whenInvoke_thenRepositoryIsCalledCorrectly() = runTest {
        // given
        val conversationId = ConversationId("conversationId", "domain")
        val (arrangement, useCase) = Arrangement()
            .withSetLegalHoldStatusChangeNotifiedSuccessful()
            .arrange()
        // when
        useCase.invoke(conversationId)
        // then
        verify(arrangement.conversationRepository)
            .suspendFunction(arrangement.conversationRepository::setLegalHoldStatusChangeNotified)
            .with(eq(conversationId))
            .wasInvoked(exactly = once)
    }

    private class Arrangement() {
        @Mock
        val conversationRepository = mock(ConversationRepository::class)

        private val useCase: SetNotifiedAboutConversationUnderLegalHoldUseCase by lazy {
            SetNotifiedAboutConversationUnderLegalHoldUseCaseImpl(conversationRepository)
        }
        fun arrange() = this to useCase
        fun withSetLegalHoldStatusChangeNotifiedSuccessful() = apply {
            given(conversationRepository)
                .suspendFunction(conversationRepository::setLegalHoldStatusChangeNotified)
                .whenInvokedWith(any())
                .thenReturn(Either.Right(true))
        }
    }
}
