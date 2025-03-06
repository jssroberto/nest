package itson.appsmoviles.nest.domain.model

import itson.appsmoviles.nest.domain.model.enums.Category
import java.time.LocalDateTime

data class Movement(
    val id: Long,
    val category: Category,
    val description: String,
    val amount: Float,
    val date: LocalDateTime,
) {

}
