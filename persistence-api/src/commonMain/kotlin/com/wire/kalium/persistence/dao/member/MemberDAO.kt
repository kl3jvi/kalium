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
package com.wire.kalium.persistence.dao.member

import com.wire.kalium.persistence.dao.ConversationIDEntity
import com.wire.kalium.persistence.dao.QualifiedIDEntity
import com.wire.kalium.persistence.dao.UserIDEntity
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface MemberDAO {
    suspend fun insertMember(member: MemberEntity, conversationID: QualifiedIDEntity)
    suspend fun updateMemberRole(userId: UserIDEntity, conversationID: QualifiedIDEntity, newRole: MemberEntity.Role)
    suspend fun insertMembersWithQualifiedId(memberList: List<MemberEntity>, conversationID: QualifiedIDEntity)
    suspend fun insertMembers(memberList: List<MemberEntity>, groupId: String)
    suspend fun deleteMemberByQualifiedID(userID: QualifiedIDEntity, conversationID: QualifiedIDEntity)

    /**
     * Deletes a list of user ids from the Conversation.
     * return a list of the users that where actually deleted
     * if the list is empty then no user was deleted
     */
    suspend fun deleteMembersByQualifiedID(userIDList: List<QualifiedIDEntity>, conversationID: QualifiedIDEntity): Long
    suspend fun observeConversationMembers(qualifiedID: QualifiedIDEntity): Flow<List<MemberEntity>>
    suspend fun updateConversationMemberRole(conversationId: QualifiedIDEntity, userId: UserIDEntity, role: MemberEntity.Role)
    suspend fun updateOrInsertOneOnOneMember(
        member: MemberEntity,
        conversationID: QualifiedIDEntity
    )

    suspend fun observeIsUserMember(conversationId: QualifiedIDEntity, userId: UserIDEntity): Flow<Boolean>
    suspend fun updateFullMemberList(memberList: List<MemberEntity>, conversationID: QualifiedIDEntity)

    suspend fun getGroupConversationWithUserIdsWithBothDomains(
        firstDomain: String,
        secondDomain: String
    ): Map<ConversationIDEntity, List<UserIDEntity>>

    suspend fun getOneOneConversationWithFederatedMembers(domain: String): Map<ConversationIDEntity, UserIDEntity>
}
