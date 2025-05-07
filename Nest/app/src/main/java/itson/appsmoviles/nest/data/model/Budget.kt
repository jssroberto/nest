package itson.appsmoviles.nest.data.model

data class Budget(
    val totalBudget: Float = 0.0f,
    val categoryBudgets: Map<String, CategoryBudget> = emptyMap()
)
