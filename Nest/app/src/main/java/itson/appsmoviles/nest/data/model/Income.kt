package itson.appsmoviles.nest.data.model

data class Income(
    override var id: String,
    override var description: String,
    override var amount: Double,
    override var date: Long
) : Movement() {

    constructor() : this("", "", 0.0, 0L)
}