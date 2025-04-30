package itson.appsmoviles.nest.domain.model.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun signUp(
        email: String,
        password: String,
        name: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { creationTask ->
                if (!creationTask.isSuccessful) {
                    val error =
                        creationTask.exception ?: Exception("Error desconocido al crear usuario")
                    Log.e("RegisterRepository", "Error al crear usuario en FirebaseAuth", error)
                    onFailure(error)
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    val error = Exception("Usuario creado pero auth.currentUser es null")
                    Log.e("RegisterRepository", error.message ?: "Error desconocido")
                    onFailure(error)
                    return@addOnCompleteListener
                }

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { updateTask ->
                        if (!updateTask.isSuccessful) {
                            val error = updateTask.exception
                                ?: Exception("Error desconocido al actualizar perfil")
                            Log.e(
                                "RegisterRepository",
                                "Error al actualizar perfil en FirebaseAuth para ${user.email}",
                                error
                            )

                            onFailure(error)

                            // Option 2: Log warning and proceed (user exists but name isn't set yet)
                            // Log.w("RegisterRepository", "Perfil no actualizado, pero se procederá a guardar en DB.")
                            // guardarUsuarioEnDatabase(user.uid, name, email, onSuccess, onFailure)
                            return@addOnCompleteListener

                        }
                        Log.d(
                            "RegisterRepository",
                            "Nombre actualizado en FirebaseAuth para ${user.email}"
                        )
                        guardarUsuarioEnDatabase(user.uid, name, email, onSuccess, onFailure)
                    }
            }
    }

    private fun guardarUsuarioEnDatabase(
        uid: String,
        name: String,
        email: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val usuario = hashMapOf(
            "name" to name,
            "email" to email,
            "incomes" to emptyMap<String, Any>(),
            "expenses" to emptyMap<String, Any>()
        )

        database.child("users").child(uid).setValue(usuario)
            .addOnSuccessListener {
                Log.d("RegisterRepository", "Usuario guardado en Realtime Database: $uid")
                onSuccess(auth.currentUser!!)
            }
            .addOnFailureListener { e ->
                Log.e(
                    "RegisterRepository",
                    "Error al guardar usuario en Realtime Database: $uid",
                    e
                )
                onFailure(e)
            }
    }
}