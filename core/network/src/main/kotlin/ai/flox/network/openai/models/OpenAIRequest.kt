package ai.flox.network.openai.models

import com.squareup.moshi.Json

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenAIRequest(
    @field:Json(name = "model")
    val model: String = "gpt-3.5-turbo",

    @field:Json(name = "messages")
    val messages: List<Message>
) {
    companion object {
        fun fromDomain(message: String): OpenAIRequest {
            return OpenAIRequest(
                messages = listOf(
                    Message(
                        role = "system",
                        content = "You are a helpful assistant"
                    ), Message(role = "user", content = message)
                )
            )
        }
    }
}