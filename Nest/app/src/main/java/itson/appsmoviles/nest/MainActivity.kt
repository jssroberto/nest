package itson.appsmoviles.nest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import itson.appsmoviles.nest.presentation.ui.AddExpenseFragment
import itson.appsmoviles.nest.presentation.ui.BudgetFragment
import itson.appsmoviles.nest.presentation.ui.HomeFragment
import itson.appsmoviles.nest.presentation.ui.SettingsFragment
import itson.appsmoviles.nest.presentation.ui.TotalExpensesFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        replaceFragment(HomeFragment())

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_expenses -> replaceFragment(AddExpenseFragment())
                R.id.nav_profile -> replaceFragment(SettingsFragment())
                R.id.nav_search -> replaceFragment(BudgetFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

