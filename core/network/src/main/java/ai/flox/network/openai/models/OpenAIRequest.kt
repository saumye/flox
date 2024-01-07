package ai.flox.network.openai.models

import com.google.gson.annotations.SerializedName

data class OpenAIRequest(
    @SerializedName("model")
    val model: String = "gpt-3.5-turbo",

    @SerializedName("messages")
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