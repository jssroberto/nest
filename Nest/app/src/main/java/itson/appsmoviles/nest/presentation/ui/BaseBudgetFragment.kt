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
 * Use the [BaseBudgetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BaseBudgetFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var switchFormat: SwitchCompat
    private lateinit var txtValue: TextView
    private lateinit var txtPercentage: TextView

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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BaseBudgetFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BaseBudgetFragment().apply {
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