package ai.flox.network.openai.models

import com.google.gson.annotations.SerializedName

data class CompletionsResponse(
    @SerializedName("choices")
    val choices: List<Choice>,

    @SerializedName("created")
    val created: Int,

    @SerializedName("id")
    val id: String,

    @SerializedName("model")
    val model: String
)

