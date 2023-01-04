package com.wire.kalium.network.api.v3.authenticated

import com.wire.kalium.util.DateTimeUtil
import com.wire.kalium.network.AuthenticatedNetworkClient
import com.wire.kalium.network.api.base.authenticated.conversation.ConvProtocol
import com.wire.kalium.network.api.base.authenticated.conversation.ConversationMemberDTO
import com.wire.kalium.network.api.base.authenticated.conversation.ConversationMembersResponse
import com.wire.kalium.network.api.base.authenticated.conversation.ConversationResponse
import com.wire.kalium.network.api.base.authenticated.conversation.ConversationResponseV3
import com.wire.kalium.network.api.base.authenticated.conversation.CreateConversationRequest
import com.wire.kalium.network.api.base.authenticated.conversation.GlobalTeamConversationResponse
import com.wire.kalium.network.api.base.authenticated.conversation.UpdateConversationAccessRequest
import com.wire.kalium.network.api.base.authenticated.conversation.UpdateConversationAccessResponse
import com.wire.kalium.network.api.base.authenticated.notification.EventContentDTO
import com.wire.kalium.network.api.base.model.ApiModelMapper
import com.wire.kalium.network.api.base.model.ApiModelMapperImpl
import com.wire.kalium.network.api.base.model.ConversationId
import com.wire.kalium.network.api.base.model.QualifiedID
import com.wire.kalium.network.api.base.model.TeamId
import com.wire.kalium.network.api.base.model.UserId
import com.wire.kalium.network.api.v2.authenticated.ConversationApiV2
import com.wire.kalium.network.exceptions.KaliumException
import com.wire.kalium.network.utils.NetworkResponse
import com.wire.kalium.network.utils.mapSuccess
import com.wire.kalium.network.utils.wrapKaliumResponse
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import okio.IOException

internal open class ConversationApiV3 internal constructor(
    authenticatedNetworkClient: AuthenticatedNetworkClient,
    private val apiModelMapper: ApiModelMapper = ApiModelMapperImpl()
) : ConversationApiV2(authenticatedNetworkClient) {

    /**
     * returns 201 when a new conversation is created or 200 if the conversation already existed
     */
    override suspend fun createNewConversation(
        createConversationRequest: CreateConversationRequest
    ): NetworkResponse<ConversationResponse> = wrapKaliumResponse<ConversationResponseV3> {
        httpClient.post(PATH_CONVERSATIONS) {
            setBody(apiModelMapper.toApiV3(createConversationRequest))
        }
    }.mapSuccess {
        apiModelMapper.fromApiV3(it)
    }

    override suspend fun createOne2OneConversation(
        createConversationRequest: CreateConversationRequest
    ): NetworkResponse<ConversationResponse> = wrapKaliumResponse<ConversationResponseV3> {
        httpClient.post("$PATH_CONVERSATIONS/$PATH_ONE_2_ONE") {
            setBody(apiModelMapper.toApiV3(createConversationRequest))
        }
    }.mapSuccess {
        apiModelMapper.fromApiV3(it)
    }

    override suspend fun fetchGroupInfo(conversationId: QualifiedID): NetworkResponse<ByteArray> =
        wrapKaliumResponse {
            httpClient.get(
                "$PATH_CONVERSATIONS/${conversationId.domain}/${conversationId.value}/$PATH_GROUP_INFO"
            )
        }

    override suspend fun fetchGlobalTeamConversationDetails(selfUserId: UserId, teamId: TeamId): NetworkResponse<ConversationResponse> {
        return wrapKaliumResponse<GlobalTeamConversationResponse> {
            httpClient.get("$PATH_TEAM/$teamId/$PATH_CONVERSATIONS/$PATH_GLOBAL")
        }.mapSuccess { response ->
            ConversationResponse(
                response.creator ?: "",
                ConversationMembersResponse(
                    ConversationMemberDTO.Self(
                        selfUserId,
                        "wire_default",
                    ),
                    emptyList()
                ),
                response.name,
                response.id,
                response.groupId,
                response.epoch,
                ConversationResponse.Type.GLOBAL_TEAM,
                0,
                response.teamId,
                ConvProtocol.MLS,
                DateTimeUtil.currentIsoDateTimeString(),
                response.mlsCipherSuiteTag,
                response.access,
                emptySet(),
                response.receiptMode
            )
        }
    }

    override suspend fun updateAccess(
        conversationId: ConversationId,
        updateConversationAccessRequest: UpdateConversationAccessRequest
    ): NetworkResponse<UpdateConversationAccessResponse> = try {
        httpClient.put("$PATH_CONVERSATIONS/${conversationId.domain}/${conversationId.value}/$PATH_ACCESS") {
            setBody(apiModelMapper.toApiV3(updateConversationAccessRequest))
        }.let { httpResponse ->
            when (httpResponse.status) {
                HttpStatusCode.NoContent -> NetworkResponse.Success(UpdateConversationAccessResponse.AccessUnchanged, httpResponse)
                else -> wrapKaliumResponse<EventContentDTO.Conversation.AccessUpdate> { httpResponse }
                    .mapSuccess {
                        UpdateConversationAccessResponse.AccessUpdated(it)
                    }
            }
        }
    } catch (e: IOException) {
        NetworkResponse.Error(KaliumException.GenericError(e))
    }

    companion object {
        const val PATH_TEAM = "team"
        const val PATH_GLOBAL = "global"
        const val PATH_GROUP_INFO = "groupinfo"
    }
}