package ai.flox.network.openai.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CompletionsResponse(
    @field:Json(name = "choices")
    val choices: List<Choice>,

    @field:Json(name = "created")
    val created: Int,

    @field:Json(name = "id")
    val id: String,

    @field:Json(name = "model")
    val model: String
)

