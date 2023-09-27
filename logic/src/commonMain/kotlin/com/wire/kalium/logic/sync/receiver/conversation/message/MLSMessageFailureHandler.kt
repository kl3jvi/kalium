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
package com.wire.kalium.logic.sync.receiver.conversation.message

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.MLSFailure

sealed class MLSMessageFailureResolution {
    object Ignore : MLSMessageFailureResolution()
    object InformUser : MLSMessageFailureResolution()
    object OutOfSync : MLSMessageFailureResolution()
}

internal object MLSMessageFailureHandler {
    fun handleFailure(failure: CoreFailure): MLSMessageFailureResolution {
        return when (failure) {
            // Received messages targeting a future epoch, we might have lost messages.
            is MLSFailure.WrongEpoch -> MLSMessageFailureResolution.OutOfSync
            // Received already sent or received message, can safely be ignored.
            is MLSFailure.DuplicateMessage -> MLSMessageFailureResolution.Ignore
            // Received self commit, any unmerged group has know when merged by CoreCrypto.
            is MLSFailure.SelfCommitIgnored -> MLSMessageFailureResolution.Ignore
            // Message arrive in an unmerged group, it has been buffered and will be consumed later.
            is MLSFailure.UnmergedPendingGroup -> MLSMessageFailureResolution.Ignore
            else -> MLSMessageFailureResolution.InformUser
        }
    }
}