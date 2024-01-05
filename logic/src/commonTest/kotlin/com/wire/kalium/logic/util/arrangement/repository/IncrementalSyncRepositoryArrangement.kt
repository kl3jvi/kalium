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
package com.wire.kalium.logic.util.arrangement.repository

import com.wire.kalium.logic.data.sync.IncrementalSyncRepository
import com.wire.kalium.logic.data.sync.IncrementalSyncStatus
import io.mockative.Mock
import io.mockative.classOf
import io.mockative.given
import io.mockative.mock
import kotlinx.coroutines.flow.Flow

internal interface IncrementalSyncRepositoryArrangement {
    @Mock
    val incrementalSyncRepository: IncrementalSyncRepository

    fun withIncrementalSyncState(statusFlow: Flow<IncrementalSyncStatus>)
}

internal open class IncrementalSyncRepositoryArrangementImpl : IncrementalSyncRepositoryArrangement {

    override val incrementalSyncRepository = mock(classOf<IncrementalSyncRepository>())

    override fun withIncrementalSyncState(statusFlow: Flow<IncrementalSyncStatus>) {
        given(incrementalSyncRepository)
            .getter(incrementalSyncRepository::incrementalSyncState)
            .whenInvoked()
            .then { statusFlow }
    }

}
