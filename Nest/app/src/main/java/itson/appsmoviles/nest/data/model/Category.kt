package itson.appsmoviles.nest.data.model

import itson.appsmoviles.nest.data.enum.CategoryType

data class Category(val type: CategoryType, var percentage:Float, var color: Int, var total: Float)