package itson.appsmoviles.nest.ui.main

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import itson.appsmoviles.nest.R
import itson.appsmoviles.nest.ui.add.AddFragment
import itson.appsmoviles.nest.ui.add.expense.AddExpenseViewModel
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

// Verificar si se tiene el permiso para mostrar notificaciones en Android 13 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // Crear canal de notificaciones
        AddExpenseViewModel().createNotificationChannel(this)
        createNotificationChannel()
        testNotification()

        bottomNavigation = findViewById(R.id.bottomNavigation)

        initializeFragments()
        setupBottomNavigation()

        supportFragmentManager.addOnBackStackChangedListener {
            val addFragment = supportFragmentManager.findFragmentByTag(AddFragment.TAG)
            if (addFragment == null) {
                switchFragment(fragments.getValue(R.id.nav_home))
                bottomNavigation.selectedItemId = R.id.nav_home
            }
        }


    }

    private fun testNotification() {
        val notificationId = 1
        val notificationManager = getSystemService(NotificationManager::class.java)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "budget_channel")
            .setContentTitle("Test Notification")
            .setContentText("This is a test notification.")
            .setSmallIcon(R.drawable.alert_circle)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta para notificación emergente
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setTicker("New test notification!")  // Mostrar texto breve en la barra de estado
            .build()

        // Mostrar la notificación
        notificationManager.notify(notificationId, notification)


    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Crear canal de notificaciones con prioridad alta
            val channel = NotificationChannel(
                "budget_channel", // ID del canal
                "Budget Notifications", // Nombre del canal
                NotificationManager.IMPORTANCE_HIGH // Prioridad alta
            ).apply {
                description = "Notifications for budget thresholds" // Descripción del canal
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // Hacer visible en la pantalla de bloqueo
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel) // Crear el canal
        }
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
