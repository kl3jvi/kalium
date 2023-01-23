package samples.cryptography

import com.wire.kalium.cryptography.ProteusClientImpl
import kotlinx.coroutines.Dispatchers

fun jvmInitialization() {
    val proteusClient = ProteusClientImpl(
        "rootDirectory",
        null,
        defaultContext = Dispatchers.Default,
        ioContext = Dispatchers.IO
    )
}
