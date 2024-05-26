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

package com.wire.kalium.persistence.db

import app.cash.sqldelight.db.SqlDriver
import com.wire.kalium.persistence.backup.DatabaseExporter
import com.wire.kalium.persistence.backup.DatabaseImporter
import com.wire.kalium.persistence.dao.ConnectionDAO
import com.wire.kalium.persistence.dao.MetadataDAO
import com.wire.kalium.persistence.dao.MigrationDAO
import com.wire.kalium.persistence.dao.PrekeyDAO
import com.wire.kalium.persistence.dao.SearchDAO
import com.wire.kalium.persistence.dao.ServiceDAO
import com.wire.kalium.persistence.dao.TeamDAO
import com.wire.kalium.persistence.dao.UserDAO
import com.wire.kalium.persistence.dao.UserIDEntity
import com.wire.kalium.persistence.dao.asset.AssetDAO
import com.wire.kalium.persistence.dao.call.CallDAO
import com.wire.kalium.persistence.dao.client.ClientDAO
import com.wire.kalium.persistence.dao.conversation.ConversationDAO
import com.wire.kalium.persistence.dao.member.MemberDAO
import com.wire.kalium.persistence.dao.message.CompositeMessageDAO
import com.wire.kalium.persistence.dao.message.MessageDAO
import com.wire.kalium.persistence.dao.message.MessageMetadataDAO
import com.wire.kalium.persistence.dao.message.draft.MessageDraftDAO
import com.wire.kalium.persistence.dao.newclient.NewClientDAO
import com.wire.kalium.persistence.dao.reaction.ReactionDAO
import com.wire.kalium.persistence.dao.receipt.ReceiptDAO
import com.wire.kalium.persistence.dao.unread.UserConfigDAO
import kotlinx.coroutines.CoroutineDispatcher

abstract class UserDBSecret(val value: ByteArray)

interface UserDatabaseBuilder {

    // val database: UserDatabase

    val userDAO: UserDAO

    val messageMetaDataDAO: MessageMetadataDAO

    val userConfigDAO: UserConfigDAO

    val connectionDAO: ConnectionDAO

    val conversationDAO: ConversationDAO

    val memberDAO: MemberDAO
    val metadataDAO: MetadataDAO

    val clientDAO: ClientDAO

    val newClientDAO: NewClientDAO

    val databaseImporter: DatabaseImporter

    val databaseExporter: DatabaseExporter

    val callDAO: CallDAO

    val messageDAO: MessageDAO

    val messageDraftDAO: MessageDraftDAO

    val assetDAO: AssetDAO

    val teamDAO: TeamDAO

    val reactionDAO: ReactionDAO

    val receiptDAO: ReceiptDAO

    val prekeyDAO: PrekeyDAO

    val compositeMessageDAO: CompositeMessageDAO

    val migrationDAO: MigrationDAO

    val serviceDAO: ServiceDAO

    val searchDAO: SearchDAO

    /**
     * @return the absolute path of the DB file or null if the DB file does not exist
     */
    fun dbFileLocation(): String?

    /**
     * drops DB connection and delete the DB file
     */
    fun nuke(): Boolean

    fun userDatabaseProvider(
        platformDatabaseData: PlatformDatabaseData,
        userId: UserIDEntity,
        passphrase: UserDBSecret?,
        dispatcher: CoroutineDispatcher,
        enableWAL: Boolean = true
    ): UserDatabaseBuilder

    fun userDatabaseDriverByPath(
        platformDatabaseData: PlatformDatabaseData,
        path: String,
        passphrase: UserDBSecret?,
        enableWAL: Boolean
    ): SqlDriver
}
