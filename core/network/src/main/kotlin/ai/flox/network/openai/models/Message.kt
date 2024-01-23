package ai.flox.network.openai.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Message(
    @field:Json(name = "content")
    val content: String,

    @field:Json(name = "role")
    val role: String
)