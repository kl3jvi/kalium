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

package com.wire.kalium.logic.data.call

import com.wire.kalium.logic.data.id.CurrentClientIdProvider
import com.wire.kalium.logic.data.user.UserRepository
import com.wire.kalium.logic.functional.fold

internal class CallingParticipantsOrderV2Impl(
    private val userRepository: UserRepository,
    private val currentClientIdProvider: CurrentClientIdProvider,
    private val participantsFilter: ParticipantsFilter,
    private val participantsOrderByName: ParticipantsOrderByName,
) : CallingParticipantsOrder {

    // todo change to sequence.
    override suspend fun reorderItems(participants: List<Participant>): List<Participant> {
        return if (participants.isNotEmpty()) {
            currentClientIdProvider().fold({
                participants
            }, { selfClientId ->
                val (otherParticipants, selfParticipant) = participants.partition { it.clientId != selfClientId.value }

                val (participantsSharingScreen, participantsNotSharingScreen) = otherParticipants.partition { it.isSharingScreen }
                val (participantsWithVideoOn, participantsWithVideoOff) = participantsNotSharingScreen.partition { it.isCameraOn }

                val participantsSharingScreenSortedByName = participantsOrderByName.sortItems(participantsSharingScreen)
                val participantsWithVideoOnSortedByName = participantsOrderByName.sortItems(participantsWithVideoOn)
                val participantsWithVideoOffSortedByName = participantsOrderByName.sortItems(participantsWithVideoOff)

                val sortedParticipantsByName =
                    participantsSharingScreenSortedByName + participantsWithVideoOnSortedByName + participantsWithVideoOffSortedByName

                return selfParticipant + sortedParticipantsByName
            })
        } else participants
    }
}
