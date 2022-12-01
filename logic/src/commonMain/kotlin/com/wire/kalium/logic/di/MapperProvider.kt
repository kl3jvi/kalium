package com.wire.kalium.logic.di

import com.wire.kalium.logic.configuration.server.ApiVersionMapper
import com.wire.kalium.logic.configuration.server.ApiVersionMapperImpl
import com.wire.kalium.logic.configuration.server.ServerConfigMapper
import com.wire.kalium.logic.configuration.server.ServerConfigMapperImpl
import com.wire.kalium.logic.data.asset.AssetMapper
import com.wire.kalium.logic.data.asset.AssetMapperImpl
import com.wire.kalium.logic.data.call.mapper.ActiveSpeakerMapper
import com.wire.kalium.logic.data.call.mapper.ActiveSpeakerMapperImpl
import com.wire.kalium.logic.data.call.mapper.CallMapper
import com.wire.kalium.logic.data.call.mapper.CallMapperImpl
import com.wire.kalium.logic.data.client.ClientMapper
import com.wire.kalium.logic.data.connection.ConnectionMapper
import com.wire.kalium.logic.data.connection.ConnectionMapperImpl
import com.wire.kalium.logic.data.connection.ConnectionStatusMapper
import com.wire.kalium.logic.data.connection.ConnectionStatusMapperImpl
import com.wire.kalium.logic.data.conversation.ConversationMapper
import com.wire.kalium.logic.data.conversation.ConversationMapperImpl
import com.wire.kalium.logic.data.conversation.ConversationRoleMapper
import com.wire.kalium.logic.data.conversation.ConversationRoleMapperImpl
import com.wire.kalium.logic.data.conversation.ConversationStatusMapper
import com.wire.kalium.logic.data.conversation.ConversationStatusMapperImpl
import com.wire.kalium.logic.data.conversation.MLSCommitBundleMapper
import com.wire.kalium.logic.data.conversation.MLSCommitBundleMapperImpl
import com.wire.kalium.logic.data.conversation.MemberMapper
import com.wire.kalium.logic.data.conversation.MemberMapperImpl
import com.wire.kalium.logic.data.conversation.ProtocolInfoMapper
import com.wire.kalium.logic.data.conversation.ProtocolInfoMapperImpl
import com.wire.kalium.logic.data.event.EventMapper
import com.wire.kalium.logic.data.featureConfig.FeatureConfigMapper
import com.wire.kalium.logic.data.featureConfig.FeatureConfigMapperImpl
import com.wire.kalium.logic.data.id.FederatedIdMapper
import com.wire.kalium.logic.data.id.FederatedIdMapperImpl
import com.wire.kalium.logic.data.id.IdMapper
import com.wire.kalium.logic.data.id.IdMapperImpl
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.location.LocationMapper
import com.wire.kalium.logic.data.message.EncryptionAlgorithmMapper
import com.wire.kalium.logic.data.message.MessageMapper
import com.wire.kalium.logic.data.message.MessageMapperImpl
import com.wire.kalium.logic.data.message.ProtoContentMapper
import com.wire.kalium.logic.data.message.ProtoContentMapperImpl
import com.wire.kalium.logic.data.message.SendMessageFailureMapper
import com.wire.kalium.logic.data.message.SendMessageFailureMapperImpl
import com.wire.kalium.logic.data.message.mention.MessageMentionMapper
import com.wire.kalium.logic.data.message.mention.MessageMentionMapperImpl
import com.wire.kalium.logic.data.message.reaction.ReactionsMapper
import com.wire.kalium.logic.data.message.reaction.ReactionsMapperImpl
import com.wire.kalium.logic.data.message.receipt.ReceiptsMapper
import com.wire.kalium.logic.data.message.receipt.ReceiptsMapperImpl
import com.wire.kalium.logic.data.mlspublickeys.MLSPublicKeysMapper
import com.wire.kalium.logic.data.mlspublickeys.MLSPublicKeysMapperImpl
import com.wire.kalium.logic.data.notification.LocalNotificationMessageMapper
import com.wire.kalium.logic.data.notification.LocalNotificationMessageMapperImpl
import com.wire.kalium.logic.data.prekey.PreKeyListMapper
import com.wire.kalium.logic.data.prekey.PreKeyMapper
import com.wire.kalium.logic.data.prekey.PreKeyMapperImpl
import com.wire.kalium.logic.data.publicuser.PublicUserMapper
import com.wire.kalium.logic.data.publicuser.PublicUserMapperImpl
import com.wire.kalium.logic.data.session.SessionMapper
import com.wire.kalium.logic.data.session.SessionMapperImpl
import com.wire.kalium.logic.data.session.SessionRepository
import com.wire.kalium.logic.data.team.TeamMapper
import com.wire.kalium.logic.data.team.TeamMapperImpl
import com.wire.kalium.logic.data.user.AvailabilityStatusMapper
import com.wire.kalium.logic.data.user.AvailabilityStatusMapperImpl
import com.wire.kalium.logic.data.user.ConnectionStateMapper
import com.wire.kalium.logic.data.user.ConnectionStateMapperImpl
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.UserMapper
import com.wire.kalium.logic.data.user.UserMapperImpl
import com.wire.kalium.logic.data.user.type.DomainUserTypeMapper
import com.wire.kalium.logic.data.user.type.DomainUserTypeMapperImpl
import com.wire.kalium.logic.data.user.type.UserEntityTypeMapper
import com.wire.kalium.logic.data.user.type.UserEntityTypeMapperImpl

internal object MapperProvider {
    fun apiVersionMapper(): ApiVersionMapper = ApiVersionMapperImpl()
    fun idMapper(): IdMapper = IdMapperImpl()
    fun serverConfigMapper(): ServerConfigMapper = ServerConfigMapperImpl(apiVersionMapper())
    fun sessionMapper(): SessionMapper = SessionMapperImpl(idMapper())
    fun availabilityStatusMapper(): AvailabilityStatusMapper = AvailabilityStatusMapperImpl()
    fun connectionStateMapper(): ConnectionStateMapper = ConnectionStateMapperImpl()
    fun userMapper(): UserMapper = UserMapperImpl(
        idMapper(),
        clientMapper(), availabilityStatusMapper(), connectionStateMapper(), userTypeEntityMapper()
    )

    fun userTypeMapper(): DomainUserTypeMapper = DomainUserTypeMapperImpl()
    fun reactionsMapper(): ReactionsMapper = ReactionsMapperImpl(domainUserTypeMapper = userTypeMapper())
    fun receiptsMapper(): ReceiptsMapper = ReceiptsMapperImpl(domainUserTypeMapper = userTypeMapper())
    fun teamMapper(): TeamMapper = TeamMapperImpl()
    fun messageMapper(selfUserId: UserId): MessageMapper = MessageMapperImpl(
        idMapper = idMapper(),
        memberMapper = memberMapper(),
        selfUserId = selfUserId
    )

    fun memberMapper(): MemberMapper = MemberMapperImpl(idMapper(), conversationRoleMapper())
    fun conversationMapper(): ConversationMapper =
        ConversationMapperImpl(
            idMapper(),
            ConversationStatusMapperImpl(idMapper()),
            ProtocolInfoMapperImpl(),
            AvailabilityStatusMapperImpl(),
            DomainUserTypeMapperImpl(),
            ConnectionStatusMapperImpl()
        )

    fun conversationRoleMapper(): ConversationRoleMapper = ConversationRoleMapperImpl()
    fun publicUserMapper(): PublicUserMapper = PublicUserMapperImpl(idMapper())
    fun sendMessageFailureMapper(): SendMessageFailureMapper = SendMessageFailureMapperImpl()
    fun assetMapper(): AssetMapper = AssetMapperImpl()
    fun encryptionAlgorithmMapper(): EncryptionAlgorithmMapper = EncryptionAlgorithmMapper()
    fun eventMapper(): EventMapper = EventMapper(
        idMapper(),
        memberMapper(),
        connectionMapper(),
        featureConfigMapper(),
        conversationRoleMapper(),
        userTypeMapper(),
    )

    fun messageMentionMapper(selfUserId: UserId): MessageMentionMapper = MessageMentionMapperImpl(idMapper(), selfUserId)

    fun preyKeyMapper(): PreKeyMapper = PreKeyMapperImpl()
    fun preKeyListMapper(): PreKeyListMapper = PreKeyListMapper(preyKeyMapper())
    fun locationMapper(): LocationMapper = LocationMapper()
    fun clientMapper(): ClientMapper = ClientMapper(idMapper(), preyKeyMapper(), locationMapper())
    fun conversationStatusMapper(): ConversationStatusMapper = ConversationStatusMapperImpl(idMapper())
    fun protoContentMapper(selfUserId: UserId): ProtoContentMapper = ProtoContentMapperImpl(selfUserId = selfUserId)
    fun qualifiedIdMapper(selfUserId: UserId): QualifiedIdMapper = QualifiedIdMapperImpl(selfUserId)
    fun callMapper(selfUserId: UserId): CallMapper = CallMapperImpl(qualifiedIdMapper(selfUserId))
    fun activeSpeakerMapper(): ActiveSpeakerMapper = ActiveSpeakerMapperImpl()
    fun connectionStatusMapper(): ConnectionStatusMapper = ConnectionStatusMapperImpl()
    fun featureConfigMapper(): FeatureConfigMapper = FeatureConfigMapperImpl()
    fun localNotificationMessageMapper(): LocalNotificationMessageMapper = LocalNotificationMessageMapperImpl()
    fun connectionMapper(): ConnectionMapper = ConnectionMapperImpl()
    fun userTypeEntityMapper(): UserEntityTypeMapper = UserEntityTypeMapperImpl()
    fun federatedIdMapper(
        userId: UserId,
        qualifiedIdMapper: QualifiedIdMapper,
        sessionRepository: SessionRepository
    ): FederatedIdMapper = FederatedIdMapperImpl(userId, qualifiedIdMapper, sessionRepository)

    fun mlsPublicKeyMapper(): MLSPublicKeysMapper = MLSPublicKeysMapperImpl()

    fun mlsCommitBundleMapper(): MLSCommitBundleMapper = MLSCommitBundleMapperImpl()

    fun protocolInfoMapper(): ProtocolInfoMapper = ProtocolInfoMapperImpl()

}