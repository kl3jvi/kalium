//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//
package com.wire.kalium.models.inbound

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.waz.model.Messages.Asset.Original
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class PhotoPreviewMessage @JsonCreator constructor(
    // TODO: dimension data class ?
    @JsonProperty("width") val height: Int,
    @JsonProperty("height") val width: Int,
    @JsonProperty("eventId") eventId: UUID,
    @JsonProperty("messageId") messageId: UUID,
    @JsonProperty("conversationId") convId: UUID,
    @JsonProperty("clientId") clientId: String,
    @JsonProperty("userId") userId: UUID,
    @JsonProperty("time") time: String,
    @JsonProperty("mimeType") mimeType: String,
    @JsonProperty("size") size: Long,
    @JsonProperty("name") name: String
) : OriginMessage(
    mimeType = mimeType,
    name = name,
    size = size,
    eventId = eventId,
    msgId = messageId,
    conversationId = convId,
    clientId = clientId,
    userId = userId,
    time = time
) {

    constructor(msg: MessageBase, original: Original) : this(
        width = original.video.width,
        height = original.video.height,
        size = original.size,
        mimeType = original.mimeType,
        name = original.name,
        eventId = msg.eventId,
        messageId = msg.messageId,
        convId = msg.conversationId,
        clientId = msg.clientId,
        userId = msg.userId,
        time = msg.time
    )
}
