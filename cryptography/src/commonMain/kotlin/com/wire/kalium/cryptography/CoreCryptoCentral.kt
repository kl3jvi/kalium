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
package com.wire.kalium.cryptography
typealias EnrollmentHandle = ByteArray

interface CoreCryptoCentral {

    fun mlsClient(clientId: String): MLSClient

    fun e2eiNewEnrollment(
        clientId: String,
        displayName: String,
        handle: String
    ): E2EIClient

    fun e2eiMlsClient(enrollment: E2EIClient, certificateChain: String): MLSClient

    @OptIn(ExperimentalUnsignedTypes::class)
    fun e2eiEnrollmentStash(enrollment: E2EIClient): EnrollmentHandle
    @OptIn(ExperimentalUnsignedTypes::class)
    fun e2eiEnrollmentStashPop(handle: EnrollmentHandle): E2EIClient
}
expect class CoreCryptoCentralImpl(rootDir: String, databaseKey: String) : CoreCryptoCentral
