package itson.appsmoviles.nest.presentation.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import itson.appsmoviles.nest.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var switchAdd: SwitchCompat
    private lateinit var txtIncome: TextView
    private lateinit var txtExpense: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
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
            when {
                isChecked -> {
                        txtIncome.setTextColor(ContextCompat.getColor(requireContext(),R.color.primary_color))
                    Handler(Looper.getMainLooper()).postDelayed({
                        txtExpense.setTextColor(ContextCompat.getColor(requireContext(),R.color.background))
                    }, 50)
                    replaceFragment(AddExpenseFragment())
                }
                else -> {
                        txtExpense.setTextColor(ContextCompat.getColor(requireContext(),R.color.primary_color))
                    Handler(Looper.getMainLooper()).postDelayed({
                    txtIncome.setTextColor(ContextCompat.getColor(requireContext(),R.color.background))
                    }, 50)
                    replaceFragment(AddIncomeFragment())
                }
            }
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_add_container, fragment)
            .commit()
    }
}