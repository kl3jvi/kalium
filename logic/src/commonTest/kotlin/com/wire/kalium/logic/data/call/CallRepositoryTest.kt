package com.wire.kalium.logic.data.call

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.util.shouldSucceed
import com.wire.kalium.network.api.call.CallApi
import com.wire.kalium.network.utils.NetworkResponse
import io.ktor.util.reflect.instanceOf
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import io.mockative.oneOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CallRepositoryTest {

    @Mock
    private val callApi = mock(classOf<CallApi>())

    private lateinit var mapOfCallProfiles: Map<String, Call>
    private lateinit var startedCall: Call
    private lateinit var answeredCall: Call
    private lateinit var incomingCall: Call
    private lateinit var establishedCall: Call

    private lateinit var callRepository: CallRepository

    @BeforeTest
    fun setUp() {
        callRepository = CallDataSource(
            callApi = callApi
        )
        startedCall = provideCall(sharedConversationId, CallStatus.STARTED)
        answeredCall = provideCall(conversationIdAnsweredCall, CallStatus.ANSWERED)
        incomingCall = provideCall(conversationIdIncomingCall, CallStatus.INCOMING)
        establishedCall = provideCall(conversationIdEstablishedCall, CallStatus.ESTABLISHED)

        mapOfCallProfiles = mapOf(
            startedCall.conversationId.toString() to startedCall,
            incomingCall.conversationId.toString() to incomingCall,
            establishedCall.conversationId.toString() to establishedCall,
            answeredCall.conversationId.toString() to answeredCall
        )
    }

    @Test
    fun whenRequestingCallConfig_withNoLimitParam_ThenAResultIsReturned() = runTest {
        given(callApi)
            .suspendFunction(callApi::getCallConfig)
            .whenInvokedWith(oneOf(null))
            .thenReturn(NetworkResponse.Success(CALL_CONFIG_API_RESPONSE, mapOf(), 200))

        val result = callRepository.getCallConfigResponse(limit = null)

        result.shouldSucceed {
            assertEquals(CALL_CONFIG_API_RESPONSE, it)
        }
    }

    @Test
    fun givenEmptyListOfCalls_whenGetAllCallsIsCalled_thenReturnAnEmptyListOfCalls() = runTest {
        val calls = callRepository.callsFlow()

        assertEquals(0, calls.first().size)
    }

    @Test
    fun givenAListOfCallProfiles_whenGetAllCallsIsCalled_thenReturnAListOfCalls() = runTest {
        callRepository.updateCallProfileFlow(CallProfile(mapOfCallProfiles))

        val calls = callRepository.callsFlow()

        assertEquals(mapOfCallProfiles.size, calls.first().size)
        assertTrue(calls.first()[0].instanceOf(Call::class))
    }

    @Test
    fun givenACallObject_whenCreateCallCalled_thenAddThatCallToTheFlow() = runTest {
        callRepository.updateCallProfileFlow(CallProfile(mapOf(startedCall.conversationId.toString() to startedCall)))

        callRepository.createCall(answeredCall)

        val calls = callRepository.callsFlow()
        assertEquals(2, calls.first().size)
        assertEquals(calls.first()[0], startedCall)
        assertEquals(calls.first()[1], answeredCall)
    }

    @Test
    fun givenACallObjectWithSameConversationIdAsAnotherOneInTheFlow_whenCreateCallCalled_thenReplaceTheCurrent() = runTest {
        val incomingCall2 = provideCall(sharedConversationId, CallStatus.INCOMING)
        callRepository.updateCallProfileFlow(CallProfile(mapOfCallProfiles))

        callRepository.createCall(incomingCall2)

        val calls = callRepository.callsFlow()
        assertEquals(mapOfCallProfiles.size, calls.first().size)
        assertEquals(calls.first()[0], incomingCall2)
    }

    @Test
    fun givenAConversationIdThatDoesNotExistsInTheFlow_whenUpdateCallStatusIsCalled_thenDoNotUpdateTheFlow() = runTest {
        callRepository.updateCallStatusById(randomConversationIdString, CallStatus.INCOMING)

        val calls = callRepository.callsFlow()
        assertEquals(0, calls.first().size)
    }

    @Test
    fun givenAConversationIdThatExistsInTheFlow_whenUpdateCallStatusIsCalled_thenUpdateCallStatusInTheFlow() = runTest {
        callRepository.updateCallProfileFlow(CallProfile(mapOfCallProfiles))

        callRepository.updateCallStatusById(startedCall.conversationId.toString(), CallStatus.ESTABLISHED)

        val calls = callRepository.callsFlow()
        assertEquals(mapOfCallProfiles.size, calls.first().size)
        assertEquals(calls.first()[0].status, CallStatus.ESTABLISHED)
    }


    @Test
    fun givenAConversationIdThatDoesNotExistsInTheFlow_whenUpdateIsMutedByIdIsCalled_thenDoNotUpdateTheFlow() = runTest {
        callRepository.updateIsMutedById(randomConversationIdString, false)

        val calls = callRepository.callsFlow()
        assertEquals(0, calls.first().size)
    }

    @Test
    fun givenAConversationIdThatExistsInTheFlow_whenUpdateIsMutedByIdIsCalled_thenUpdateCallStatusInTheFlow() = runTest {
        val expectedValue = true
        callRepository.updateCallProfileFlow(CallProfile(mapOfCallProfiles))

        callRepository.updateIsMutedById(startedCall.conversationId.toString(), expectedValue)

        val calls = callRepository.callsFlow()
        assertEquals(mapOfCallProfiles.size, calls.first().size)
        assertEquals(calls.first()[0].isMuted, expectedValue)
    }

    @Test
    fun givenAConversationIdThatDoesNotExistsInTheFlow_whenUpdateIsCameraOnIdIsCalled_thenDoNotUpdateTheFlow() = runTest {
        callRepository.updateIsCameraOnById(randomConversationIdString, false)

        val calls = callRepository.callsFlow()
        assertEquals(0, calls.first().size)
    }

    @Test
    fun givenAConversationIdThatExistsInTheFlow_whenUpdateIsCameraOnByIdIsCalled_thenUpdateCallStatusInTheFlow() = runTest {
        val expectedValue = true
        callRepository.updateCallProfileFlow(CallProfile(mapOfCallProfiles))

        callRepository.updateIsCameraOnById(startedCall.conversationId.toString(), expectedValue)

        val calls = callRepository.callsFlow()
        assertEquals(mapOfCallProfiles.size, calls.first().size)
        assertEquals(calls.first()[0].isCameraOn, expectedValue)
    }

    @Test
    fun givenAConversationId_whenRemoveCallByIdIsCalled_thenRemoveThatCallFromTheFlow() = runTest {
        callRepository.updateCallProfileFlow(CallProfile(mapOfCallProfiles))

        callRepository.removeCallById(startedCall.conversationId.toString())

        val calls = callRepository.callsFlow()
        assertEquals(mapOfCallProfiles.size - 1, calls.first().size)
        val removedItem = calls.first().find {
            it.conversationId == startedCall.conversationId
        }
        assertNull(removedItem)
    }

    @Test
    fun givenNoIncomingCallsInTheFlow_whenGetIncomingCallsIsCalled_thenReturnAnEmptyListInTheFlow() = runTest {
        val calls = callRepository.incomingCallsFlow()

        assertEquals(0, calls.first().size)
    }

    @Test
    fun givenSomeIncomingCallsInTheFlow_whenGetIncomingCallsIsCalled_thenReturnTheListOfIncomingCallsInTheFlow() = runTest {
        callRepository.updateCallProfileFlow(CallProfile(mapOfCallProfiles))

        val calls = callRepository.incomingCallsFlow()

        assertEquals(1, calls.first().size)
        assertEquals(calls.first()[0], incomingCall)
    }

    @Test
    fun givenNoOngoingCallsInTheFlow_whenGetOngoingCallIsCalled_thenReturnAnEmptyListInTheFlow() = runTest {
        val calls = callRepository.incomingCallsFlow()

        assertEquals(0, calls.first().size)
    }

    @Test
    fun givenSomeOngoingCallsInTheFlow_whenGetOngoingCallIsCalled_thenReturnTheListOfOngoingCallsInTheFlow() = runTest {
        callRepository.updateCallProfileFlow(CallProfile(mapOfCallProfiles))

        val calls = callRepository.ongoingCallsFlow()

        assertEquals(2, calls.first().size)
        assertEquals(calls.first()[0], establishedCall)
        assertEquals(calls.first()[1], answeredCall)
    }

    @Test
    fun givenNewCallParticipants_whenUpdatingParticipants_thenCallProfileIsUpdated() = runTest {
        callRepository.updateCallProfileFlow(
            CallProfile(
                mapOf(establishedCall.conversationId.toString() to establishedCall)
            )
        )

        val expectedParticipants = listOf(
            Participant(
                id = QualifiedID(
                    value = "value1",
                    domain = "domain1"
                ),
                clientId = "clientid",
                muted = true
            )
        )

        val calls = callRepository.callsFlow()

        callRepository.updateCallParticipants(
            conversationId = establishedCall.conversationId.toString(),
            participants = expectedParticipants
        )

        assertEquals(expectedParticipants, calls.first()[0].participants)
    }

    @Test
    fun givenActiveSpeakers_whenUpdatingActiveSpeakers_thenCallProfileIsUpdated() = runTest {
        callRepository.updateCallProfileFlow(
            CallProfile(
                mapOf(establishedCall.conversationId.toString() to establishedCall)
            )
        )

        val calls = callRepository.callsFlow()

        val participants = listOf(
            Participant(
                id = QualifiedID(
                    value = "value1",
                    domain = "domain1"
                ),
                clientId = "clientid",
                muted = false,
                isSpeaking = false
            )
        )

        callRepository.updateCallParticipants(
            conversationId = establishedCall.conversationId.toString(),
            participants = participants
        )

        val expectedActiveSpeakers = CallActiveSpeakers(
            activeSpeakers = listOf(
                CallActiveSpeaker(
                    userId = "value1@domain1",
                    clientId = "clientid",
                    audioLevel = 1,
                    audioLevelNow = 1
                )
            )
        )

        callRepository.updateParticipantsActiveSpeaker(
            conversationId = establishedCall.conversationId.toString(),
            activeSpeakers = expectedActiveSpeakers
        )

        assertEquals(true, calls.first()[0].participants.first().isSpeaking)
    }

    private fun provideCall(id: ConversationId, status: CallStatus) = Call(
        conversationId = id,
        status = status,
        isMuted = false,
        isCameraOn = false,
        callerId = "caller-id",
        participants = listOf(),
        maxParticipants = 0
    )

    private companion object {
        const val CALL_CONFIG_API_RESPONSE = "{'call':'success','config':'dummy_config'}"
        private const val randomConversationIdString = "random@domain"
        private val sharedConversationId = ConversationId("value", "domain")
        private val conversationIdAnsweredCall = ConversationId("value2", "domain2")
        private val conversationIdIncomingCall = ConversationId("value3", "domain3")
        private val conversationIdEstablishedCall = ConversationId("value4", "domain4")
    }
}
