package itson.appsmoviles.nest.ui.budget

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.budget.PercentageBudgetFragment
import itson.appsmoviles.nest.ui.budget.ValueBudgetFragment
import itson.appsmoviles.nest.ui.home.SharedMovementsViewModel
import kotlin.getValue

class BaseBudgetFragment : Fragment() {

    private lateinit var switchFormat: SwitchCompat
    private lateinit var txtValue: TextView
    private lateinit var txtPercentage: TextView
    private lateinit var budgetViewModel: BudgetViewModel

    private var valueBudgetFragmentInstance: ValueBudgetFragment? = null
    private var percentageBudgetFragmentInstance: PercentageBudgetFragment? = null

    private val sharedMovementsViewModel: SharedMovementsViewModel by activityViewModels()

    companion object {
        private const val VALUE_FRAGMENT_TAG = "VALUE_FRAGMENT_TAG"
        private const val PERCENTAGE_FRAGMENT_TAG = "PERCENTAGE_FRAGMENT_TAG"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_base_budget, container, false)

        switchFormat = view.findViewById(R.id.switchFormat)
        txtValue = view.findViewById(R.id.txtValue)
        txtPercentage = view.findViewById(R.id.txtPercentage)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        budgetViewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
                    // Pass the application and the sharedMovementsViewModel instance
                    return BudgetViewModel(requireActivity().application, sharedMovementsViewModel) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        })[BudgetViewModel::class.java]

        // Attempt to find existing fragments if BaseBudgetFragment is recreated (e.g., config change)
        if (savedInstanceState != null) {
            valueBudgetFragmentInstance = childFragmentManager.findFragmentByTag(VALUE_FRAGMENT_TAG) as? ValueBudgetFragment
            percentageBudgetFragmentInstance = childFragmentManager.findFragmentByTag(PERCENTAGE_FRAGMENT_TAG) as? PercentageBudgetFragment
        }
        // Or, if they are already in the FragmentManager from a previous state but not savedInstanceState
        if (valueBudgetFragmentInstance == null) {
            valueBudgetFragmentInstance = childFragmentManager.findFragmentByTag(VALUE_FRAGMENT_TAG) as? ValueBudgetFragment
        }
        if (percentageBudgetFragmentInstance == null) {
            percentageBudgetFragmentInstance = childFragmentManager.findFragmentByTag(PERCENTAGE_FRAGMENT_TAG) as? PercentageBudgetFragment
        }


        // Set initial state based on the switch's current checked status
        // This ensures the correct fragment is shown and colors are set on initial load or recreation
        updateFragmentVisibilityAndColors(switchFormat.isChecked, isInitialSetup = true)

        switchFormat.setOnCheckedChangeListener { _, isChecked ->
            updateFragmentVisibilityAndColors(isChecked)
        }
    }

    private fun updateFragmentVisibilityAndColors(showPercentage: Boolean, isInitialSetup: Boolean = false) {
        val transaction = childFragmentManager.beginTransaction()

        if (showPercentage) {
            // Configure UI for Percentage View
            setTextColor(txtValue, R.color.primary_color) // Set value text to "inactive" color
            Handler(Looper.getMainLooper()).postDelayed({
                setTextColor(txtPercentage, R.color.off_white) // Set percentage text to "active" color
            }, 50)

            // Manage PercentageBudgetFragment
            if (percentageBudgetFragmentInstance == null) {
                percentageBudgetFragmentInstance = PercentageBudgetFragment()
                transaction.add(R.id.fragmentBudgetContainer, percentageBudgetFragmentInstance!!, PERCENTAGE_FRAGMENT_TAG)
            } else {
                transaction.show(percentageBudgetFragmentInstance!!)
            }

            // Hide ValueBudgetFragment if it exists and is added
            valueBudgetFragmentInstance?.let {
                if (it.isAdded) {
                    transaction.hide(it)
                }
            }
        } else {
            // Configure UI for Value View
            setTextColor(txtPercentage, R.color.primary_color) // Set percentage text to "inactive" color
            Handler(Looper.getMainLooper()).postDelayed({
                setTextColor(txtValue, R.color.off_white) // Set value text to "active" color
            }, 50)

            // Manage ValueBudgetFragment
            if (valueBudgetFragmentInstance == null) {
                valueBudgetFragmentInstance = ValueBudgetFragment()
                transaction.add(R.id.fragmentBudgetContainer, valueBudgetFragmentInstance!!, VALUE_FRAGMENT_TAG)
            } else {
                transaction.show(valueBudgetFragmentInstance!!)
            }

            // Hide PercentageBudgetFragment if it exists and is added
            percentageBudgetFragmentInstance?.let {
                if (it.isAdded) {
                    transaction.hide(it)
                }
            }
        }

        // Commit transaction only if the fragment manager's state is not saved
        // This prevents "Can not perform this action after onSaveInstanceState"
        if (!childFragmentManager.isStateSaved) {
            transaction.commit()
        } else if (isInitialSetup) {
            // If state is saved during initial setup, try commitAllowingStateLoss
            // This can happen if onViewCreated is called as part of a state restoration
            // where the activity is already trying to save its state.
            // Use with caution, understand implications of state loss if not handled perfectly.
            transaction.commitAllowingStateLoss()
        }
    }

    private fun setTextColor(textView: TextView, colorResId: Int) {
        if (isAdded && context != null) { // Ensure fragment is attached and context is available
            textView.setTextColor(ContextCompat.getColor(requireContext(), colorResId))
        }
    }
}