package itson.appsmoviles.nest.ui.auth.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import itson.appsmoviles.nest.data.repository.SignUpRepository
import android.util.Patterns

class SignUpViewModel : ViewModel() {

    private val repository: SignUpRepository = SignUpRepository()

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // Optional: Add a loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun signUp(email: String, name: String, password: String, confirmPassword: String) {
        _error.value = null
        _isLoading.value = true

        if (!isInputValid(email, name, password, confirmPassword)) {
            _isLoading.value = false
            return
        }

        repository.signUp(email, password, name,
            onSuccess = { firebaseUser ->
                _user.value = firebaseUser
                _error.value = null
                _isLoading.value = false
            },
            onFailure = { exception ->
                _user.value = null
                _error.value = exception.localizedMessage ?: "Sign up failed. Please try again."
                _isLoading.value = false
            }
        )
    }

    private fun isInputValid(
        email: String,
        name: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (name.isBlank()) {
            _error.value = "Name is required"
            return false
        }
        if (email.isBlank()) {
            _error.value = "Email is required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _error.value = "Invalid email format"
            return false
        }
        if (password.isBlank()) {
            _error.value = "Password is required"
            return false
        }
        if (password.length < 6) {
            _error.value = "Password must be at least 6 characters"
            return false
        }
        if (confirmPassword.isBlank()) {
            _error.value = "Confirm password is required"
            return false
        }
        if (password != confirmPassword) {
            _error.value = "Passwords do not match"
            return false
        }
        return true
    }

    fun clearError() {
        _error.value = null
    }
}