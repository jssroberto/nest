package itson.appsmoviles.nest.ui.auth.signin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseUser
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.util.setupPasswordVisibilityToggle
import itson.appsmoviles.nest.ui.util.showToast
import itson.appsmoviles.nest.ui.auth.signup.SignUpActivity
import itson.appsmoviles.nest.ui.main.MainActivity

class SignInActivity : AppCompatActivity() {

    private val viewModel: SignInViewModel by viewModels()

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signUpTextView: TextView
    private lateinit var signInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        setupWindowInsets()

        findViews()

        passwordEditText.setupPasswordVisibilityToggle()
        setupClickListeners()

        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        viewModel.checkCurrentUser()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun findViews() {
        emailEditText = findViewById(R.id.editTextLoginEmail)
        passwordEditText = findViewById(R.id.editTextLoginPassword)
        signUpTextView = findViewById(R.id.textViewSignUp)
        signInButton = findViewById(R.id.buttonSignIn)
    }

    private fun setupClickListeners() {
        signInButton.setOnClickListener {
            handleSignInAttempt()
        }

        signUpTextView.setOnClickListener {
            navigateToSignUp()
        }
    }

    private fun handleSignInAttempt() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.login(email, password)
    }

    private fun observeViewModel() {
        viewModel.user.observe(this) { firebaseUser ->
            firebaseUser?.let {
                goToMain(it)
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                showToast(this, it)
                viewModel.clearError()
            }
        }
    }

    private fun navigateToSignUp() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun goToMain(user: FirebaseUser) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

}