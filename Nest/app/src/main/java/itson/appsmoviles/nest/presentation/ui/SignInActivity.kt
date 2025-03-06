package itson.appsmoviles.nest.presentation.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import itson.appsmoviles.nest.MainActivity
import itson.appsmoviles.nest.R

class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editTextLoginPassword = findViewById<EditText>(R.id.editTextLoginPassword)
        val textViewSignUp = findViewById<TextView>(R.id.textViewSignUp)
        val buttonSignIn = findViewById<Button>(R.id.buttonSignIn)

        setupPasswordToggle(editTextLoginPassword)

        buttonSignIn.setOnClickListener {
            val intentSignIn = Intent(this, MainActivity::class.java)
            startActivity(intentSignIn)
        }

        textViewSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)

        }
    }

    fun validarCampos(): Boolean {
        val editTextLoginPassword = findViewById<EditText>(R.id.editTextLoginPassword)
        val editTextLoginEmail = findViewById<EditText>(R.id.editTextLoginEmail)
        val alertIcon = findViewById<ImageView>(R.id.alertIcon)
        val textAlert = findViewById<TextView>(R.id.txtAlert)
        val email = editTextLoginEmail.text.toString().trim()
        val password = editTextLoginPassword.text.toString().trim()

        if (email.isEmpty()) {
            editTextLoginEmail.error = "Email is required"
            return false
        }
        if (!esEmailValido(email)) {
            editTextLoginEmail.error = "Invalid email format"
            return false
        }
        if (password.isEmpty()) {
            alertIcon.visibility = View.VISIBLE
            textAlert.visibility = View.VISIBLE
            textAlert.setText("Password is required")

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