package itson.appsmoviles.nest.data.model

sealed class Movement {
    abstract var id: String
    abstract var description: String
    abstract var amount: Double
    abstract var date: Long
}