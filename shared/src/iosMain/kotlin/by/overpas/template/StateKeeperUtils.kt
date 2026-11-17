package by.overpas.template

import com.arkivanov.essenty.statekeeper.SerializableContainer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.Foundation.NSCoder
import platform.Foundation.NSString
import platform.Foundation.decodeTopLevelObjectOfClass
import platform.Foundation.encodeObject

private const val STATE_KEY = "state"

private val json = Json

fun SerializableContainer.saveTo(coder: NSCoder) {
    coder.encodeObject(
        `object` = json.encodeToString(SerializableContainer.serializer(), this),
        forKey = STATE_KEY,
    )
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun restore(coder: NSCoder): SerializableContainer? {
    val encoded = coder.decodeTopLevelObjectOfClass(
        aClass = NSString,
        forKey = STATE_KEY,
        error = null,
    )
    if (encoded !is String) return null
    return runCatching {
        json.decodeFromString(SerializableContainer.serializer(), encoded)
    }.getOrNull()
}
