package dk.rosswap.mobile

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.core.ui.components.popup.PopupHost
import dk.rosswap.mobile.databinding.ActivityMainBinding
import javax.inject.Inject
import dk.rosswap.mobile.LogoutDialogFragment

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var navController: NavController? = null

    @Inject lateinit var firebaseAuth: FirebaseAuth

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PopupHost.install(this)
        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .show()
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navHostFragment =
            (supportFragmentManager.primaryNavigationFragment as? NavHostFragment)
                ?: supportFragmentManager.fragments.filterIsInstance<NavHostFragment>().firstOrNull()
                ?: throw IllegalStateException("NavHostFragment not found")

        navController = navHostFragment.navController

        // Pick start destination based on auth state
        val graph = navController!!.navInflater.inflate(R.navigation.mobile_navigation)
        val isLoggedIn = firebaseAuth.currentUser != null
        val rootDestinationId =
            if (isLoggedIn) R.id.nav_home else R.id.nav_login_required
        graph.setStartDestination(rootDestinationId)
        navController!!.graph = graph

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_login,
                R.id.nav_profile,
                R.id.nav_swipe,
                R.id.nav_createpost,
                R.id.nav_chat_list,
                R.id.nav_liked,
                R.id.nav_wall,
                R.id.nav_create_account,
                R.id.nav_report_bug,
                R.id.nav_privacy_policy,
                R.id.nav_about_us,
                R.id.nav_login_required
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController!!, appBarConfiguration)

        // Use NavigationUI to wire the NavigationView to the NavController. This ensures
        // proper handling of menu item -> destination resolution and checked states.
        NavigationUI.setupWithNavController(navView, navController!!)

        // Special-case the Home drawer item so it always returns to the true Home destination
        // instead of potentially navigating to a destination that became current via a
        // Home-screen button action. For all other menu items, delegate to NavigationUI so
        // default behavior (including restoreState) is preserved.
        navView.setNavigationItemSelectedListener { item ->
            val handled = try {
                if (item.itemId == R.id.nav_home) {
                    // Always pop back to the explicit home destination (do not change startDestination here)
                    navController!!.popBackStack(R.id.nav_home, false)
                    true
                } else {
                    NavigationUI.onNavDestinationSelected(item, navController!!)
                }
            } catch (_: IllegalArgumentException) {
                false
            }

            if (handled) drawerLayout.closeDrawer(GravityCompat.START)
            handled
        }

        // 🔑 LISTEN for login / logout changes
        authStateListener = FirebaseAuth.AuthStateListener {
            // Only update UI if Activity is in valid state (at least RESUMED)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                updateDrawerMenu()
                invalidateOptionsMenu()
            }
        }

        // Initial state
        updateDrawerMenu()
    }

    override fun onStart() {
        super.onStart()
        authStateListener?.let { firebaseAuth.addAuthStateListener(it) }
    }

    override fun onStop() {
        super.onStop()
        authStateListener?.let { firebaseAuth.removeAuthStateListener(it) }
    }

    // =========================
    // DRAWER MENU VISIBILITY
    // =========================
    private fun updateDrawerMenu() {
        val menu = binding.navView.menu
        val isLoggedIn = firebaseAuth.currentUser != null

        // Login / Create account
        menu.findItem(R.id.nav_login)?.isVisible = !isLoggedIn
        menu.findItem(R.id.nav_create_account)?.isVisible = !isLoggedIn

        // Profile
        menu.findItem(R.id.nav_profile)?.isVisible = isLoggedIn
    }

    // =========================
    // TOP BAR MENU (LOGOUT)
    // =========================
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menuInflater.inflate(R.menu.main_menu, menu)

        // Logout only when logged in
        menu.findItem(R.id.action_logout)?.isVisible =
            firebaseAuth.currentUser != null

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                LogoutDialogFragment()
                    .show(supportFragmentManager, "logout_dialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            (supportFragmentManager.primaryNavigationFragment as? NavHostFragment)
                ?: supportFragmentManager.fragments.filterIsInstance<NavHostFragment>().firstOrNull()
                ?: throw IllegalStateException("NavHostFragment not found")

        return navHostFragment.navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
