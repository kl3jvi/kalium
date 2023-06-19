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
package com.wire.kalium.logic.feature.selfDeletingMessages

import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.configuration.UserConfigRepository
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationRepository
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.util.isPositiveNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Duration.Companion.ZERO

/**
 * When invoked, this use case will start observing on a given conversation, the currently applied [SelfDeletionTimer]
 */
interface ObserveSelfDeletionTimerSettingsForConversationUseCase {
    /**
     * @param conversationId the conversation id to observe
     * @param considerSelfUserSettings if true, the user settings will be considered,
     *          otherwise only the team and conversation settings will be considered
     */
    suspend operator fun invoke(conversationId: ConversationId, considerSelfUserSettings: Boolean): Flow<SelfDeletionTimer>
}

class ObserveSelfDeletionTimerSettingsForConversationUseCaseImpl internal constructor(
    private val userConfigRepository: UserConfigRepository,
    private val conversationRepository: ConversationRepository
) : ObserveSelfDeletionTimerSettingsForConversationUseCase {

    override suspend fun invoke(conversationId: ConversationId, considerSelfUserSettings: Boolean): Flow<SelfDeletionTimer> =
        userConfigRepository.observeTeamSettingsSelfDeletingStatus()
            .combine(
                conversationRepository.observeById(conversationId)
            ) { teamSettings, conversationDetailsEither ->
                teamSettings.fold({
                    onTeamEnabled(conversationDetailsEither, considerSelfUserSettings)
                }, {
                    when (it.enforcedSelfDeletionTimer) {
                        TeamSelfDeleteTimer.Disabled -> SelfDeletionTimer.Disabled
                        TeamSelfDeleteTimer.Enabled -> onTeamEnabled(conversationDetailsEither, considerSelfUserSettings)
                        is TeamSelfDeleteTimer.Enforced -> SelfDeletionTimer.Enforced.ByTeam(
                            it.enforcedSelfDeletionTimer.enforcedDuration
                        )
                    }
                })
            }

    private fun onTeamEnabled(conversation: Either<StorageFailure, Conversation>, considerSelfUserSettings: Boolean): SelfDeletionTimer =
        conversation.fold({
            SelfDeletionTimer.Enabled(ZERO)
        }, {
            when {
                it.messageTimer.isPositiveNotNull() -> SelfDeletionTimer.Enforced.ByGroup(it.messageTimer)
                considerSelfUserSettings && it.userMessageTimer.isPositiveNotNull() -> SelfDeletionTimer.Enabled(it.userMessageTimer)
                else -> SelfDeletionTimer.Enabled(ZERO)
            }
        })
}