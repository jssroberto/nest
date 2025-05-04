package itson.appsmoviles.nest.ui.main

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.add.AddFragment
import itson.appsmoviles.nest.ui.budget.BaseBudgetFragment
import itson.appsmoviles.nest.ui.expenses.ExpensesFragment
import itson.appsmoviles.nest.ui.home.HomeFragment
import itson.appsmoviles.nest.ui.profile.ProfileFragment

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private val fragments = mapOf(
        R.id.nav_home to HomeFragment(),
        R.id.nav_expenses to ExpensesFragment(),
        R.id.nav_profile to ProfileFragment(),
        R.id.nav_search to BaseBudgetFragment()
    )

    private var activeFragment: Fragment = fragments.getValue(R.id.nav_home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        initializeFragments()
        setupBottomNavigation()
    }

    private fun initializeFragments() {
        val transaction = supportFragmentManager.beginTransaction()

        fragments.forEach { (id, fragment) ->
            transaction.add(R.id.fragment_container, fragment, id.toString())
                .apply { if (fragment != activeFragment) hide(fragment) }
        }

        transaction.commit()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            fragments[item.itemId]?.let { switchFragment(it) }
            true
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        supportFragmentManager.findFragmentByTag(AddFragment.TAG)?.takeIf { it.isVisible }?.let {
            transaction.remove(it)
        }

        if (targetFragment != activeFragment) {
            transaction.hide(activeFragment).show(targetFragment)
            activeFragment = targetFragment
        }

        transaction.commit()
    }

    fun showAddFragment() {
        val existingAddFragment = supportFragmentManager.findFragmentByTag(AddFragment.TAG)
        if (existingAddFragment?.isVisible == true) return

        val newFragment = AddFragment()
        val transaction = supportFragmentManager.beginTransaction()

        transaction.hide(activeFragment)
        transaction.add(R.id.fragment_container, newFragment, AddFragment.TAG)
            .addToBackStack(AddFragment.TAG)
            .commit()

        activeFragment = newFragment
    }
}
