package itson.appsmoviles.nest.domain.model.entity

data class Expense(
    var id: Long = 0,
    var categoria: String = "",
    var descripcion: String = "",
    var monto: Float = 0.0f,
    var payment: String = ""
) {
    constructor() : this(0, "", "", 0.0f)
}
