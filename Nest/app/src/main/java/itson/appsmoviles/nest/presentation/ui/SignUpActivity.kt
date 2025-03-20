package itson.appsmoviles.nest.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import itson.appsmoviles.nest.MainActivity
import itson.appsmoviles.nest.R
import kotlinx.coroutines.Dispatchers.Main
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore


class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = Firebase.auth

        val email: EditText = findViewById(R.id.etEmail)
        val name: EditText = findViewById(R.id.etName)
        val password = findViewById<EditText>(R.id.etPassword)
        val editConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val signIn = findViewById<TextView>(R.id.txtSignIn)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        setupPasswordToggle(editConfirmPassword)
        setupPasswordToggle(password)

        signIn.setOnClickListener {
            val intentSignIn = Intent(this, SignInActivity::class.java)
            startActivity(intentSignIn)
        }

        btnSignUp.setOnClickListener {
            if (validarCampos()) {
                signUp(email.text.toString(), password.text.toString(), name.text.toString())
            }
        }
    }

    private fun signUp(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("INFO", "Registro exitoso con email: $email")

                    // 🔹 Obtener usuario autenticado
                    val user = auth.currentUser

                    // 🔹 Guardar el nombre en Firebase Authentication
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)  // Guarda el nombre en Firebase Auth
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            Log.d("INFO", "Nombre guardado correctamente en FirebaseAuth")
                        }
                    }


                    val userId = user?.uid
                    val db = FirebaseFirestore.getInstance()
                    val userData = hashMapOf(
                        "uid" to userId,
                        "name" to name,
                        "email" to email
                    )

                    db.collection("users").document(userId!!)
                        .set(userData)
                        .addOnSuccessListener { Log.d("INFO", "Usuario guardado en Firestore") }
                        .addOnFailureListener { e -> Log.e("ERROR", "Error al guardar en Firestore", e) }


                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("user", email)
                        putExtra("name", name)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Log.w("ERROR", "Registro fallido", task.exception)
                    Toast.makeText(
                        baseContext,
                        "El registro falló: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }



    fun validarCampos(): Boolean {
        val editTextPassword = findViewById<EditText>(R.id.etPassword)
        val editConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val editName = findViewById<EditText>(R.id.etName)
        val editMail = findViewById<EditText>(R.id.etEmail)
        val alertIcon = findViewById<ImageView>(R.id.alertIcon)
        val textAlert = findViewById<TextView>(R.id.txtAlert)
        val nombre = editName.text.toString().trim()
        val email = editMail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val confirmPassword = editConfirmPassword.text.toString().trim()

        if (nombre.isEmpty()) {
            editName.error = "Name is required"
            return false
        }
        if (email.isEmpty()) {
            editMail.error = "Email is required"
            return false
        }
        if (!esEmailValido(email)) {
            editMail.error = "Invalid email format"
            return false
        }
        if (password.isEmpty()) {
            alertIcon.visibility = View.VISIBLE
            textAlert.visibility = View.VISIBLE
            textAlert.setText("Password is required")

            return false
        }
        if (confirmPassword.isEmpty()) {
            alertIcon.visibility = View.VISIBLE
            textAlert.visibility = View.VISIBLE
            textAlert.setText("Confirm password is required")
            return false
        }
        if (password != confirmPassword) {
            alertIcon.visibility = View.VISIBLE
            textAlert.visibility = View.VISIBLE
            textAlert.setText("Passwords do not match")
            return false
        }
        return true
    }


    fun esEmailValido(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }


    @SuppressLint("ClickableViewAccessibility")
    fun setupPasswordToggle(editText: EditText) {
        var isPasswordVisible = false

        editText.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }

            val drawable = editText.compoundDrawables[2] ?: return@setOnTouchListener false
            if (event.rawX < (editText.right - drawable.bounds.width())) {
                return@setOnTouchListener false
            }

            v.performClick()

            isPasswordVisible = !isPasswordVisible
            if (!isPasswordVisible) {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                editText.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.eye,
                    0
                )
            } else {
                editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                editText.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.eye_off,
                    0
                )
            }

            editText.setSelection(editText.text.length)
            return@setOnTouchListener true
        }
    }

}








