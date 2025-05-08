package itson.appsmoviles.nest.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Budget
import itson.appsmoviles.nest.data.model.CategoryBudget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class BudgetRepository {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance()
        .getReference("users")
        .child(userId ?: "unknown")

    // Flows para manejar las alarmas y umbrales
    private val alarmThresholdMapFlow = MutableStateFlow<Map<CategoryType, Float>>(emptyMap())
    private val alarmEnabledMapFlow = MutableStateFlow<Map<CategoryType, Boolean>>(emptyMap())

    // Métodos para obtener los Flows de alarmas
    fun getAlarmThresholds(): StateFlow<Map<CategoryType, Float>> = alarmThresholdMapFlow
    fun getAlarmEnabled(): StateFlow<Map<CategoryType, Boolean>> = alarmEnabledMapFlow

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

        // Crear el objeto CategoryBudget con los datos de la categoría y alarmas
        val categoryData = CategoryBudget(
            categoryBudget = categoryBudget,
            alarmThreshold = alarmThreshold,
            alarmEnabled = alarmEnabled
        )

        // Actualizar los datos de la categoría en Firebase
        categoryRef.setValue(categoryData)
            .addOnSuccessListener {
                Log.d("BudgetRepository", "Category budget updated for $category")
            }
            .addOnFailureListener { e ->
                Log.e("BudgetRepository", "Failed to update category budget", e)
            }
    }

    // Método para actualizar el umbral de la alarma y habilitar la alarma en Firebase
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

        // Obtener la referencia a la categoría
        val categoryRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("budget")
            .child("categoryBudgets")
            .child(category.name) // Asumiendo que `CategoryType` tiene un atributo `name`

        // Crear el objeto CategoryBudget con los valores de la categoría y alarmas
        val categoryData = CategoryBudget(
            categoryBudget = categoryBudget,
            alarmThreshold = alarmThreshold,
            alarmEnabled = alarmEnabled
        )

        // Actualizar los datos de la categoría en Firebase
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

    suspend fun getBudgetDataSuspend(): Budget? = suspendCoroutine { continuation ->
        dbRef.child("budget").get()
            .addOnSuccessListener { snapshot ->
                val budget = snapshot.getValue(Budget::class.java)
                continuation.resume(budget)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    fun getTotalBudget(onDataReceived: (Float) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("BudgetRepository", "User not authenticated. Please log in.")
            return
        }

        val userBudgetRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("budget")

        userBudgetRef.get().addOnSuccessListener { snapshot ->
            val budget = snapshot.getValue(Budget::class.java)
            if (budget != null) {
                // Si el presupuesto total está almacenado directamente, lo obtenemos
                val totalBudget = budget.totalBudget
                onDataReceived(totalBudget)
                Log.d("BudgetRepository", "Total budget loaded successfully.")
            } else {
                Log.e("BudgetRepository", "No budget data found")
            }
        }.addOnFailureListener { e ->
            Log.e("BudgetRepository", "Failed to load total budget data", e)
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

            // Llamamos al callback para pasar los datos obtenidos
            callback(alarmData)
        }.addOnFailureListener { e ->
            Log.e("BudgetRepository", "Failed to load category alarms", e)
        }
    }

    fun persistAlarmThreshold(
        category: CategoryType,
        threshold: Double,
        isEnabled: Boolean
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

        // Primero obtenemos el presupuesto actual para no sobrescribirlo
        categoryRef.get().addOnSuccessListener { snapshot ->
            val currentData = snapshot.getValue(CategoryBudget::class.java)
            val categoryBudget = currentData?.categoryBudget ?: 0f

            val updatedData = CategoryBudget(
                categoryBudget = categoryBudget,
                alarmThreshold = threshold,
                alarmEnabled = isEnabled
            )

            categoryRef.setValue(updatedData)
                .addOnSuccessListener {
                    Log.d("BudgetRepository", "Alarm threshold persisted for $category")
                }
                .addOnFailureListener { e ->
                    Log.e("BudgetRepository", "Failed to persist alarm threshold", e)
                }
        }.addOnFailureListener { e ->
            Log.e("BudgetRepository", "Failed to fetch current category budget", e)
        }
    }


}
