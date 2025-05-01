package itson.appsmoviles.nest.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.auth.signin.SignInActivity

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLogOut = view.findViewById<TextView>(R.id.btn_logout)
        val editTextOldPass = view.findViewById<EditText>(R.id.editTextOldPass)
        val editTextNewPass = view.findViewById<EditText>(R.id.editTextNewPass)
        val buttonSaveChanges = view.findViewById<Button>(R.id.buttonSaveChanges)

        setupPasswordToggle(editTextOldPass)
        setupPasswordToggle(editTextNewPass)

        buttonSaveChanges.setOnClickListener {
            if (!validarCampos(view)) {
                Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()


            }
        }

        btnLogOut.setOnClickListener(){
            FirebaseAuth.getInstance().signOut()
            val intentSignIn = Intent(requireContext(), SignInActivity::class.java)
            intentSignIn.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intentSignIn)
        }
    }

    private fun validarCampos(view: View): Boolean {
        val editTextName = view.findViewById<EditText>(R.id.editTextName)
        val editTextEmail = view.findViewById<EditText>(R.id.editTextEmail)
        val editTextOldPass = view.findViewById<EditText>(R.id.editTextOldPass)
        val editTextNewPass = view.findViewById<EditText>(R.id.editTextNewPass)
        val alertIcon = view.findViewById<ImageView>(R.id.alertIcon)
        val textAlert = view.findViewById<TextView>(R.id.txtAlert)


        val nombre = editTextName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val password = editTextOldPass.text.toString().trim()
        val newPassword = editTextNewPass.text.toString().trim()

        if (nombre.isEmpty()) {
            editTextName.error = "Name is required"
            return false
        }
        if (email.isEmpty()) {
            editTextEmail.error = "Email is required"
            return false
        }
        if (!esEmailValido(email)) {
            editTextEmail.error = "Invalid email format"
            return false
        }
        if (password.isEmpty()) {
            alertIcon.visibility = View.VISIBLE
            textAlert.visibility = View.VISIBLE
            textAlert.text = "Password is required"
            return false
        }
        if (newPassword.isEmpty()) {
            alertIcon.visibility = View.VISIBLE
            textAlert.visibility = View.VISIBLE
            textAlert.text = "New password is required"
            return false
        }
        if (password == newPassword) {
            alertIcon.visibility = View.VISIBLE
            textAlert.visibility = View.VISIBLE
            textAlert.text = "New password cannot be the same as the current one"
            return false
        }
        return true
    }

    private fun esEmailValido(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle(editText: EditText) {
        var isPasswordVisible = false

        editText.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener false

            val drawable = editText.compoundDrawables[2] ?: return@setOnTouchListener false
            if (event.rawX < (editText.right - drawable.bounds.width())) return@setOnTouchListener false

            v.performClick()
            isPasswordVisible = !isPasswordVisible

            if (!isPasswordVisible) {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eye, 0)
            } else {
                editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eye_off, 0)
            }

            editText.setSelection(editText.text.length)
            return@setOnTouchListener true
        }
    }
}