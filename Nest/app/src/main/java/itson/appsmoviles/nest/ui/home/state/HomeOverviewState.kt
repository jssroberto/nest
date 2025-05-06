package itson.appsmoviles.nest.ui.home.state

data class HomeOverviewState(
    val userName: String = "User",
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netBalance: Double = totalIncome - totalExpenses,
    val budget: Double = 0.0
)