package itson.appsmoviles.nest.data.model

data class User(
    var id: String,
    var name: String,
    var email: String,
) {

    constructor() : this("", "", "")
}