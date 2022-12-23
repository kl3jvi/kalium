package com.wire.kalium.logic.feature.call.usecase

import com.wire.kalium.logic.data.call.CallRepository
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.call.CallManager

/**
 * This use case is responsible for un-mute a call.
 */
class UnMuteCallUseCase(
    private val callManager: Lazy<CallManager>,
    private val callRepository: CallRepository
) {
    /**
     * We should call AVS muting method only for established call, otherwise incoming call could mute/un-mute the current call
     * @param conversationId the id of the conversation.
     */
    suspend operator fun invoke(conversationId: ConversationId) {
        callRepository.updateIsMutedById(
            conversationId = conversationId.toString(),
            isMuted = false
        )
        // We should call AVS muting method only for established call, otherwise incoming call could mute the current call
        callRepository.getCallMetadataProfile()[conversationId.toString()]?.establishedTime?.let {
            callManager.value.muteCall(false)
        }
    }
}
