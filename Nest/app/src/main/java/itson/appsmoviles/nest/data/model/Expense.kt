package itson.appsmoviles.nest.data.model

import itson.appsmoviles.nest.data.enums.CategoryType
import itson.appsmoviles.nest.data.enums.PaymentMethod

data class Expense(
    var id: String,
    var category: CategoryType,
    var description: String,
    var amount: Float,
    var paymentMethod: PaymentMethod,
    var date: String
) {

    constructor() : this(
        id = "",
        category = CategoryType.OTHER,
        description = "",
        amount = 0.0f,
        paymentMethod = PaymentMethod.UNKNOWN,
        date = ""
    )

}