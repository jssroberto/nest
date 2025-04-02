package itson.appsmoviles.nest.domain.model.entity

import android.os.Build
import androidx.annotation.RequiresApi
import itson.appsmoviles.nest.domain.model.enums.Category

data class Expense(
    var id: String = "",
    var category: Category,
    var description: String = "",
    var amount: Float = 0.0f,
    var paymentMethod: String = "",
    var date: String
) {
    @RequiresApi(Build.VERSION_CODES.O)
    constructor() : this("", Category.OTHER, "", 0.0f, "", "")
}
