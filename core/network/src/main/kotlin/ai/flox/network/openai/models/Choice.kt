package ai.flox.network.openai.models

import com.google.gson.annotations.SerializedName

data class Choice(
    @SerializedName("message")
    val message: Message
)