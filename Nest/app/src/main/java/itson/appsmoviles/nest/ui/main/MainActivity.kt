package itson.appsmoviles.nest.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.budget.BaseBudgetFragment
import itson.appsmoviles.nest.ui.home.HomeFragment
import itson.appsmoviles.nest.ui.profile.ProfileFragment
import itson.appsmoviles.nest.ui.expenses.ExpensesFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private val homeFragment = HomeFragment()
    private val expensesFragment = ExpensesFragment()
    private val profileFragment = ProfileFragment()
    private val searchFragment = BaseBudgetFragment()

    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)


        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, searchFragment, "4").hide(searchFragment)
            .add(R.id.fragment_container, profileFragment, "3").hide(profileFragment)
            .add(R.id.fragment_container, expensesFragment, "2").hide(expensesFragment)
            .add(R.id.fragment_container, homeFragment, "1") // este queda visible
            .commit()

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(homeFragment)
                R.id.nav_expenses -> switchFragment(expensesFragment)
                R.id.nav_profile -> switchFragment(profileFragment)
                R.id.nav_search -> switchFragment(searchFragment)
            }
            true
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (targetFragment != activeFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(targetFragment)
                .commit()
            activeFragment = targetFragment
        }
    }
}