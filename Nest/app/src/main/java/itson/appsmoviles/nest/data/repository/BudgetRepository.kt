package itson.appsmoviles.nest.data.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Budget
import itson.appsmoviles.nest.data.model.CategoryBudget
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class BudgetRepository {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance()
        .getReference("users")
        .child(userId ?: "unknown")
    private val _budgetLiveData = MutableLiveData<Budget?>()
    val budgetLiveData: MutableLiveData<Budget?> get() = _budgetLiveData

    // Método para obtener el presupuesto de Firebase
    suspend fun getBudgetDataSuspend(): Budget? = suspendCoroutine { continuation ->
        dbRef.child("budget").get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue(Budget::class.java)
                _budgetLiveData.postValue(budget) // Actualiza el LiveData
                continuation.resume(budget)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    // Método para actualizar el presupuesto en Firebase
    suspend fun updateBudget(budget: Budget) {
        try {
            dbRef.child("budget").setValue(budget)
                .addOnSuccessListener {
                    _budgetLiveData.postValue(budget) // Refresca el LiveData
                }
                .addOnFailureListener { exception ->
                    Log.e("BudgetRepository", "Failed to update budget: ${exception.message}")
                }
        } catch (e: Exception) {
            Log.e("BudgetRepository", "Error updating budget", e)
        }
    }

    fun updateCategoryBudgetAmount(
        category: CategoryType,
        categoryBudget: Float,
        alarmThreshold: Double? = null,
        alarmEnabled: Boolean
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BudgetRepository", "User not authenticated")
            return
        }

        val categoryRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("budget")
            .child("categoryBudgets")
            .child(category.name)


        val categoryData = CategoryBudget(
            categoryBudget = categoryBudget,
            alarmThreshold = alarmThreshold,
            alarmEnabled = alarmEnabled
        )


        categoryRef.setValue(categoryData)
            .addOnSuccessListener {
                Log.d("BudgetRepository", "Category budget updated for $category")
            }
            .addOnFailureListener { e ->
                Log.e("BudgetRepository", "Failed to update category budget", e)
            }
    }


    fun updateCategoryAlarmThreshold(
        category: CategoryType,
        categoryBudget: Float,
        alarmThreshold: Double? = null,
        alarmEnabled: Boolean
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BudgetRepository", "User not authenticated")
            return
        }


        val categoryRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("budget")
            .child("categoryBudgets")
            .child(category.name)


        val categoryData = CategoryBudget(
            categoryBudget = categoryBudget,
            alarmThreshold = alarmThreshold,
            alarmEnabled = alarmEnabled
        )

        categoryRef.setValue(categoryData)
            .addOnSuccessListener {
                Log.d("BudgetRepository", "Category alarm configuration updated for $category")
            }
            .addOnFailureListener { e ->
                Log.e("BudgetRepository", "Failed to update alarm configuration", e)
            }
    }

    fun saveBudget(budget: Budget, onComplete: (() -> Unit)? = null) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BudgetRepository", "User is not authenticated. Please log in.")
            return
        }

        val userBudgetRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("budget")

        userBudgetRef.setValue(budget)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete?.invoke()
                    Log.d("BudgetRepository", "Budget saved successfully.")
                } else {
                    Log.e("BudgetRepository", "Failed to save budget", task.exception)
                }
            }
    }


    fun getCategoryBudget(category: CategoryType, onDataReceived: (CategoryBudget?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BudgetRepository", "User not authenticated. Please log in.")
            return
        }

        val categoryRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("budget")
            .child("categoryBudgets")
            .child(category.name) // Suponiendo que `CategoryType` tiene un atributo `name`

        categoryRef.get().addOnSuccessListener { snapshot ->
            val categoryData = snapshot.getValue(CategoryBudget::class.java)
            onDataReceived(categoryData)
            Log.d("BudgetRepository", "Category data loaded successfully for $category.")
        }.addOnFailureListener { e ->
            Log.e("BudgetRepository", "Failed to load category data for $category", e)
            onDataReceived(null)  // Si falla, devolvemos null
        }
    }

    fun loadCategoryAlarmsFromFirebase(callback: (Map<CategoryType, Pair<Float, Boolean>>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BudgetRepository", "User not authenticated")
            return
        }

        val categoryRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("budget")
            .child("categoryBudgets")

        categoryRef.get().addOnSuccessListener { snapshot ->
            val alarmData = mutableMapOf<CategoryType, Pair<Float, Boolean>>()

            snapshot.children.forEach { childSnapshot ->
                val categoryName = childSnapshot.key
                val categoryData = childSnapshot.getValue(CategoryBudget::class.java)

                categoryData?.let {
                    val categoryType = CategoryType.valueOf(categoryName ?: "OTHER")
                    val alarmThreshold = it.alarmThreshold?.toFloat() ?: 0f
                    val alarmEnabled = it.alarmEnabled ?: false
                    alarmData[categoryType] = Pair(alarmThreshold, alarmEnabled)
                }
            }

            callback(alarmData)
        }.addOnFailureListener { e ->
            Log.e("BudgetRepository", "Failed to load category alarms", e)
        }
    }

}
