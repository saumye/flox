package ai.flox.network.openai.models

import com.squareup.moshi.Json

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Choice(
    @field:Json(name = "message")
    val message: Message
)