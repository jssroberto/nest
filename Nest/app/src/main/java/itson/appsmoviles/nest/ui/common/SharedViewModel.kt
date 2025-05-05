package itson.appsmoviles.nest.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class SharedViewModel : ViewModel() {
    private val _movementsUpdated = MutableLiveData<Unit>()
    val movementsUpdated: LiveData<Unit> = _movementsUpdated

    fun notifyMovementsUpdated() {
        _movementsUpdated.value = Unit
    }
}