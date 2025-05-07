package itson.appsmoviles.nest.ui.budget

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Budget
import itson.appsmoviles.nest.data.model.CategoryBudget
import itson.appsmoviles.nest.data.repository.BudgetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BudgetViewModel : ViewModel() {

    private val repository = BudgetRepository()
    val totalBudget = MutableLiveData<Float>()
    val categoryBudgets = MutableLiveData<Map<CategoryType, Float>>()

    val alarmThresholdMap = mutableMapOf<CategoryType, Float>()
    val alarmEnabledMap = mutableMapOf<CategoryType, Boolean>()
    private val _alarmThresholds = MutableStateFlow<Map<CategoryType, Float>>(emptyMap())
    val alarmThresholds: StateFlow<Map<CategoryType, Float>> = _alarmThresholds

    private val _alarmEnabled = MutableStateFlow<Map<CategoryType, Boolean>>(emptyMap())
    val alarmEnabled: StateFlow<Map<CategoryType, Boolean>> = _alarmEnabled


    init {
        loadBudgetData()
        observeAlarmThresholds()
        observeAlarmEnabled()
    }

    // Método para observar los Flows de umbrales de alarma
    private fun observeAlarmThresholds() {
        viewModelScope.launch {
            alarmThresholds.collect { thresholds ->
                alarmThresholdMap.clear()
                alarmThresholdMap.putAll(thresholds)
                // Puedes actualizar LiveData si es necesario
            }
        }
    }

    private fun observeAlarmEnabled() {
        viewModelScope.launch {
            alarmEnabled.collect { enabledMap ->
                alarmEnabledMap.clear()
                alarmEnabledMap.putAll(enabledMap)
                // Puedes actualizar LiveData si es necesario
            }
        }
    }

    private fun loadBudgetData() {
        viewModelScope.launch {
            val budget = repository.getBudgetDataSuspend()
            budget?.let {
                totalBudget.value = it.totalBudget

                // Mapear los presupuestos de las categorías desde CategoryBudget
                val categoryMap = mutableMapOf<CategoryType, Float>()
                val alarmThresholdsMap = mutableMapOf<CategoryType, Float>()
                val alarmEnabledMapLocal = mutableMapOf<CategoryType, Boolean>()

                it.categoryBudgets.forEach { (categoryName, categoryBudgetObj) ->
                    val category = try {
                        CategoryType.valueOf(categoryName)
                    } catch (e: IllegalArgumentException) {
                        CategoryType.OTHER
                    }
                    categoryMap[category] = categoryBudgetObj.categoryBudget
                    categoryBudgetObj.alarmThreshold?.let {
                        alarmThresholdsMap[category] = it.toFloat()
                    }
                    alarmEnabledMapLocal[category] = categoryBudgetObj.alarmEnabled
                }

                categoryBudgets.value = categoryMap
                alarmThresholdMap.clear()
                alarmThresholdMap.putAll(alarmThresholdsMap)
                alarmEnabledMap.clear()
                alarmEnabledMap.putAll(alarmEnabledMapLocal)
            }
        }
    }

    // Método para actualizar el presupuesto de una categoría
    fun setCategoryBudget(
        category: CategoryType,
        amount: Float,
        alarmThreshold: Float? = null,
        alarmEnabled: Boolean = false
    ) {
        // Actualizamos el presupuesto de la categoría en el LiveData
        categoryBudgets.value = categoryBudgets.value?.toMutableMap()?.apply {
            put(category, amount)
        }

        // Llamada al repositorio para actualizar el presupuesto y las configuraciones de alarma
        repository.updateCategoryBudgetAmount(category, amount, alarmThreshold?.toDouble(), alarmEnabled)
    }

    // Método para actualizar el umbral de la alarma de una categoría
    fun setAlarmThreshold(category: CategoryType, threshold: Float) {
        alarmThresholdMap[category] = threshold
        repository.updateCategoryAlarmThreshold(
            category,
            categoryBudgets.value?.get(category) ?: 0f,
            threshold.toDouble(),
            alarmEnabledMap[category] ?: false
        )
    }

    // Método para habilitar/deshabilitar la alarma de una categoría
    fun setAlarmEnabled(category: CategoryType, enabled: Boolean) {
        alarmEnabledMap[category] = enabled
        repository.updateCategoryAlarmThreshold(
            category,
            categoryBudgets.value?.get(category) ?: 0f,
            alarmThresholdMap[category]?.toDouble(),
            enabled
        )
    }

    // Método para actualizar el presupuesto total
    fun setTotalBudget(amount: Float) {
        if (totalBudget.value != amount) {
            totalBudget.value = amount
            val updatedBudget = Budget(
                totalBudget = amount,
                categoryBudgets = buildSimpleCategoryBudgets()
            )
            repository.saveBudget(updatedBudget) {
                Log.d("BudgetViewModel", "Total budget saved successfully.")
            }
        }
    }
    private fun buildSimpleCategoryBudgets(): Map<String, CategoryBudget> {
        val result = mutableMapOf<String, CategoryBudget>()
        categoryBudgets.value?.forEach { (category, amount) ->
            val threshold = alarmThresholdMap[category]
            val isEnabled = alarmEnabledMap[category] ?: false
            result[category.name] = CategoryBudget(
                categoryBudget = amount,
                alarmThreshold = threshold?.toDouble(),
                alarmEnabled = isEnabled
            )
        }
        return result
    }

    // Método para verificar si el umbral de la categoría se ha superado
    fun checkAndNotifyIfThresholdExceeded(category: CategoryType, newExpense: Float): Boolean {
        val threshold = alarmThresholdMap[category] ?: return false
        val isAlarmEnabled = alarmEnabledMap[category] ?: false
        if (!isAlarmEnabled) return false

        val currentSpent = categoryBudgets.value?.get(category) ?: 0f
        val newTotal = currentSpent + newExpense
        return newTotal > threshold
    }

    fun persistAlarmThreshold(category: CategoryType, threshold: Double, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.getCategoryBudget(category) { categoryBudget ->
                val budgetAmount = categoryBudget?.categoryBudget ?: 0f
                repository.updateCategoryAlarmThreshold(
                    category = category,
                    categoryBudget = budgetAmount,
                    alarmThreshold = threshold,
                    alarmEnabled = isEnabled
                )
            }
        }
    }

    fun loadCategoryAlarms() {
        repository.loadCategoryAlarmsFromFirebase { alarmData ->
            val thresholdMap = alarmData.mapValues { it.value.first }
            val enabledMap = alarmData.mapValues { it.value.second }

            _alarmThresholds.value = thresholdMap
            _alarmEnabled.value = enabledMap
        }
    }



}
