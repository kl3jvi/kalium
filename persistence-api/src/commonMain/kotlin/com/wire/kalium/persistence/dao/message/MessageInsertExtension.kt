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
package com.wire.kalium.persistence.dao.message

internal fun MessageEntityContent.Asset.hasValidRemoteData() =
    assetId.isNotEmpty() && assetOtrKey.isNotEmpty() && assetSha256Key.isNotEmpty()

internal fun ByteArray?.isNullOrEmpty() = this?.isEmpty() ?: true

/**
 * Explaining that this is mainly used to share a bit of logic between MessageDAO and MigrationDAO
 */
internal interface MessageInsertExtension {
    /**
     * Returns true if the [message] is an asset message that is already in the DB and any of its decryption keys are null/empty. This means
     * that this asset message that is in the DB was only a preview message with valid metadata but no valid keys (Web clients send 2
     * separated messages). Therefore, it still needs to be updated with the valid keys in order to be displayed.
     */
    fun isValidAssetMessageUpdate(message: MessageEntity): Boolean
    fun updateAssetMessage(message: MessageEntity)
    fun contentTypeOf(content: MessageEntityContent): MessageEntity.ContentType
    fun insertMessageOrIgnore(message: MessageEntity)
}
