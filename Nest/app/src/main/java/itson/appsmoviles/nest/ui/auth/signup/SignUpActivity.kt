package itson.appsmoviles.nest.ui.auth.signup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.util.setupPasswordVisibilityToggle
import itson.appsmoviles.nest.ui.util.showToast
import itson.appsmoviles.nest.ui.auth.signin.SignInActivity
import itson.appsmoviles.nest.ui.main.MainActivity


class SignUpActivity : AppCompatActivity() {

    private val viewModel: SignUpViewModel by viewModels()

    private lateinit var emailEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var signInTextView: TextView
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        findViews()
        setupClickListeners()

        passwordEditText.setupPasswordVisibilityToggle()
        confirmPasswordEditText.setupPasswordVisibilityToggle()

        observeViewModel()
    }

    private fun findViews() {
        emailEditText = findViewById(R.id.etEmail)
        nameEditText = findViewById(R.id.etName)
        passwordEditText = findViewById(R.id.etPassword)
        confirmPasswordEditText = findViewById(R.id.etConfirmPassword)
        signInTextView = findViewById(R.id.txtSignIn)
        signUpButton = findViewById(R.id.btnSignUp)
    }

    private fun setupClickListeners() {
        signInTextView.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        signUpButton.setOnClickListener {
            handleSignUpAttempt()
        }
    }

    private fun handleSignUpAttempt() {
        val email = emailEditText.text.toString().trim()
        val name = nameEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()

        emailEditText.error = null
        nameEditText.error = null
        passwordEditText.error = null
        confirmPasswordEditText.error = null

        viewModel.signUp(email, name, password, confirmPassword)
    }

    private fun observeViewModel() {
        viewModel.user.observe(this) { firebaseUser ->
            firebaseUser?.let {
                navigateToMain()
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                showToast(this, it)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            signUpButton.isEnabled = !isLoading
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

}