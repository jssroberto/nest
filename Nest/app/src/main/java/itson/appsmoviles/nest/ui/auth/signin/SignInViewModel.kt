package itson.appsmoviles.nest.ui.auth.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignInViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> get() = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _user.value = auth.currentUser
                    _error.value = null
                } else {
                    _user.value = null
                    _error.value = task.exception?.message ?: "Authentication failed. Please check credentials."
                }
            }
    }

    fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (_user.value != currentUser) {
            _user.value = currentUser
        }
    }

    fun clearError() {
        _error.value = null
    }

}