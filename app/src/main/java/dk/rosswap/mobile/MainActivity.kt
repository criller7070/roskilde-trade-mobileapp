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
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.core.ui.components.popup.PopupHost
import dk.rosswap.mobile.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

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

        // Custom navigation handling to work around back stack issues with NavigationView.
        // Issue: Using the standard navView.setupWithNavController(navController) causes
        // inconsistent back stack behavior when navigating between top-level destinations.
        // The framework doesn't properly restore state or manage the back stack, causing
        // the back button to behave unexpectedly (e.g., skipping screens or not returning
        // to the correct previous destination).
        //
        // This custom implementation explicitly controls NavOptions to ensure:
        // - Top-level destinations use setPopUpTo() to prevent back stack buildup
        // - State is properly saved and restored using setRestoreState() and saveState
        // - Single instance behavior via setLaunchSingleTop()
        //
        // TODO: Monitor https://issuetracker.google.com/issues?q=componentid:409828
        // for fixes to NavigationView back stack handling, then consider reverting to
        // the standard setupWithNavController() approach.

        navView.setNavigationItemSelectedListener { item ->
            val handled = try {
                val topLevel = appBarConfiguration.topLevelDestinations.contains(item.itemId)
                val navOptions = if (topLevel) {
                    NavOptions.Builder()    // Navigate to top-level
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(navController.graph.startDestinationId, inclusive = false, saveState = true)
                        .build()
                } else {
                    NavOptions.Builder()    // Navigate to non-top-level
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
