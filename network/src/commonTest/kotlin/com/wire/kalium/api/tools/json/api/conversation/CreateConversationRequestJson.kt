package com.wire.kalium.api.tools.json.api.conversation

import com.wire.kalium.api.tools.json.ValidJsonProvider
import com.wire.kalium.api.tools.json.model.QualifiedIDSamples
import com.wire.kalium.network.api.conversation.ConvProtocol
import com.wire.kalium.network.api.conversation.ConvTeamInfo
import com.wire.kalium.network.api.conversation.CreateConversationRequest
import com.wire.kalium.network.api.model.ConversationAccess
import com.wire.kalium.network.api.model.ConversationAccessRole

object CreateConversationRequestJson {

    val valid = ValidJsonProvider(
        CreateConversationRequest(
        listOf(QualifiedIDSamples.one),
        "group name",
        listOf(ConversationAccess.PRIVATE),
        listOf(ConversationAccessRole.TEAM_MEMBER),
        ConvTeamInfo(false, "teamID"),
        0,
        0,
        "WIRE_MEMBER",
        ConvProtocol.PROTEUS)
    ) {
        """
        |{
        |   "access": [
        |       "${it.access[0]}"
        |   ],
        |   "access_role_v2": [
        |       "${it.accessRole[0]}}"
        |   ],
        |   "conversation_role": "${it.conversationRole}",
        |   "message_timer": ${it.messageTimer},
        |   "name": "${it.name}",
        |   "protocol": "${it.protocol}",
        |   "qualified_users": [
        |       {
        |           "domain": "${it.qualifiedUsers[0].domain}",
        |           "id": "${it.qualifiedUsers[0].value}"
        |       }
        |   ],
        |   "receipt_mode": ${it.receiptMode},
        |   "team": {
        |       "managed": "false",
        |       "teamid": "${it.convTeamInfo?.teamId}"
        |   }
        |}
        """.trimIndent()
        }

}
