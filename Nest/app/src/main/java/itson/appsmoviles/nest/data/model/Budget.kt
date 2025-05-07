package itson.appsmoviles.nest.data.model

data class Budget(
    var totalBudget: Float = 0f,
    var categoryBudgets: Map<String, Float> = emptyMap()
)
