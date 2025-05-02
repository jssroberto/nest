package itson.appsmoviles.nest.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import itson.appsmoviles.nest.data.model.Expense
import itson.appsmoviles.nest.data.enums.CategoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.Normalizer

// Define constants within the ViewModel or a shared Constants object
private const val NODE_EXPENSES = "expenses"
private const val NODE_INCOMES = "incomes"
private const val USERS_NODE = "users"
private const val TAG = "HomeViewModel"

class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // User Info
    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?> get() = _userName

    // Totals
    private val _totalIncome = MutableLiveData<Result<Double>>()
    val totalIncome: LiveData<Result<Double>> get() = _totalIncome

    private val _totalExpenses = MutableLiveData<Result<Double>>()
    val totalExpenses: LiveData<Result<Double>> get() = _totalExpenses

    // Expenses List (Raw and Displayed)
    private var _fullExpenseList: List<Expense> = listOf() // Internal full list

    private val _displayedExpenses = MutableLiveData<List<Expense>>()
    val displayedExpenses: LiveData<List<Expense>> get() = _displayedExpenses

    // Budget Bar Data
    private val _budgetDistribution = MutableLiveData<Map<CategoryType, Float>>()
    val budgetDistribution: LiveData<Map<CategoryType, Float>> get() = _budgetDistribution

    // Loading State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Keep track of the current search query
    private var currentSearchQuery: String = ""

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = auth.currentUser
            if (currentUser == null) {
                // Handle not logged in state if necessary
                _userName.value = null // Or a default value
                _totalIncome.value = Result.success(0.0)
                _totalExpenses.value = Result.success(0.0)
                _fullExpenseList = emptyList()
                _displayedExpenses.value = emptyList()
                _budgetDistribution.value = emptyMap()
                _isLoading.value = false
                Log.w(TAG, "No authenticated user found.")
                return@launch
            }

            _userName.value = currentUser.displayName ?: "User"

            // Fetch totals and expenses concurrently if desired, or sequentially
            fetchTotals(currentUser.uid) // Fetches both income and expenses
            fetchExpensesData(currentUser.uid) // Fetches expenses list and updates derived data

            // Note: isLoading is set to false within fetchExpensesData after processing
        }
    }

    // Public function to trigger a refresh, e.g., after an update
    fun refreshData() {
        loadInitialData() // Reload everything
    }

    // Fetches both income and expense totals
    private fun fetchTotals(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Use separate calls or combine if your backend structure allows
            calculateTotalForNode(userId, NODE_INCOMES) { result ->
                _totalIncome.postValue(result) // Use postValue when updating from background thread
            }
            calculateTotalForNode(userId, NODE_EXPENSES) { result ->
                _totalExpenses.postValue(result)
            }
        }
    }

    // Fetches the raw expense list and updates related LiveData
    private fun fetchExpensesData(userId: String) {
        _isLoading.postValue(true) // Indicate loading started
        val expensesRef = database.child(USERS_NODE).child(userId).child(NODE_EXPENSES)

        expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val expenses = snapshot.children.mapNotNull { it.getValue(Expense::class.java) }
                _fullExpenseList = expenses.sortedByDescending { it.date } // Update internal list

                // Update budget distribution
                _budgetDistribution.postValue(calculateBudgetDistribution(_fullExpenseList))

                // Apply current filter to the new full list
                filterExpenses(currentSearchQuery) // This will update _displayedExpenses

                _isLoading.postValue(false) // Indicate loading finished
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to fetch expenses: ${error.message}", error.toException())
                _fullExpenseList = emptyList() // Clear list on error
                _displayedExpenses.postValue(emptyList())
                _budgetDistribution.postValue(emptyMap())
                _isLoading.postValue(false) // Indicate loading finished (with error)
                // Consider exposing an error state via another LiveData
            }
        })
    }


    // --- Calculation and Filtering Logic ---

    private fun calculateBudgetDistribution(expenses: List<Expense>): Map<CategoryType, Float> {
        // Guard clause for empty list
        if (expenses.isEmpty()) {
            return emptyMap()
        }
        return expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }
    }

    fun filterExpenses(query: String) {
        currentSearchQuery = query // Store the latest query
        val queryProcessed = query.lowercase().trim().unaccent()

        viewModelScope.launch(Dispatchers.Default) { // Use Default dispatcher for CPU-bound filtering
            val filteredList = if (queryProcessed.isEmpty()) {
                _fullExpenseList // Use the internal full list
            } else {
                _fullExpenseList.filter { expense ->
                    // Guard clause for potentially null fields if applicable
                    val descriptionProcessed = expense.description?.lowercase()?.unaccent() ?: ""
                    val categoryProcessed = expense.category.name.lowercase().unaccent()
                    val amountProcessed = expense.amount.toString() // No need for lowercase/unaccent on numbers usually

                    descriptionProcessed.contains(queryProcessed) ||
                            categoryProcessed.contains(queryProcessed) ||
                            amountProcessed.contains(queryProcessed) // Keep if searching amount as string is needed
                }
            }
            _displayedExpenses.postValue(filteredList) // Update the LiveData for the UI
        }
    }

    // --- Firebase Helper Logic ---

    private fun calculateTotalForNode(
        userId: String,
        nodeName: String,
        onResult: (Result<Double>) -> Unit
    ) {
        // Guard clause: Check if userId is valid before proceeding
        if (userId.isBlank()) {
            Log.w(TAG, "Invalid userId provided for calculating total.")
            onResult(Result.failure(IllegalArgumentException("User ID cannot be blank")))
            return
        }

        val nodeRef: DatabaseReference = database
            .child(USERS_NODE)
            .child(userId)
            .child(nodeName)

        nodeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Guard clause: Early return if snapshot doesn't exist
                if (!dataSnapshot.exists()) {
                    Log.d(TAG, "Node '$nodeName' does not exist for user '$userId'")
                    onResult(Result.success(0.0))
                    return
                }

                try {
                    val totalSum = dataSnapshot.children
                        .mapNotNull { itemSnapshot ->
                            getAmountFromSnapshot(itemSnapshot)
                        }
                        .sum()
                    onResult(Result.success(totalSum))
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing data for node '$nodeName'", e)
                    onResult(Result.failure(e))
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(
                    TAG,
                    "Database error reading node '$nodeName' for user '$userId': ${databaseError.message}",
                    databaseError.toException()
                )
                onResult(Result.failure(databaseError.toException()))
            }
        })
    }

    private fun getAmountFromSnapshot(snapshot: DataSnapshot): Double? {
        val amountChild = snapshot.child("amount")
        // Guard clause: Return null if 'amount' child doesn't exist
        if (!amountChild.exists()) return null

        // Prioritize Double, then Long, then handle potential type mismatch
        return when (val value = amountChild.value) {
            is Double -> value
            is Long -> value.toDouble()
            is String -> value.toDoubleOrNull() // Attempt conversion if stored as String
            else -> {
                Log.w(TAG, "Unexpected type for amount: ${value?.javaClass?.name}")
                null
            }
        }
    }

    // --- String Helper ---
    private fun String.unaccent(): String {
        // Guard clause for empty string
        if (this.isEmpty()) {
            return this
        }
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        return regex.replace(normalized, "")
    }
}