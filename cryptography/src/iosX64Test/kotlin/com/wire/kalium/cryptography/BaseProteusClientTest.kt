package com.wire.kalium.cryptography

import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL

actual open class BaseProteusClientTest actual constructor() {
    actual fun createProteusClient(userId: PlainUserId): ProteusClient {
        val rootDir = NSURL.fileURLWithPath(NSTemporaryDirectory() + "/proteus", isDirectory = true)
        return ProteusClientImpl(rootDir.absoluteString!!, userId.value)
    }

}
