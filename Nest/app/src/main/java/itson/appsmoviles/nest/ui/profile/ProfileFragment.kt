package itson.appsmoviles.nest.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.auth.signin.SignInActivity
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.util.setupPasswordVisibilityToggle
import itson.appsmoviles.nest.ui.util.showToast

class ProfileFragment : Fragment() {
    private val sharedViewModel: SharedMovementsViewModel by activityViewModels()

    private val auth = FirebaseAuth.getInstance()
    private lateinit var editTextUsername: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var btnLogOut: TextView
    private lateinit var editTextNewPass: EditText
    private lateinit var editTextConfirmPass: EditText
    private lateinit var editTextOldPass: EditText
    private lateinit var buttonSaveChanges: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnLogOut = view.findViewById(R.id.btn_logout)
        editTextUsername = view.findViewById(R.id.editTextUsername)
        editTextEmail = view.findViewById(R.id.editTextEmail)
        editTextOldPass = view.findViewById(R.id.editTextOldPass)
        editTextNewPass = view.findViewById(R.id.editTextNewPass)
        buttonSaveChanges = view.findViewById(R.id.buttonSaveChanges)
        editTextConfirmPass = view.findViewById(R.id.editTextConfirmPass)

        editTextOldPass.setupPasswordVisibilityToggle()
        editTextNewPass.setupPasswordVisibilityToggle()
        editTextConfirmPass.setupPasswordVisibilityToggle()
        loadAndDisplayUserData()

        buttonSaveChanges.setOnClickListener {
            handleSaveChanges()
        }

        btnLogOut.setOnClickListener {
            auth.signOut()
            val intentSignIn = Intent(requireContext(), SignInActivity::class.java)
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
        val confirmPassword = editTextConfirmPass.text.toString()

        if (!validateInputs(newName, newEmail)) return

        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast(requireContext(), "User not logged in.")
            return
        }

        val originalName = currentUser.displayName ?: ""
        val originalEmail = currentUser.email ?: ""

        val isNameChanged = newName != originalName
        val isEmailChanged = newEmail != originalEmail
        val isPasswordChangeRequested = newPassword.isNotEmpty() || confirmPassword.isNotEmpty()

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

            if (isPasswordChangeRequested && !validatePasswordChange(
                    oldPassword,
                    newPassword,
                    confirmPassword
                )
            ) {
                return
            }
            reauthenticateAndUpdateUser(
                currentUser,
                oldPassword,
                newName,
                newEmail,
                newPassword,
                isNameChanged,
                isEmailChanged,
                isPasswordChangeRequested
            )
        } else if (isNameChanged) {
            updateUserName(currentUser, newName) { success ->
                showToast(
                    requireContext(),
                    if (success) "Name updated successfully." else "Failed to update name."
                )
            }
        }
    }

    private fun validateInputs(name: String, email: String): Boolean {
        if (name.isEmpty()) {
            editTextUsername.error = "Name is required"
            return false
        }
        editTextUsername.error = null

        if (email.isEmpty()) {
            editTextEmail.error = "Email is required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.error = "Invalid email format"
            return false
        }
        editTextEmail.error = null
        return true
    }

    private fun validatePasswordChange(
        oldPass: String,
        newPass: String,
        confirmPass: String
    ): Boolean {
        if (newPass.isEmpty()) {
            editTextNewPass.error = "New password is required"
            return false
        }
        editTextNewPass.error = null

        if (confirmPass.isEmpty()) {
            editTextConfirmPass.error = "Confirm password is required"
            return false
        }
        editTextConfirmPass.error = null

        if (newPass.length < 6) {
            editTextNewPass.error = "Min 6 characters"
            showToast(requireContext(), "New password must be at least 6 characters.")
            return false
        }
        editTextNewPass.error = null

        if (newPass != confirmPass) {
            editTextConfirmPass.error = "Passwords do not match"
            showToast(requireContext(), "New password and confirm password do not match.")
            return false
        }
        editTextConfirmPass.error = null

        if (oldPass == newPass) {
            editTextNewPass.error = "Must be different"
            showToast(requireContext(), "New password must be different from current password.")
            return false
        }
        editTextNewPass.error = null
        return true
    }

    private fun reauthenticateAndUpdateUser(
        user: FirebaseUser,
        oldPasswordVal: String,
        newNameVal: String,
        newEmailVal: String,
        newPasswordVal: String,
        nameChanged: Boolean,
        emailChanged: Boolean,
        passwordChangeRequested: Boolean
    ) {
        val credential = EmailAuthProvider.getCredential(user.email!!, oldPasswordVal)
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (!reauthTask.isSuccessful) {
                editTextOldPass.error = "Incorrect password"
                showToast(
                    requireContext(),
                    "Re-authentication failed: ${reauthTask.exception?.message}"
                )
                return@addOnCompleteListener
            }

            performUserUpdates(
                user,
                newNameVal,
                newEmailVal,
                newPasswordVal,
                nameChanged,
                emailChanged,
                passwordChangeRequested
            )
        }
    }

    private fun performUserUpdates(
        user: FirebaseUser,
        newName: String,
        newEmail: String,
        newPasswordValue: String,
        isNameChanged: Boolean,
        isEmailChanged: Boolean,
        isPasswordChangeRequested: Boolean
    ) {
        val operations = mutableListOf<Pair<() -> Unit, String>>()

        if (isNameChanged) operations.add({
            updateUserName(
                user,
                newName,
                getUpdateCallback("Name")
            )
        } to "Name")
        if (isEmailChanged) operations.add({
            updateUserEmail(
                user,
                newEmail,
                getUpdateCallback("Email")
            )
        } to "Email")
        if (isPasswordChangeRequested) operations.add({
            updateUserPassword(
                user,
                newPasswordValue,
                getUpdateCallback("Password")
            )
        } to "Password")

        if (operations.isEmpty()) {
            showToast(requireContext(), "No changes to apply after re-authentication.")
            clearPasswordFields()
            return
        }

        var tasksCompleted = 0
        var tasksSucceeded = 0

        val totalTasks = operations.size

        val commonCallback: (Boolean, String) -> Unit = { success, type ->
            tasksCompleted++
            if (success) tasksSucceeded++

            if (tasksCompleted == totalTasks) {
                when {
                    tasksSucceeded == totalTasks -> showToast(
                        requireContext(),
                        "All changes saved successfully."
                    )

                    tasksSucceeded > 0 -> showToast(
                        requireContext(),
                        "Some changes saved successfully."
                    )

                    else -> showToast(requireContext(), "Failed to save any changes.")
                }
                clearPasswordFields()
            }
        }

        operations.forEach { operationPair ->
            val updateFunction = operationPair.first
            updateFunction()
        }
        var pendingTasks = operations.size
        var successfulTasks = 0

        if (pendingTasks == 0) {
            clearPasswordFields()
            return
        }

        val taskCompletionHandler = { success: Boolean ->
            pendingTasks--
            if (success) successfulTasks++
            if (pendingTasks == 0) {
                when {
                    successfulTasks == operations.size -> showToast(
                        requireContext(),
                        "All changes saved successfully."
                    )

                    successfulTasks > 0 -> showToast(
                        requireContext(),
                        "Some changes saved successfully."
                    )

                    else -> showToast(
                        requireContext(),
                        "Failed to save changes after re-authentication."
                    )
                }
                clearPasswordFields()
            }
        }

        if (isNameChanged) {
            updateUserName(user, newName, taskCompletionHandler)
        }
        if (isEmailChanged) {
            updateUserEmail(user, newEmail, taskCompletionHandler)
        }
        if (isPasswordChangeRequested) {
            updateUserPassword(user, newPasswordValue, taskCompletionHandler)
        }
    }


    private fun getUpdateCallback(type: String): (Boolean) -> Unit {
        return { success ->
        }
    }


    private fun clearPasswordFields() {
        editTextOldPass.text.clear()
        editTextNewPass.text.clear()
        editTextConfirmPass.text.clear()
    }

    private fun updateUserName(
        currentUser: FirebaseUser,
        newName: String,
        callback: (Boolean) -> Unit
    ) {
        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
        currentUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // User name updated in Firebase successfully
                sharedViewModel.signalUserNameUpdated() // Notify observers (like HomeFragment)
                // showToast(requireContext(),"Name updated successfully.") // This toast can be shown by the aggregate status
            } else {
                showToast(requireContext(), "Failed to update name: ${task.exception?.message}")
            }
            callback(task.isSuccessful)
        }
    }

    private fun updateUserEmail(
        currentUser: FirebaseUser,
        newEmail: String,
        callback: (Boolean) -> Unit
    ) {
        currentUser.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast(requireContext(), "Verification email sent to $newEmail.")
            } else {
                showToast(
                    requireContext(),
                    "Failed to send verification email: ${task.exception?.message}"
                )
            }
            callback(task.isSuccessful)
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
            } else {
                showToast(requireContext(), "Failed to update password: ${task.exception?.message}")
            }
            callback(task.isSuccessful)
        }
    }

}