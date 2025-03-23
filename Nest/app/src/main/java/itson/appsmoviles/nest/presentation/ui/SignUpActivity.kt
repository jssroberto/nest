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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.MainActivity
import itson.appsmoviles.nest.R


class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        val email: EditText = findViewById(R.id.etEmail)
        val name: EditText = findViewById(R.id.etName)
        val password: EditText = findViewById(R.id.etPassword)
        val confirmPassword: EditText = findViewById(R.id.etConfirmPassword)
        val signIn: TextView = findViewById(R.id.txtSignIn)
        val btnSignUp: Button = findViewById(R.id.btnSignUp)

        setupPasswordToggle(password)
        setupPasswordToggle(confirmPassword)

        signIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        btnSignUp.setOnClickListener {
            val emailText = email.text.toString().trim()
            val nameText = name.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (validarCampos()) {
                signUp(emailText, passwordText, nameText)
            }
        }
    }

    private fun signUp(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {

                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user.updateProfile(profileUpdates).addOnCompleteListener {
                            Log.d("INFO", "Nombre actualizado en FirebaseAuth")
                        }


                        guardarNombreEnDatabase(user.uid, name)


                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("user", email)
                            putExtra("name", name)
                        }
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Log.w("ERROR", "Registro fallido", task.exception)
                    Toast.makeText(
                        this,
                        "Error: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun guardarNombreEnDatabase(uid: String, name: String) {
        val database = FirebaseDatabase.getInstance().reference
        database.child("usuarios").child(uid).setValue(name)
            .addOnSuccessListener { Log.d("INFO", "Nombre guardado en Realtime Database") }
            .addOnFailureListener { e ->
                Log.e(
                    "ERROR",
                    "Error al guardar en Realtime Database",
                    e
                )
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
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
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










