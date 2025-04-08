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
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener {
                                Log.d("INFO", "Nombre actualizado en FirebaseAuth")
                            }


                        guardarUsuarioEnDatabase(user.uid, name, email, onSuccess, onFailure)
                    }
                } else {
                    task.exception?.let { onFailure(it) }
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
            "nombre" to name,
            "email" to email,
            "gastos" to emptyMap<String, Any>()
        )

        database.child("usuarios").child(uid).setValue(usuario)
            .addOnSuccessListener {
                Log.d("INFO", "Usuario guardado en Realtime Database")
                onSuccess(auth.currentUser!!)
            }
            .addOnFailureListener { e ->
                Log.e("ERROR", "Error al guardar usuario en Realtime Database", e)
                onFailure(e)
            }
    }
}