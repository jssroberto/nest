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

                    return BudgetViewModel(requireActivity().application, sharedMovementsViewModel) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        })[BudgetViewModel::class.java]


        if (savedInstanceState != null) {
            valueBudgetFragmentInstance = childFragmentManager.findFragmentByTag(VALUE_FRAGMENT_TAG) as? ValueBudgetFragment
            percentageBudgetFragmentInstance = childFragmentManager.findFragmentByTag(PERCENTAGE_FRAGMENT_TAG) as? PercentageBudgetFragment
        }

        if (valueBudgetFragmentInstance == null) {
            valueBudgetFragmentInstance = childFragmentManager.findFragmentByTag(VALUE_FRAGMENT_TAG) as? ValueBudgetFragment
        }
        if (percentageBudgetFragmentInstance == null) {
            percentageBudgetFragmentInstance = childFragmentManager.findFragmentByTag(PERCENTAGE_FRAGMENT_TAG) as? PercentageBudgetFragment
        }



        updateFragmentVisibilityAndColors(switchFormat.isChecked, isInitialSetup = true)

        switchFormat.setOnCheckedChangeListener { _, isChecked ->
            updateFragmentVisibilityAndColors(isChecked)
        }
    }

    private fun updateFragmentVisibilityAndColors(showPercentage: Boolean, isInitialSetup: Boolean = false) {
        val transaction = childFragmentManager.beginTransaction()

        if (showPercentage) {

            setTextColor(txtValue, R.color.primary_color)
            Handler(Looper.getMainLooper()).postDelayed({
                setTextColor(txtPercentage, R.color.off_white)
            }, 50)


            if (percentageBudgetFragmentInstance == null) {
                percentageBudgetFragmentInstance = PercentageBudgetFragment()
                transaction.add(R.id.fragmentBudgetContainer, percentageBudgetFragmentInstance!!, PERCENTAGE_FRAGMENT_TAG)
            } else {
                transaction.show(percentageBudgetFragmentInstance!!)
            }


            valueBudgetFragmentInstance?.let {
                if (it.isAdded) {
                    transaction.hide(it)
                }
            }
        } else {

            setTextColor(txtPercentage, R.color.primary_color)
            Handler(Looper.getMainLooper()).postDelayed({
                setTextColor(txtValue, R.color.off_white)
            }, 50)


            if (valueBudgetFragmentInstance == null) {
                valueBudgetFragmentInstance = ValueBudgetFragment()
                transaction.add(R.id.fragmentBudgetContainer, valueBudgetFragmentInstance!!, VALUE_FRAGMENT_TAG)
            } else {
                transaction.show(valueBudgetFragmentInstance!!)
            }


            percentageBudgetFragmentInstance?.let {
                if (it.isAdded) {
                    transaction.hide(it)
                }
            }
        }


        if (!childFragmentManager.isStateSaved) {
            transaction.commit()
        } else if (isInitialSetup) {

            transaction.commitAllowingStateLoss()
        }
    }

    private fun setTextColor(textView: TextView, colorResId: Int) {
        if (isAdded && context != null) {
            textView.setTextColor(ContextCompat.getColor(requireContext(), colorResId))
        }
    }
}