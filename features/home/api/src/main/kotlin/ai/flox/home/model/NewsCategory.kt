package ai.flox.home.model

enum class NewsCategory(val displayName: String, val apiValue: String) {
    TOP("Top", "general"),
    BUSINESS("Business", "business"),
    ENTERTAINMENT("Entertainment", "entertainment"),
    HEALTH("Health", "health"),
    SCIENCE("Science", "science"),
    SPORTS("Sports", "sports"),
    TECHNOLOGY("Technology", "technology");

    companion object {
        fun fromApiValue(value: String): NewsCategory {
            return entries.find { it.apiValue == value } ?: TOP
        }
    }
} 