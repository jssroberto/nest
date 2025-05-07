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
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.budget.PercentageBudgetFragment
import itson.appsmoviles.nest.ui.budget.ValueBudgetFragment

/**
 * A simple [androidx.fragment.app.Fragment] subclass.
 * Use the [BaseBudgetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BaseBudgetFragment : Fragment() {


    private lateinit var switchFormat: SwitchCompat
    private lateinit var txtValue: TextView
    private lateinit var txtPercentage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_base_budget, container, false)
        switchFormat = view.findViewById(R.id.switchFormat)
        txtValue = view.findViewById(R.id.txtValue)
        txtPercentage = view.findViewById(R.id.txtPercentage)

        replaceFragment(ValueBudgetFragment())

        switchFormat.setOnCheckedChangeListener { _, isChecked ->
            toggleSwitchTextColors(isChecked)
        }

        return view

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.fragmentBudgetContainer, fragment)
            .commit()
    }

    private fun toggleSwitchTextColors(isChecked: Boolean) {
        if (isChecked) {
            setTextColor(txtValue, R.color.primary_color)
            Handler(Looper.getMainLooper()).postDelayed({
                setTextColor(txtPercentage, R.color.off_white)
            }, 50)
            replaceFragment(PercentageBudgetFragment())
        } else {
            setTextColor(txtPercentage, R.color.primary_color)
            Handler(Looper.getMainLooper()).postDelayed({
                setTextColor(txtValue, R.color.off_white)
            }, 50)
            replaceFragment(ValueBudgetFragment())
        }
    }

    private fun setTextColor(textView: TextView, color: Int) {
        textView.setTextColor(ContextCompat.getColor(requireContext(), color))
    }
}