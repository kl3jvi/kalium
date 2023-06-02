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

package com.wire.kalium.logic.data.message

import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.message.receipt.ReceiptType
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import kotlinx.datetime.Instant

sealed class MessageContent {

    /**
     * Messages that are not sent between client, but
     * are instead generated by this client and stored locally
     * to be displayed and in-lined with the rest of the content
     * of a conversation.
     * These messages can be created based on events received
     * from the backend (_e.g._ users added/removed from a conversation),
     * or our own logic (_e.g._ missed call).
     */
    sealed class System : MessageContent()

    /**
     * Content that can be serialized/deserialized using the
     * regular Protobuf, in order to be transmitted to/from
     * other clients.
     * @see ProtoContentMapper
     */
    sealed class FromProto : MessageContent()

    /**
     * Main content of messages created by users/bot,
     * It's expected that this content will form the
     * main "Conversation View" (_i.e._ the list of
     * messages inside a conversation).
     *
     * Examples: [Text], [Asset], [Knock], Locations (coordinates).
     */
    sealed class Regular : FromProto()

    /**
     * Content that is transferred between clients, but
     * do NOT bring a standalone content to users, that is:
     * these are helping to enrich already existing messages,
     * or provide other sorts of auxiliary features.
     *
     * Examples: [Receipt], [Reaction], [DeleteMessage],
     * [DeleteForMe], [TextEdited], [UserAvailabilityStatus],
     * [Calling], [ClientAction] etc.
     */
    sealed class Signaling : FromProto()

    // client message content types
    data class Text(
        val value: String,
        val mentions: List<MessageMention> = listOf(),
        val quotedMessageReference: QuoteReference? = null,
        val quotedMessageDetails: QuotedMessageDetails? = null
    ) : Regular()

    data class QuoteReference(
        val quotedMessageId: String,
        /**
         * The hash of the text of the quoted message
         */
        val quotedMessageSha256: ByteArray?,
        val isVerified: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as QuoteReference

            if (quotedMessageId != other.quotedMessageId) return false
            if (quotedMessageSha256 != null) {
                if (other.quotedMessageSha256 == null) return false
                if (!quotedMessageSha256.contentEquals(other.quotedMessageSha256)) return false
            } else if (other.quotedMessageSha256 != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = quotedMessageId.hashCode()
            result = 31 * result + (quotedMessageSha256?.contentHashCode() ?: 0)
            return result
        }

    }

    data class QuotedMessageDetails(
        val senderId: UserId,
        val senderName: String?,
        val isQuotingSelfUser: Boolean,
        /**
         * Indicates that the hash of the quote
         * matched the hash of the original message
         */
        val isVerified: Boolean,
        val messageId: String,
        val timeInstant: Instant,
        val editInstant: Instant?,
        val quotedContent: Content
    ) {

        sealed interface Content

        data class Text(val value: String) : Content

        data class Asset(
            val assetName: String?,
            val assetMimeType: String
        ) : Content

        object Deleted : Content

        object Invalid : Content
    }

    data class Asset(val value: AssetContent) : Regular()

    data class RestrictedAsset(
        val mimeType: String,
        val sizeInBytes: Long,
        val name: String
    ) : Regular()

    data class DeleteForMe(
        val messageId: String,
        val conversationId: ConversationId,
    ) : Signaling()

    data class Calling(val value: String, val conversationId: ConversationId? = null) : Signaling()

    data class DeleteMessage(val messageId: String) : Signaling()

    data class TextEdited(
        val editMessageId: String,
        val newContent: String,
        val newMentions: List<MessageMention> = listOf()
    ) : Signaling()

    data class Knock(val hotKnock: Boolean) : Regular()

    data class Unknown( // messages that aren't yet handled properly but stored in db in case
        val typeName: String? = null,
        val encodedData: ByteArray? = null,
        val hidden: Boolean = false
    ) : Regular()

    data class Cleared(
        val conversationId: ConversationId,
        val time: Instant
    ) : Signaling()

    // server message content types
    // TODO: rename members to userList
    sealed class MemberChange(open val members: List<UserId>) : System() {
        data class Added(override val members: List<UserId>) : MemberChange(members)
        data class Removed(override val members: List<UserId>) : MemberChange(members)
        data class FailedToAdd(override val members: List<UserId>) : MemberChange(members)
        data class CreationAdded(override val members: List<UserId>) : MemberChange(members)
    }

    data class LastRead(
        val messageId: String,
        val conversationId: ConversationId,
        val time: Instant
    ) : Signaling()

    data class ConversationRenamed(val conversationName: String) : System()

    data class TeamMemberRemoved(val userName: String) : System()

    object MissedCall : System()

    data class Reaction(
        val messageId: String,
        val emojiSet: Set<String>
    ) : Signaling()

    data class Availability(val status: UserAvailabilityStatus) : Signaling()

    data class Receipt(val type: ReceiptType, val messageIds: List<String>) : Signaling() {
        fun toLogMap(): Map<String, Any> = mapOf(
            "type" to "$type",
            "messageIds" to messageIds.map { it.obfuscateId() }
        )

    }

    data class NewConversationReceiptMode(
        val receiptMode: Boolean
    ) : System()

    data class ConversationReceiptModeChanged(
        val receiptMode: Boolean
    ) : System()

    data class ConversationMessageTimerChanged(
        val messageTimer: Long?
    ) : System()

    // we can add other types to be processed, but signaling ones shouldn't be persisted
    object Ignored : Signaling() // messages that aren't processed in any way

    data class FailedDecryption(
        val encodedData: ByteArray? = null,
        val isDecryptionResolved: Boolean,
        val senderUserId: UserId,
        val clientId: ClientId? = null
    ) : Regular()

    object ClientAction : Signaling()

    object CryptoSessionReset : System()

    object HistoryLost : System()
    object ConversationCreated : System()
}

/**
 * @return A string representing the type of content.
 * Useful for logging. Plain strings must be used, otherwise it may be affected by code minification.
 */
@Suppress("ComplexMethod")
fun MessageContent?.getType() = when (this) {
    is MessageContent.Asset -> "Asset"
    is MessageContent.FailedDecryption -> "FailedDecryption"
    is MessageContent.Knock -> "Knock"
    is MessageContent.RestrictedAsset -> "RestrictedAsset"
    is MessageContent.Text -> "Text"
    is MessageContent.Unknown -> "Unknown"
    is MessageContent.Availability -> "Availability"
    is MessageContent.Calling -> "Calling"
    is MessageContent.Cleared -> "Cleared"
    is MessageContent.ClientAction -> "ClientAction"
    is MessageContent.DeleteForMe -> "DeleteForMe"
    is MessageContent.DeleteMessage -> "DeleteMessage"
    is MessageContent.Ignored -> "Ignored"
    is MessageContent.LastRead -> "LastRead"
    is MessageContent.Reaction -> "Reaction"
    is MessageContent.Receipt -> "Receipt"
    is MessageContent.TextEdited -> "TextEdited"
    is MessageContent.ConversationMessageTimerChanged -> "ConversationMessageTimerChanged"
    is MessageContent.ConversationReceiptModeChanged -> "ConversationReceiptModeChanged"
    is MessageContent.ConversationRenamed -> "ConversationRenamed"
    is MessageContent.CryptoSessionReset -> "CryptoSessionReset"
    is MessageContent.HistoryLost -> "HistoryLost"
    is MessageContent.MemberChange.Added -> "MemberChange.Added"
    is MessageContent.MemberChange.Removed -> "MemberChange.Removed"
    is MessageContent.MissedCall -> "MissedCall"
    is MessageContent.NewConversationReceiptMode -> "NewConversationReceiptMode"
    is MessageContent.TeamMemberRemoved -> "TeamMemberRemoved"
    is MessageContent.ConversationCreated -> "ConversationCreated"
    is MessageContent.MemberChange.CreationAdded -> "MemberChange.CreationAdded"
    is MessageContent.MemberChange.FailedToAdd -> "MemberChange.FailedToAdd"
    null -> "Unknown"
}

sealed class MessagePreviewContent {

    sealed class WithUser(open val username: String?) : MessagePreviewContent() {

        data class Text(override val username: String?, val messageBody: String) : WithUser(username)

        data class Asset(override val username: String?, val type: AssetType) : WithUser(username)

        data class MentionedSelf(override val username: String?) : WithUser(username)

        data class QuotedSelf(override val username: String?) : WithUser(username)

        data class Knock(override val username: String?) : WithUser(username)

        data class MemberLeft(override val username: String?) : WithUser(username)

        data class MemberJoined(override val username: String?) : WithUser(username)

        data class MembersAdded(
            val senderName: String?,
            val isSelfUserAdded: Boolean,
            val otherUserIdList: List<UserId> // TODO add usernames
        ) : WithUser(senderName)

        data class MembersRemoved(
            val senderName: String?,
            val isSelfUserRemoved: Boolean,
            val otherUserIdList: List<UserId> // TODO add usernames
        ) : WithUser(senderName)

        data class MembersFailedToAdd(
            val senderName: String?,
            val isSelfUserRemoved: Boolean,
            val otherUserIdList: List<UserId> // TODO add usernames
        ) : WithUser(senderName)

        data class MembersCreationAdded(
            val senderName: String?,
            val isSelfUserRemoved: Boolean,
            val otherUserIdList: List<UserId> // TODO add usernames
        ) : WithUser(senderName)

        data class ConversationNameChange(val adminName: String?) : WithUser(adminName)

        data class TeamMemberRemoved(val userName: String?) : WithUser(userName)

        data class MissedCall(override val username: String?) : WithUser(username)

    }

    data class Ephemeral(val isGroupConversation: Boolean) : MessagePreviewContent()

    object CryptoSessionReset : MessagePreviewContent()

    object Unknown : MessagePreviewContent()

}
