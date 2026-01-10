package dk.rosswap.mobile

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.core.ui.components.popup.PopupHost
import dk.rosswap.mobile.databinding.ActivityMainBinding
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    @Inject lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Install centralized, lifecycle-aware popup handling
        PopupHost.install(this)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab)
                .show()
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navHostFragment = (supportFragmentManager.primaryNavigationFragment as? NavHostFragment)
            ?: supportFragmentManager.fragments.filterIsInstance<NavHostFragment>().firstOrNull()
        val navController = navHostFragment?.navController
            ?: throw IllegalStateException("NavHostFragment not found")

        // Dynamically pick start destination based on auth state.
        // This prevents the app from being stuck on the login-required screen.
        val graph = navController.navInflater.inflate(R.navigation.mobile_navigation)
        val isLoggedIn = firebaseAuth.currentUser != null
        val rootDestinationId = if (isLoggedIn) R.id.nav_home else R.id.nav_login_required
        graph.setStartDestination(rootDestinationId)
        navController.graph = graph

        // Only include IDs that actually exist in the current nav graph.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_login,
                R.id.nav_createpost,
                R.id.nav_chat_list,
                R.id.nav_liked,
                R.id.nav_wall,
                R.id.nav_create_account,
                R.id.nav_report_bug,
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Bit of a patchwork solution but NavController's built-in handling of
        // NavigationView has some issues with pressing "back" from different views

        navView.setNavigationItemSelectedListener { item ->
            val handled = try {
                val isTopLevel = appBarConfiguration.topLevelDestinations.contains(item.itemId)
                val navOptions = if (isTopLevel) {
                    NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        // Use a stable root destination for popUpTo (dynamic start destinations break back stack otherwise)
                        .setPopUpTo(rootDestinationId, inclusive = false, saveState = true)
                        .build()
                } else {
                    NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build()
                }

                navController.navigate(item.itemId, null, navOptions)
                true
            } catch (_: IllegalArgumentException) {
                false
            }

            if (handled) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            handled
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = (supportFragmentManager.primaryNavigationFragment as? NavHostFragment)
            ?: supportFragmentManager.fragments.filterIsInstance<NavHostFragment>().firstOrNull()
        val navController = navHostFragment?.navController
            ?: throw IllegalStateException("NavHostFragment not found")

        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
