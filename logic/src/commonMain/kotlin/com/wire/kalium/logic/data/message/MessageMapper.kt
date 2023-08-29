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

import com.wire.kalium.logic.data.asset.AssetMapper
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.toDao
import com.wire.kalium.logic.data.id.toModel
import com.wire.kalium.logic.data.message.AssetContent.AssetMetadata.Audio
import com.wire.kalium.logic.data.message.AssetContent.AssetMetadata.Image
import com.wire.kalium.logic.data.message.AssetContent.AssetMetadata.Video
import com.wire.kalium.logic.data.message.mention.MessageMentionMapper
import com.wire.kalium.logic.data.message.mention.toModel
import com.wire.kalium.logic.data.notification.LocalNotificationCommentType
import com.wire.kalium.logic.data.notification.LocalNotificationMessage
import com.wire.kalium.logic.data.notification.LocalNotificationMessageAuthor
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.di.MapperProvider
import com.wire.kalium.persistence.dao.message.AssetTypeEntity
import com.wire.kalium.persistence.dao.message.ButtonEntity
import com.wire.kalium.persistence.dao.message.DeliveryStatusEntity
import com.wire.kalium.persistence.dao.message.MessageEntity
import com.wire.kalium.persistence.dao.message.MessageEntityContent
import com.wire.kalium.persistence.dao.message.MessagePreviewEntity
import com.wire.kalium.persistence.dao.message.MessagePreviewEntityContent
import com.wire.kalium.persistence.dao.message.NotificationMessageEntity
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface MessageMapper {
    fun fromMessageToEntity(message: Message.Standalone): MessageEntity
    fun fromEntityToMessage(message: MessageEntity): Message.Standalone
    fun fromEntityToMessagePreview(message: MessagePreviewEntity): MessagePreview
    fun fromMessageToLocalNotificationMessage(message: NotificationMessageEntity): LocalNotificationMessage?
    fun toMessageEntityContent(regularMessage: MessageContent.Regular): MessageEntityContent.Regular
}

@Suppress("TooManyFunctions")
class MessageMapperImpl(
    private val selfUserId: UserId,
    private val messageMentionMapper: MessageMentionMapper = MapperProvider.messageMentionMapper(selfUserId),
    private val assetMapper: AssetMapper = MapperProvider.assetMapper()
) : MessageMapper {

    override fun fromMessageToEntity(message: Message.Standalone): MessageEntity =
        when (message) {
            is Message.Regular -> mapFromRegularMessage(message)
            is Message.System -> mapFromSystemMessage(message)
        }

    private fun mapFromRegularMessage(
        message: Message.Regular
    ) = MessageEntity.Regular(
        id = message.id,
        content = toMessageEntityContent(message.content),
        conversationId = message.conversationId.toDao(),
        date = message.date.toInstant(),
        senderUserId = message.senderUserId.toDao(),
        senderClientId = message.senderClientId.value,
        status = message.status.toEntityStatus(),
        readCount = if (message.status is Message.Status.Read) message.status.readCount else 0,
        editStatus = when (message.editStatus) {
            is Message.EditStatus.NotEdited -> MessageEntity.EditStatus.NotEdited
            is Message.EditStatus.Edited -> MessageEntity.EditStatus.Edited(message.editStatus.lastTimeStamp.toInstant())
        },
        expireAfterMs = message.expirationData?.expireAfter?.inWholeMilliseconds,
        selfDeletionStartDate = message.expirationData?.let {
            when (it.selfDeletionStatus) {
                is Message.ExpirationData.SelfDeletionStatus.Started -> it.selfDeletionStatus.selfDeletionStartDate
                is Message.ExpirationData.SelfDeletionStatus.NotStarted -> null
            }
        },
        visibility = message.visibility.toEntityVisibility(),
        senderName = message.senderUserName,
        isSelfMessage = message.isSelfMessage,
        expectsReadConfirmation = message.expectsReadConfirmation
    )

    private fun mapFromSystemMessage(
        message: Message.System
    ) = MessageEntity.System(
        id = message.id,
        content = message.content.toMessageEntityContent(),
        conversationId = message.conversationId.toDao(),
        date = message.date.toInstant(),
        senderUserId = message.senderUserId.toDao(),
        status = message.status.toEntityStatus(),
        visibility = message.visibility.toEntityVisibility(),
        senderName = message.senderUserName,
        expireAfterMs = message.expirationData?.expireAfter?.inWholeMilliseconds,
        readCount = if (message.status is Message.Status.Read) message.status.readCount else 0,
        selfDeletionStartDate = message.expirationData?.let {
            when (it.selfDeletionStatus) {
                is Message.ExpirationData.SelfDeletionStatus.Started -> it.selfDeletionStatus.selfDeletionStartDate
                is Message.ExpirationData.SelfDeletionStatus.NotStarted -> null
            }
        }
    )

    override fun fromEntityToMessage(message: MessageEntity): Message.Standalone {
        return when (message) {
            is MessageEntity.Regular -> mapRegularMessage(message)
            is MessageEntity.System -> mapSystemMessage(message)
        }
    }

    private fun mapRegularMessage(message: MessageEntity.Regular) = Message.Regular(
        id = message.id,
        content = message.content.toMessageContent(message.visibility.toModel() == Message.Visibility.HIDDEN, selfUserId),
        conversationId = message.conversationId.toModel(),
        date = message.date.toIsoDateTimeString(),
        senderUserId = message.senderUserId.toModel(),
        senderClientId = ClientId(message.senderClientId),
        status = message.status.toModel(message.readCount),
        editStatus = when (val editStatus = message.editStatus) {
            MessageEntity.EditStatus.NotEdited -> Message.EditStatus.NotEdited
            is MessageEntity.EditStatus.Edited -> Message.EditStatus.Edited(editStatus.lastDate.toIsoDateTimeString())
        },
        expirationData = message.expireAfterMs?.let {
            Message.ExpirationData(
                expireAfter = it.toDuration(DurationUnit.MILLISECONDS),
                selfDeletionStatus = message.selfDeletionStartDate
                    ?.let { Message.ExpirationData.SelfDeletionStatus.Started(it) }
                    ?: Message.ExpirationData.SelfDeletionStatus.NotStarted)
        },
        visibility = message.visibility.toModel(),
        reactions = Message.Reactions(message.reactions.totalReactions, message.reactions.selfUserReactions),
        senderUserName = message.senderName,
        isSelfMessage = message.isSelfMessage,
        expectsReadConfirmation = message.expectsReadConfirmation,
        deliveryStatus = when (val recipientsFailure = message.deliveryStatus) {
            is DeliveryStatusEntity.CompleteDelivery -> DeliveryStatus.CompleteDelivery
            is DeliveryStatusEntity.PartialDelivery -> DeliveryStatus.PartialDelivery(
                recipientsFailedWithNoClients = recipientsFailure.recipientsFailedWithNoClients.map { it.toModel() },
                recipientsFailedDelivery = recipientsFailure.recipientsFailedDelivery.map { it.toModel() }
            )
        }
    )

    private fun mapSystemMessage(message: MessageEntity.System) = Message.System(
        id = message.id,
        content = message.content.toMessageContent(),
        conversationId = message.conversationId.toModel(),
        date = message.date.toIsoDateTimeString(),
        senderUserId = message.senderUserId.toModel(),
        status = message.status.toModel(message.readCount),
        visibility = message.visibility.toModel(),
        senderUserName = message.senderName,
        expirationData = message.expireAfterMs?.let {
            Message.ExpirationData(
                expireAfter = it.toDuration(DurationUnit.MILLISECONDS),
                selfDeletionStatus = message.selfDeletionStartDate
                    ?.let { Message.ExpirationData.SelfDeletionStatus.Started(it) }
                    ?: Message.ExpirationData.SelfDeletionStatus.NotStarted)
        }
    )

    override fun fromEntityToMessagePreview(message: MessagePreviewEntity): MessagePreview {
        return MessagePreview(
            id = message.id,
            conversationId = message.conversationId.toModel(),
            content = message.content.toMessageContent(),
            date = message.date,
            visibility = message.visibility.toModel(),
            isSelfMessage = message.isSelfMessage,
            senderUserId = message.senderUserId.toModel()
        )
    }

    @Suppress("ComplexMethod")
    override fun fromMessageToLocalNotificationMessage(
        message: NotificationMessageEntity
    ): LocalNotificationMessage? {
        val sender = LocalNotificationMessageAuthor(
            message.senderName.orEmpty(),
            message.senderImage?.toModel()
        )
        if (message.isSelfDelete) {
            return LocalNotificationMessage.SelfDeleteMessage(
                message.id,
                message.date
            )
        }
        return when (message.contentType) {
            MessageEntity.ContentType.TEXT -> LocalNotificationMessage.Text(
                messageId = message.id,
                author = sender,
                text = message.text.orEmpty(),
                time = message.date,
                isQuotingSelfUser = message.isQuotingSelf
            )

            MessageEntity.ContentType.ASSET -> {
                val type = message.assetMimeType?.contains("image/")?.let {
                    if (it) LocalNotificationCommentType.PICTURE else LocalNotificationCommentType.FILE
                } ?: LocalNotificationCommentType.FILE

                LocalNotificationMessage.Comment(message.id, sender, message.date, type)
            }

            MessageEntity.ContentType.KNOCK -> {
                LocalNotificationMessage.Knock(
                    message.id,
                    sender,
                    message.date
                )
            }

            MessageEntity.ContentType.MISSED_CALL -> {
                LocalNotificationMessage.Comment(
                    message.id,
                    sender,
                    message.date,
                    LocalNotificationCommentType.MISSED_CALL
                )
            }

            MessageEntity.ContentType.MEMBER_CHANGE -> null
            MessageEntity.ContentType.RESTRICTED_ASSET -> null
            MessageEntity.ContentType.CONVERSATION_RENAMED -> null
            MessageEntity.ContentType.UNKNOWN -> null
            MessageEntity.ContentType.FAILED_DECRYPTION -> null
            MessageEntity.ContentType.REMOVED_FROM_TEAM -> null
            MessageEntity.ContentType.CRYPTO_SESSION_RESET -> null
            MessageEntity.ContentType.NEW_CONVERSATION_RECEIPT_MODE -> null
            MessageEntity.ContentType.CONVERSATION_RECEIPT_MODE_CHANGED -> null
            MessageEntity.ContentType.HISTORY_LOST -> null
            MessageEntity.ContentType.CONVERSATION_MESSAGE_TIMER_CHANGED -> null
            MessageEntity.ContentType.CONVERSATION_CREATED -> null
            MessageEntity.ContentType.MLS_WRONG_EPOCH_WARNING -> null
            MessageEntity.ContentType.CONVERSATION_DEGRADED_MLS -> null
            MessageEntity.ContentType.CONVERSATION_DEGRADED_PREOTEUS -> null
            MessageEntity.ContentType.COMPOSITE -> null
            MessageEntity.ContentType.FEDERATION -> null
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    override fun toMessageEntityContent(regularMessage: MessageContent.Regular): MessageEntityContent.Regular = when (regularMessage) {
        is MessageContent.Text -> toTextEntity(regularMessage)

        is MessageContent.Asset -> with(regularMessage.value) {
            val assetWidth = when (metadata) {
                is Image -> metadata.width
                is Video -> metadata.width
                else -> null
            }
            val assetHeight = when (metadata) {
                is Image -> metadata.height
                is Video -> metadata.height
                else -> null
            }
            val assetDurationMs = when (metadata) {
                is Video -> metadata.durationMs
                is Audio -> metadata.durationMs
                else -> null
            }
            MessageEntityContent.Asset(
                assetSizeInBytes = sizeInBytes,
                assetName = name,
                assetMimeType = mimeType,
                assetUploadStatus = assetMapper.fromUploadStatusToDaoModel(uploadStatus),
                assetDownloadStatus = assetMapper.fromDownloadStatusToDaoModel(downloadStatus),
                assetOtrKey = remoteData.otrKey,
                assetSha256Key = remoteData.sha256,
                assetId = remoteData.assetId,
                assetDomain = remoteData.assetDomain,
                assetToken = remoteData.assetToken,
                assetEncryptionAlgorithm = remoteData.encryptionAlgorithm?.name,
                assetWidth = assetWidth,
                assetHeight = assetHeight,
                assetDurationMs = assetDurationMs,
                assetNormalizedLoudness = if (metadata is Audio) metadata.normalizedLoudness else null,
            )
        }

        is MessageContent.RestrictedAsset -> MessageEntityContent.RestrictedAsset(
            regularMessage.mimeType,
            regularMessage.sizeInBytes,
            regularMessage.name
        )

        // We store the encoded data in case we decide to try to decrypt them again in the future
        is MessageContent.FailedDecryption -> MessageEntityContent.FailedDecryption(
            regularMessage.encodedData,
            regularMessage.isDecryptionResolved,
            regularMessage.senderUserId.toDao(),
            regularMessage.clientId?.value
        )

        // We store the unknown fields of the message in case we want to start handling them in the future
        is MessageContent.Unknown -> MessageEntityContent.Unknown(regularMessage.typeName, regularMessage.encodedData)

        // We don't care about the content of these messages as they are only used to perform other actions, i.e. update the content of a
        // previously stored message, delete the content of a previously stored message, etc... Therefore, we map their content to Unknown
        is MessageContent.Knock -> MessageEntityContent.Knock(hotKnock = regularMessage.hotKnock)
        is MessageContent.Composite -> MessageEntityContent.Composite(
            text = regularMessage.textContent?.let(this::toTextEntity),
            buttonList = regularMessage.buttonList.map {
                ButtonEntity(
                    id = it.id,
                    text = it.text,
                    isSelected = it.isSelected
                )
            },
        )
    }

    private fun toTextEntity(textContent: MessageContent.Text): MessageEntityContent.Text = MessageEntityContent.Text(
        messageBody = textContent.value,
        mentions = textContent.mentions.map(messageMentionMapper::fromModelToDao),
        quotedMessageId = textContent.quotedMessageReference?.quotedMessageId,
        isQuoteVerified = textContent.quotedMessageReference?.isVerified,
    )

    @Suppress("ComplexMethod")
    private fun MessageEntityContent.System.toMessageContent(): MessageContent.System = when (this) {
        is MessageEntityContent.MemberChange -> {
            val memberList = this.memberUserIdList.map { it.toModel() }
            when (this.memberChangeType) {
                MessageEntity.MemberChangeType.ADDED -> MessageContent.MemberChange.Added(memberList)
                MessageEntity.MemberChangeType.REMOVED -> MessageContent.MemberChange.Removed(memberList)
                MessageEntity.MemberChangeType.CREATION_ADDED -> MessageContent.MemberChange.CreationAdded(memberList)
                MessageEntity.MemberChangeType.FAILED_TO_ADD -> MessageContent.MemberChange.FailedToAdd(memberList)
                MessageEntity.MemberChangeType.FEDERATION_REMOVED -> MessageContent.MemberChange.FederationRemoved(memberList)
            }
        }

        is MessageEntityContent.MissedCall -> MessageContent.MissedCall
        is MessageEntityContent.ConversationRenamed -> MessageContent.ConversationRenamed(conversationName)
        is MessageEntityContent.TeamMemberRemoved -> MessageContent.TeamMemberRemoved(userName)
        is MessageEntityContent.CryptoSessionReset -> MessageContent.CryptoSessionReset
        is MessageEntityContent.NewConversationReceiptMode -> MessageContent.NewConversationReceiptMode(receiptMode)
        is MessageEntityContent.ConversationReceiptModeChanged -> MessageContent.ConversationReceiptModeChanged(receiptMode)
        is MessageEntityContent.HistoryLost -> MessageContent.HistoryLost
        is MessageEntityContent.ConversationMessageTimerChanged -> MessageContent.ConversationMessageTimerChanged(messageTimer)
        is MessageEntityContent.ConversationCreated -> MessageContent.ConversationCreated
        is MessageEntityContent.MLSWrongEpochWarning -> MessageContent.MLSWrongEpochWarning
        is MessageEntityContent.ConversationDegradedMLS -> MessageContent.ConversationDegradedMLS
        is MessageEntityContent.ConversationDegradedProteus -> MessageContent.ConversationDegradedProteus
        is MessageEntityContent.Federation -> when (type) {
            MessageEntity.FederationType.DELETE -> MessageContent.FederationStopped.Removed(domainList.first())
            MessageEntity.FederationType.CONNECTION_REMOVED -> MessageContent.FederationStopped.ConnectionRemoved(domainList)
        }
    }
}

fun Message.Visibility.toEntityVisibility(): MessageEntity.Visibility = when (this) {
    Message.Visibility.VISIBLE -> MessageEntity.Visibility.VISIBLE
    Message.Visibility.HIDDEN -> MessageEntity.Visibility.HIDDEN
    Message.Visibility.DELETED -> MessageEntity.Visibility.DELETED
}

fun MessageEntity.Visibility.toModel(): Message.Visibility = when (this) {
    MessageEntity.Visibility.VISIBLE -> Message.Visibility.VISIBLE
    MessageEntity.Visibility.HIDDEN -> Message.Visibility.HIDDEN
    MessageEntity.Visibility.DELETED -> Message.Visibility.DELETED
}

@Suppress("ComplexMethod")
private fun MessagePreviewEntityContent.toMessageContent(): MessagePreviewContent = when (this) {
    is MessagePreviewEntityContent.Asset -> MessagePreviewContent.WithUser.Asset(username = senderName, type = type.toModel())
    is MessagePreviewEntityContent.ConversationNameChange -> MessagePreviewContent.WithUser.ConversationNameChange(adminName)
    is MessagePreviewEntityContent.Knock -> MessagePreviewContent.WithUser.Knock(senderName)
    is MessagePreviewEntityContent.MemberJoined -> MessagePreviewContent.WithUser.MemberJoined(senderName)
    is MessagePreviewEntityContent.MemberLeft -> MessagePreviewContent.WithUser.MemberLeft(senderName)
    is MessagePreviewEntityContent.MembersAdded -> MessagePreviewContent.WithUser.MembersAdded(
        username = senderName,
        isSelfUserAdded = isContainSelfUserId,
        otherUserIdList = otherUserIdList.map { it.toModel() }
    )

    is MessagePreviewEntityContent.MembersRemoved -> MessagePreviewContent.WithUser.MembersRemoved(
        username = senderName,
        isSelfUserRemoved = isContainSelfUserId,
        otherUserIdList = otherUserIdList.map { it.toModel() }
    )

    is MessagePreviewEntityContent.FederatedMembersRemoved -> MessagePreviewContent.FederatedMembersRemoved(
        isSelfUserRemoved = isContainSelfUserId,
        otherUserIdList = otherUserIdList.map { it.toModel() }
    )

    is MessagePreviewEntityContent.MembersCreationAdded -> MessagePreviewContent.WithUser.MembersCreationAdded(
        username = senderName,
        isSelfUserRemoved = isContainSelfUserId,
        otherUserIdList = otherUserIdList.map { it.toModel() }
    )

    is MessagePreviewEntityContent.MembersFailedToAdded -> MessagePreviewContent.WithUser.MembersFailedToAdd(
        username = senderName,
        isSelfUserRemoved = isContainSelfUserId,
        otherUserIdList = otherUserIdList.map { it.toModel() }
    )

    is MessagePreviewEntityContent.Ephemeral -> MessagePreviewContent.Ephemeral(isGroupConversation)
    is MessagePreviewEntityContent.MentionedSelf -> MessagePreviewContent.WithUser.MentionedSelf(senderName)
    is MessagePreviewEntityContent.MissedCall -> MessagePreviewContent.WithUser.MissedCall(senderName)
    is MessagePreviewEntityContent.QuotedSelf -> MessagePreviewContent.WithUser.QuotedSelf(senderName)
    is MessagePreviewEntityContent.TeamMemberRemoved -> MessagePreviewContent.WithUser.TeamMemberRemoved(userName)
    is MessagePreviewEntityContent.Text -> MessagePreviewContent.WithUser.Text(username = senderName, messageBody = messageBody)
    is MessagePreviewEntityContent.CryptoSessionReset -> MessagePreviewContent.CryptoSessionReset
    MessagePreviewEntityContent.Unknown -> MessagePreviewContent.Unknown
    is MessagePreviewEntityContent.Composite -> MessagePreviewContent.WithUser.Composite(username = senderName, messageBody = messageBody)
}

fun AssetTypeEntity.toModel(): AssetType = when (this) {
    AssetTypeEntity.IMAGE -> AssetType.IMAGE
    AssetTypeEntity.VIDEO -> AssetType.VIDEO
    AssetTypeEntity.AUDIO -> AssetType.AUDIO
    AssetTypeEntity.GENERIC_ASSET -> AssetType.GENERIC_ASSET
}

fun Message.Status.toEntityStatus() =
    when (this) {
        Message.Status.Delivered -> MessageEntity.Status.DELIVERED
        Message.Status.Pending -> MessageEntity.Status.PENDING
        is Message.Status.Read -> MessageEntity.Status.READ
        Message.Status.Sent -> MessageEntity.Status.SENT
        Message.Status.Failed -> MessageEntity.Status.FAILED
        Message.Status.FailedRemotely -> MessageEntity.Status.FAILED_REMOTELY
    }

fun MessageEntity.Status.toModel(readCount: Long) =
    when (this) {
        MessageEntity.Status.PENDING -> Message.Status.Pending
        MessageEntity.Status.SENT -> Message.Status.Sent
        MessageEntity.Status.DELIVERED -> Message.Status.Delivered
        MessageEntity.Status.READ -> Message.Status.Read(readCount)
        MessageEntity.Status.FAILED -> Message.Status.Failed
        MessageEntity.Status.FAILED_REMOTELY -> Message.Status.FailedRemotely
    }

fun MessageEntityContent.Regular.toMessageContent(hidden: Boolean, selfUserId: UserId): MessageContent.Regular = when (this) {
    is MessageEntityContent.Text -> {
        val quotedMessageDetails = this.quotedMessage?.let {
            MessageContent.QuotedMessageDetails(
                senderId = it.senderId.toModel(),
                senderName = it.senderName,
                isQuotingSelfUser = it.isQuotingSelfUser,
                isVerified = it.isVerified,
                messageId = it.id,
                timeInstant = Instant.parse(it.dateTime),
                editInstant = it.editTimestamp?.let { editTime -> Instant.parse(editTime) },
                quotedContent = quotedContentFromEntity(it)
            )
        }
        MessageContent.Text(
            value = this.messageBody,
            mentions = this.mentions.map { it.toModel(selfUserId = selfUserId) },
            quotedMessageReference = quotedMessageId?.let {
                MessageContent.QuoteReference(
                    quotedMessageId = it,
                    quotedMessageSha256 = null,
                    isVerified = quotedMessageDetails?.isVerified ?: false
                )
            },
            quotedMessageDetails = quotedMessageDetails
        )
    }

    is MessageEntityContent.Asset -> MessageContent.Asset(
        value = MapperProvider.assetMapper().fromAssetEntityToAssetContent(this)
    )

    is MessageEntityContent.Knock -> MessageContent.Knock(this.hotKnock)

    is MessageEntityContent.RestrictedAsset -> MessageContent.RestrictedAsset(
        this.mimeType, this.assetSizeInBytes, this.assetName
    )

    is MessageEntityContent.Unknown -> MessageContent.Unknown(this.typeName, this.encodedData, hidden)
    is MessageEntityContent.FailedDecryption -> MessageContent.FailedDecryption(
        this.encodedData,
        this.isDecryptionResolved,
        this.senderUserId.toModel(),
        ClientId(this.senderClientId.orEmpty())
    )

    is MessageEntityContent.Composite -> MessageContent.Composite(
        this.text?.toMessageContent(hidden, selfUserId) as? MessageContent.Text,
        this.buttonList.map {
            MessageContent.Composite.Button(
                text = it.text,
                id = it.id,
                isSelected = it.isSelected
            )
        }
    )
}

private fun quotedContentFromEntity(it: MessageEntityContent.Text.QuotedMessage) = when {
    // Prioritise Invalid and Deleted over content types
    !it.isVerified -> MessageContent.QuotedMessageDetails.Invalid
    !it.visibility.isVisible -> MessageContent.QuotedMessageDetails.Deleted
    it.contentType == MessageEntity.ContentType.TEXT -> MessageContent.QuotedMessageDetails.Text(it.textBody!!)
    it.contentType == MessageEntity.ContentType.ASSET -> {
        MessageContent.QuotedMessageDetails.Asset(
            assetName = it.assetName,
            assetMimeType = requireNotNull(it.assetMimeType)
        )
    }

    // If a new content type can be replied to (Pings, for example), fallback to Invalid
    else -> MessageContent.QuotedMessageDetails.Invalid
}

@Suppress("ComplexMethod")
fun MessageContent.System.toMessageEntityContent(): MessageEntityContent.System = when (this) {
    is MessageContent.MemberChange -> {
        val memberUserIdList = this.members.map { it.toDao() }
        when (this) {
            is MessageContent.MemberChange.Added ->
                MessageEntityContent.MemberChange(memberUserIdList, MessageEntity.MemberChangeType.ADDED)

            is MessageContent.MemberChange.Removed ->
                MessageEntityContent.MemberChange(memberUserIdList, MessageEntity.MemberChangeType.REMOVED)

            is MessageContent.MemberChange.CreationAdded ->
                MessageEntityContent.MemberChange(memberUserIdList, MessageEntity.MemberChangeType.CREATION_ADDED)

            is MessageContent.MemberChange.FailedToAdd ->
                MessageEntityContent.MemberChange(memberUserIdList, MessageEntity.MemberChangeType.FAILED_TO_ADD)

            is MessageContent.MemberChange.FederationRemoved -> MessageEntityContent.MemberChange(
                memberUserIdList,
                MessageEntity.MemberChangeType.FEDERATION_REMOVED
            )

        }
    }

    is MessageContent.CryptoSessionReset -> MessageEntityContent.CryptoSessionReset
    is MessageContent.MissedCall -> MessageEntityContent.MissedCall
    is MessageContent.ConversationRenamed -> MessageEntityContent.ConversationRenamed(conversationName)
    is MessageContent.TeamMemberRemoved -> MessageEntityContent.TeamMemberRemoved(userName)
    is MessageContent.NewConversationReceiptMode -> MessageEntityContent.NewConversationReceiptMode(receiptMode)
    is MessageContent.ConversationReceiptModeChanged -> MessageEntityContent.ConversationReceiptModeChanged(receiptMode)
    is MessageContent.HistoryLost -> MessageEntityContent.HistoryLost
    is MessageContent.ConversationMessageTimerChanged -> MessageEntityContent.ConversationMessageTimerChanged(messageTimer)
    is MessageContent.ConversationCreated -> MessageEntityContent.ConversationCreated
    is MessageContent.MLSWrongEpochWarning -> MessageEntityContent.MLSWrongEpochWarning
    is MessageContent.ConversationDegradedMLS -> MessageEntityContent.ConversationDegradedMLS
    is MessageContent.ConversationDegradedProteus -> MessageEntityContent.ConversationDegradedProteus
    is MessageContent.FederationStopped.ConnectionRemoved -> MessageEntityContent.Federation(
        domainList,
        MessageEntity.FederationType.CONNECTION_REMOVED
    )

    is MessageContent.FederationStopped.Removed -> MessageEntityContent.Federation(
        listOf(domain),
        MessageEntity.FederationType.DELETE
    )
}
