package itson.appsmoviles.nest.data.model

import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.enum.PaymentMethod

data class Expense(
    override var id: String,
    override var description: String,
    override var amount: Double,
    override var date: Long,
    var category: CategoryType,
    var paymentMethod: PaymentMethod
) : Movement() {

    constructor() : this("", "", 0.0, 0L, CategoryType.OTHER, PaymentMethod.UNKNOWN)
}