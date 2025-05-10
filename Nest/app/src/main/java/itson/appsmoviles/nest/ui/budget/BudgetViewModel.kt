package itson.appsmoviles.nest.ui.budget

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.data.enum.CategoryType
import itson.appsmoviles.nest.data.model.Budget
import itson.appsmoviles.nest.data.model.CategoryBudget
import itson.appsmoviles.nest.data.repository.BudgetRepository
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import itson.appsmoviles.nest.ui.main.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BudgetViewModel(
    application: Application,
    private val sharedMovementsViewModel: SharedMovementsViewModel
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    private val repository = BudgetRepository()
    val totalBudget = MutableLiveData<Float>()
    val categoryBudgets = MutableLiveData<Map<CategoryType, Float>>()

    val alarmThresholdMap = mutableMapOf<CategoryType, Float>()
    val alarmEnabledMap = mutableMapOf<CategoryType, Boolean>()
    private val _alarmThresholds = MutableStateFlow<Map<CategoryType, Float>>(emptyMap())
    val alarmThresholds: StateFlow<Map<CategoryType, Float>> = _alarmThresholds

    private val _alarmEnabled = MutableStateFlow<Map<CategoryType, Boolean>>(emptyMap())
    val alarmEnabled: StateFlow<Map<CategoryType, Boolean>> = _alarmEnabled
    private val _categoryPercentages = MediatorLiveData<Map<CategoryType, Float>>()
    val categoryPercentages: LiveData<Map<CategoryType, Float>> = _categoryPercentages

    private val _categoryAmounts = MutableLiveData<Map<CategoryType, Float>>(emptyMap())
    val categoryAmounts: LiveData<Map<CategoryType, Float>> = _categoryAmounts

    init {
        loadBudgetData()
        observeAlarmThresholds()
        observeAlarmEnabled()
        observeBudgetChanges()
    }


    private fun observeBudgetChanges() {
        _categoryPercentages.addSource(totalBudget) {
            recalculatePercentages()
        }
        _categoryPercentages.addSource(categoryBudgets) {
            recalculatePercentages()
        }
    }

    private fun recalculatePercentages() {
        val total = totalBudget.value ?: 0f
        val categories = categoryBudgets.value ?: emptyMap()
        _categoryPercentages.value = if (total > 0f) {
            categories.mapValues { it.value * 100f / total }
        } else {
            categories.mapValues { 0f }
        }
    }


    fun setCategoryBudget(
        category: CategoryType,
        amount: Float,
        alarmThreshold: Float? = null,
        alarmEnabled: Boolean? = null
    ) {

        val updatedMap = categoryBudgets.value?.toMutableMap() ?: mutableMapOf()
        updatedMap[category] = amount
        categoryBudgets.value = updatedMap


        val total = totalBudget.value ?: 0f
        _categoryPercentages.value = if (total > 0f) {
            updatedMap.mapValues { it.value * 100f / total }
        } else {
            updatedMap.mapValues { 0f }
        }


        val threshold = alarmThreshold ?: alarmThresholdMap[category]
        val enabled = alarmEnabled ?: alarmEnabledMap[category] ?: false

        if (threshold != null) alarmThresholdMap[category] = threshold
        alarmEnabledMap[category] = enabled


        repository.updateCategoryBudgetAmount(
            category,
            amount,
            threshold?.toDouble(),
            enabled
        )

        sharedMovementsViewModel.notifyBudgetDataChanged()
    }



    private fun observeAlarmThresholds() {
        viewModelScope.launch {
            alarmThresholds.collect { thresholds ->
                alarmThresholdMap.clear()
                alarmThresholdMap.putAll(thresholds)

            }
        }
    }

    private fun observeAlarmEnabled() {
        viewModelScope.launch {
            alarmEnabled.collect { enabledMap ->
                alarmEnabledMap.clear()
                alarmEnabledMap.putAll(enabledMap)

            }
        }
    }

    private fun loadBudgetData() {
        viewModelScope.launch {
            val budget = repository.getBudgetDataSuspend()
            budget?.let {
                totalBudget.value = it.totalBudget

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
                _alarmThresholds.value = alarmThresholdsMap
                _alarmEnabled.value = alarmEnabledMapLocal
            } ?: run {
                totalBudget.value = 0f
                categoryBudgets.value = CategoryType.values()
                    .associateWith { 0f }
                _alarmThresholds.value = CategoryType.values().associateWith { 0f }
                _alarmEnabled.value = CategoryType.values().associateWith { false }
            }
        }
    }


    fun setAlarmThreshold(category: CategoryType, threshold: Float) {
        val updatedThresholdMap = _alarmThresholds.value.toMutableMap()
        updatedThresholdMap[category] = threshold
        _alarmThresholds.value = updatedThresholdMap


        alarmThresholdMap[category] = threshold
    }

    fun setAlarmEnabled(category: CategoryType, enabled: Boolean) {
        val updatedEnabledMap = _alarmEnabled.value.toMutableMap()
        updatedEnabledMap[category] = enabled
        _alarmEnabled.value = updatedEnabledMap

        alarmEnabledMap[category] = enabled
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
                sharedMovementsViewModel.notifyBudgetDataChanged()
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

            if (_alarmThresholds.value != thresholdMap) {
                _alarmThresholds.value = thresholdMap
            }
            if (_alarmEnabled.value != enabledMap) {
                _alarmEnabled.value = enabledMap
            }
            alarmThresholdMap.clear()
            alarmThresholdMap.putAll(thresholdMap)
            alarmEnabledMap.clear()
            alarmEnabledMap.putAll(enabledMap)
        }
    }

    fun checkAndNotifyIfThresholdExceeded(category: CategoryType, newExpense: Float): Boolean {
        val threshold = alarmThresholdMap[category] ?: return false
        val isAlarmEnabled = alarmEnabledMap[category] ?: false
        Log.d(
            "BudgetViewModel",
            "Checking category: $category, Threshold: $threshold, New Expense: $newExpense"
        )

        if (!isAlarmEnabled) {
            Log.d("BudgetViewModel", "Alarm not enabled for category: $category")
            return false
        } else {

            val currentSpent = categoryBudgets.value?.get(category) ?: 0f
            val newTotal = currentSpent + newExpense
            Log.d("BudgetViewModel", "Current spent: $currentSpent, New total: $newTotal")

            if (newTotal > threshold) {
                Log.d("BudgetViewModel", "Threshold exceeded for category: $category")
                sendThresholdExceededNotification(category)
                return true
            }
        }

        Log.d("BudgetViewModel", "No threshold exceeded for category: $category")
        return false
    }


    private fun sendThresholdExceededNotification(category: CategoryType) {

        val notificationId = 1
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "budget_channel")
            .setContentTitle("Budget Exceeded")
            .setContentText("Your budget for $category has been exceeded!")
            .setSmallIcon(R.drawable.alert_circle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)

        Log.d("BudgetViewModel", "Notification sent successfully for category: $category")
    }



}