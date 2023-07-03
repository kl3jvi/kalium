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
package com.wire.kalium.logic.data.client

import com.wire.kalium.cryptography.*
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.util.SecurityHelperImpl
import com.wire.kalium.persistence.dbPassphrase.PassphraseStorage
import com.wire.kalium.util.FileUtil
import com.wire.kalium.util.KaliumDispatcher
import com.wire.kalium.util.KaliumDispatcherImpl
import kotlinx.coroutines.withContext
import com.wire.kalium.logic.data.user.UserId

interface CoreCryptoCentralProvider {
    suspend fun getCoreCrypto(): Either<CoreFailure, CoreCryptoCentral>
}

class CoreCryptoCentralProviderImpl(
    private val rootKeyStorePath: String,
    private val userId: UserId,
    private val passphraseStorage: PassphraseStorage,
    private val dispatchers: KaliumDispatcher = KaliumDispatcherImpl
) : CoreCryptoCentralProvider {

    private var coreCryptoCentral: CoreCryptoCentral? = null

    override suspend fun getCoreCrypto(): Either<CoreFailure, CoreCryptoCentral> = withContext(dispatchers.io) {
        val location = "$rootKeyStorePath/${userId.value}".also {
            // TODO: migrate to okio solution once assert refactor is merged
            FileUtil.mkDirs(it)
        }

        return@withContext coreCryptoCentral?.let {
            Either.Right(it)
        } ?: run {
            val newCoreCryptoCentral = coreCryptoCentral(
                location,
                SecurityHelperImpl(passphraseStorage).mlsDBSecret(userId)
            )
            coreCryptoCentral = newCoreCryptoCentral
            Either.Right(newCoreCryptoCentral)
        }
    }

    private fun coreCryptoCentral(
        location: String,
        passphrase: MlsDBSecret
    ): CoreCryptoCentral {
        return CoreCryptoCentralImpl(
            "$location/$KEYSTORE_NAME",
            passphrase.toString()
        )
    }

    private companion object {
        const val KEYSTORE_NAME = "keystore"
    }
}
