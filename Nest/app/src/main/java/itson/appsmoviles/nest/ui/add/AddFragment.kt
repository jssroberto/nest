package itson.appsmoviles.nest.ui.add

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
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.add.income.AddIncomeFragment
import itson.appsmoviles.nest.ui.add.expense.AddExpenseFragment

class AddFragment : Fragment() {

    private lateinit var switchAdd: SwitchCompat
    private lateinit var txtIncome: TextView
    private lateinit var txtExpense: TextView

    companion object {
        const val TAG = "AddFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add, container, false)
        switchAdd = view.findViewById(R.id.switch_add)
        txtIncome = view.findViewById(R.id.txt_income)
        txtExpense = view.findViewById(R.id.txt_expense)

        replaceFragment(AddIncomeFragment())

        switchAdd.setOnCheckedChangeListener { _, isChecked ->
            toggleSwitchTextColors(isChecked)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_add_container, fragment)
            .commit()
    }

    private fun toggleSwitchTextColors(isChecked: Boolean) {
        if (isChecked) {
            setTextColor(txtIncome, R.color.primary_color)
            Handler(Looper.getMainLooper()).postDelayed({
                setTextColor(txtExpense, R.color.off_white)
            }, 50)
            replaceFragment(AddExpenseFragment())
        } else {
            setTextColor(txtExpense, R.color.primary_color)
            Handler(Looper.getMainLooper()).postDelayed({
                setTextColor(txtIncome, R.color.off_white)
            }, 50)
            replaceFragment(AddIncomeFragment())
        }
    }

    private fun setTextColor(textView: TextView, color: Int) {
        textView.setTextColor(ContextCompat.getColor(requireContext(), color))
    }
}