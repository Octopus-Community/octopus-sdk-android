package com.octopuscommunity.sample.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A user owned by the app.
 * If you don't handle one field, it will be passed as null to the SDK
 */
@Serializable
data class ClientObject(
    val id: String = UUID.randomUUID().toString(),
    val title: String? = null,
    val text: String? = null,
    val imageUrl: String? = null,
    val cta: String? = null,
    val octopusCatchPhrase: String? = null,
    val octopusViewClientObjectButtonText: String? = null,
    val octopusTopicId: String? = null,
    val signed: Boolean = true
) {
    companion object {
        val default = ClientObject(
            id = "object-id-0001",
            title = "Bridge Object Title",
            text = "This is the content text of the Bridge Object",
            imageUrl = "https://chovdelices.com/wp-content/uploads/2021/08/Caneles-1-scaled.jpg",
            cta = null,
            octopusCatchPhrase = "Tell us how what you think about it",
            octopusViewClientObjectButtonText = "View the object"
        )
    }
}