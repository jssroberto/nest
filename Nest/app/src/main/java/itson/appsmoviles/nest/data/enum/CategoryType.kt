package itson.appsmoviles.nest.data.enum

enum class CategoryType(val displayName: String) {
    LIVING("Living"),
    RECREATION("Recreation"),
    TRANSPORT("Transport"),
    FOOD("Food"),
    HEALTH("Health"),
    OTHER("Other");

    companion object {
        fun fromDisplayName(displayName: String?): CategoryType {
            if (displayName.isNullOrBlank()) {
                return OTHER
            }

            return entries.firstOrNull {
                it.displayName.equals(displayName.trim(), ignoreCase = true)
            } ?: OTHER
        }

        fun fromName(name: String?): CategoryType {
            if (name.isNullOrBlank()) {
                return OTHER
            }
            return try {
                valueOf(name.trim().uppercase())
            } catch (e: IllegalArgumentException) {
                OTHER
            }
        }
    }
}