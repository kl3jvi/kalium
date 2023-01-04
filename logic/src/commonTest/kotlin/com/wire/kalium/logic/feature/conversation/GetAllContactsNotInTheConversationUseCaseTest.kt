package com.wire.kalium.logic.feature.conversation

import app.cash.turbine.test
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserRepository
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.functional.Either
import io.mockative.Mock
import io.mockative.anything
import io.mockative.given
import io.mockative.mock
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GetAllContactsNotInTheConversationUseCaseTest {

    @Test
    fun givenSuccessFullResult_whenGettingUsersNotPartofTheConversation_ThenReturnTheResult() = runTest {
        // given
        val (_, getAllContactsNotInTheConversation) = Arrangement()
            .withSuccessFullGetUsersNotPartOfConversation()
            .arrange()

        // when
        getAllContactsNotInTheConversation(ConversationId("someValue", "someDomain")).test {
            // then
            val result = awaitItem()
            assertIs<Result.Success>(result)
            assertTrue { result.contactsNotInConversation == Arrangement.mockAllContacts }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenFailure_whenGettingUsersNotPartofTheConversation_ThenReturnTheResult() = runTest {
        // given
        val (_, getAllContactsNotInTheConversation) = Arrangement()
            .withFailureGetUsersNotPartOfConversation()
            .arrange()

        // when
        getAllContactsNotInTheConversation(ConversationId("someValue", "someDomain")).test {
            // then
            val result = awaitItem()
            assertIs<Result.Failure>(result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class Arrangement {
        companion object {
            val mockAllContacts = listOf(
                OtherUser(
                    id = QualifiedID("someAllContactsValue", "someAllContactsDomain"),
                    name = null,
                    handle = null,
                    email = null,
                    phone = null,
                    accentId = 0,
                    teamId = null,
                    connectionStatus = ConnectionState.ACCEPTED,
                    previewPicture = null,
                    completePicture = null,
                    availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                    userType = UserType.INTERNAL,
                    botService = null,
                    deleted = false
                ),
                OtherUser(
                    id = QualifiedID("someAllContactsValue1", "someAllContactsDomain1"),
                    name = null,
                    handle = null,
                    email = null,
                    phone = null,
                    accentId = 0,
                    teamId = null,
                    connectionStatus = ConnectionState.ACCEPTED,
                    previewPicture = null,
                    completePicture = null,
                    availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                    userType = UserType.INTERNAL,
                    botService = null,
                    deleted = false
                )
            )
        }

        @Mock
        val conversationRepository = mock(ConversationRepository::class)

        @Mock
        val userRepository = mock(UserRepository::class)

        fun withSuccessFullGetUsersNotPartOfConversation(allContacts: List<OtherUser> = mockAllContacts): Arrangement {
            given(userRepository)
                .function(userRepository::observeAllKnownUsersNotInConversation)
                .whenInvokedWith(anything())
                .thenReturn(
                    flowOf(
                        Either.Right(
                            allContacts
                        )
                    )
                )
            return this
        }

        fun withFailureGetUsersNotPartOfConversation(): Arrangement {
            given(userRepository)
                .function(userRepository::observeAllKnownUsersNotInConversation)
                .whenInvokedWith(anything())
                .thenReturn(flowOf(Either.Left(StorageFailure.DataNotFound)))

            return this
        }

        fun arrange() = this to GetAllContactsNotInConversationUseCase(userRepository)
    }

}