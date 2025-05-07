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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.auth.signin.SignInActivity
import itson.appsmoviles.nest.ui.util.showToast

class ProfileFragment : Fragment() {
    private val auth = FirebaseAuth.getInstance()
    private lateinit var editTextUsername: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var btnLogOut: TextView
    private lateinit var editTextOldPass: EditText
    private lateinit var editTextNewPass: EditText
    private lateinit var buttonSaveChanges: Button
    // Assume R.id.alertIcon and R.id.txtAlert are ImageView and TextView respectively


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnLogOut = view.findViewById<TextView>(R.id.btn_logout)
        editTextUsername = view.findViewById<EditText>(R.id.editTextUsername)
        editTextEmail = view.findViewById<EditText>(R.id.editTextEmail)
        editTextOldPass = view.findViewById<EditText>(R.id.editTextOldPass)
        editTextNewPass = view.findViewById<EditText>(R.id.editTextNewPass)
        buttonSaveChanges = view.findViewById<Button>(R.id.buttonSaveChanges)


        setupPasswordToggle(editTextOldPass)
        setupPasswordToggle(editTextNewPass)
        loadAndDisplayUserData()

        buttonSaveChanges.setOnClickListener {
            handleSaveChanges()
        }

        btnLogOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intentSignIn = Intent(
                requireContext(),
                SignInActivity::class.java
            ) // Assuming SignInActivity exists
            intentSignIn.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intentSignIn)
        }
    }

    private fun loadAndDisplayUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            editTextUsername.setText("---")
            editTextEmail.setText("---")
            return
        }
        showUserInfo(currentUser)
    }

    private fun showUserInfo(user: FirebaseUser) {
        editTextUsername.setText(user.displayName ?: "")
        editTextEmail.setText(user.email ?: "")
    }

    private fun handleSaveChanges() {
        val newName = editTextUsername.text.toString().trim()
        val newEmail = editTextEmail.text.toString().trim()
        val oldPassword = editTextOldPass.text.toString()
        val newPassword = editTextNewPass.text.toString()


        if (newName.isEmpty()) {
            editTextUsername.error = "Name is required"
            return
        }
        editTextUsername.error = null

        if (newEmail.isEmpty()) {
            editTextEmail.error = "Email is required"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            editTextEmail.error = "Invalid email format"
            return
        }
        editTextEmail.error = null

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val originalName = currentUser.displayName ?: ""
        val originalEmail = currentUser.email ?: ""

        val isNameChanged = newName != originalName
        val isEmailChanged = newEmail != originalEmail
        val isPasswordChangeRequested = newPassword.isNotEmpty()

        if (!isNameChanged && !isEmailChanged && !isPasswordChangeRequested) {
            showToast(requireContext(), "No changes detected.")
            return
        }

        if (isEmailChanged || isPasswordChangeRequested) {
            if (oldPassword.isEmpty()) {
                editTextOldPass.error = "Current password needed"
                showToast(
                    requireContext(),
                    "Current password is required to change email or password."
                )
                return
            }
            editTextOldPass.error = null

            if (isPasswordChangeRequested) {
                if (newPassword.length < 6) {
                    editTextNewPass.error = "Min 6 characters"
                    showToast(requireContext(), "New password must be at least 6 characters.")
                    return
                }
                if (oldPassword == newPassword) {
                    editTextNewPass.error = "Must be different"
                    showToast(
                        requireContext(),
                        "New password must be different from current password."
                    )
                    return
                }
            }
            editTextNewPass.error = null

            val credential = EmailAuthProvider.getCredential(currentUser.email!!, oldPassword)
            currentUser.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    var tasksPending = 0
                    var tasksSuccessful = 0
                    val totalTasks =
                        (if (isNameChanged) 1 else 0) + (if (isEmailChanged) 1 else 0) + (if (isPasswordChangeRequested) 1 else 0)

                    val onTaskCompletion = { success: Boolean ->
                        tasksPending--
                        if (success) tasksSuccessful++
                        if (tasksPending == 0 && totalTasks > 0) {
                            if (tasksSuccessful == totalTasks) {
                                Toast.makeText(
                                    requireContext(),
                                    "All changes saved successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (tasksSuccessful > 0) {
                                showToast(requireContext(), "Some changes saved successfully.")
                            }
                            editTextOldPass.text.clear()
                            editTextNewPass.text.clear()
                        }
                    }

                    if (isNameChanged) {
                        tasksPending++
                        updateUserName(currentUser, newName, onTaskCompletion)
                    }
                    if (isEmailChanged) {
                        tasksPending++
                        updateUserEmail(currentUser, newEmail, onTaskCompletion)
                    }
                    if (isPasswordChangeRequested) {
                        tasksPending++
                        updateUserPassword(currentUser, newPassword, onTaskCompletion)
                    }
                    if (tasksPending == 0 && isNameChanged && !isEmailChanged && !isPasswordChangeRequested) {
                        // This case is for name change only, already handled if no re-auth was needed
                        // Or if re-auth was done but only name changed.
                    }


                } else {
                    editTextOldPass.error = "Incorrect password"
                    showToast(
                        requireContext(),
                        "Re-authentication failed: ${reauthTask.exception?.message}"
                    )
                }
            }
        } else if (isNameChanged) { // Only name change
            updateUserName(currentUser, newName) { success ->
                if (success) {
                    showToast(requireContext(), "Name updated successfully.")
                }
            }
        }
    }

    private fun updateUserName(
        currentUser: FirebaseUser,
        newName: String,
        callback: (Boolean) -> Unit
    ) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()
        currentUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    private fun updateUserEmail(
        currentUser: FirebaseUser,
        newEmail: String,
        callback: (Boolean) -> Unit
    ) {
        currentUser.verifyBeforeUpdateEmail(newEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast(requireContext(), "Verification email sent to $newEmail.")
                    callback(true)
                } else {
                    showToast(
                        requireContext(),
                        "Failed to send verification email: ${task.exception?.message}"
                    )
                }
            }
    }

    private fun updateUserPassword(
        currentUser: FirebaseUser,
        newPass: String,
        callback: (Boolean) -> Unit
    ) {
        currentUser.updatePassword(newPass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast(requireContext(), "Password updated successfully.")
                callback(true)
            } else {
                showToast(requireContext(), "Failed to update password: ${task.exception?.message}")
                callback(false)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle(editText: EditText) {
        var isPasswordVisible = false
        editText.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener false
            val drawable =
                editText.compoundDrawablesRelative[2] // Use compoundDrawablesRelative for LTR/RTL support
            if (drawable == null) return@setOnTouchListener false // Check if drawable exists
            if (event.rawX < (editText.right - drawable.bounds.width() - editText.paddingRight) || event.rawX > (editText.right - editText.paddingRight)) {
                return@setOnTouchListener false
            }

            v.performClick()
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                editText.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.eye_off,
                    0
                ) // eye_off
            } else {
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.eye, 0) // eye
            }
            editText.setSelection(editText.text.length)
            true
        }
    }
}