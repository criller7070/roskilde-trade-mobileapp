package dk.rosswap.mobile

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
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
                R.id.nav_login_required,
                R.id.action_nav_home_to_see_new_posts,
                R.id.nav_disliked,
                R.id.nav_item_detail
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController!!, appBarConfiguration)

        NavigationUI.setupWithNavController(navView, navController!!)

        // Add special-cases for drawer items that should reliably take the user to list destinations
        navView.setNavigationItemSelectedListener { item ->
            val handled = try {
                when (item.itemId) {
                    R.id.nav_home -> {
                        navController?.let { nc ->
                            if (nc.currentDestination?.id != R.id.nav_home) {
                                val popped = nc.popBackStack(R.id.nav_home, false)
                                if (!popped) {
                                    nc.navigate(R.id.nav_home)
                                } else {
                                    if (nc.currentDestination?.id != R.id.nav_home) {
                                        nc.navigate(R.id.nav_home)
                                    }
                                }
                            }
                        }
                        true
                    }

                    // Ensure 'New posts' always lands on the wall list (ItemListFragment)
                    R.id.nav_wall -> {
                        navController?.let { nc ->
                            // If wall exists in back stack, pop back to it
                            val popped = nc.popBackStack(R.id.nav_wall, false)
                            if (!popped) {
                                // If not, navigate to wall but clear any transient detail by popping up to graph start first
                                val navOptions = NavOptions.Builder()
                                    .setPopUpTo(nc.graph.startDestinationId, false)
                                    .build()
                                nc.navigate(R.id.nav_wall, null, navOptions)
                            }
                        }
                        true
                    }

                    // Ensure 'Liked posts' always lands on the liked list (LikedFragment)
                    R.id.nav_liked -> {
                        navController?.let { nc ->
                            val popped = nc.popBackStack(R.id.nav_liked, false)
                            if (!popped) {
                                val navOptions = NavOptions.Builder()
                                    .setPopUpTo(nc.graph.startDestinationId, false)
                                    .build()
                                nc.navigate(R.id.nav_liked, null, navOptions)
                            }
                        }
                        true
                    }

                    else -> NavigationUI.onNavDestinationSelected(item, navController!!)
                }
            } catch (_: IllegalArgumentException) {
                false
            }

            if (handled) drawerLayout.closeDrawer(GravityCompat.START)
            handled
        }

        // 🔑 LISTEN for login / logout changes
        authStateListener = FirebaseAuth.AuthStateListener {
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

        // Profile (only for logged in users)
        menu.findItem(R.id.nav_profile)?.isVisible = isLoggedIn

        // Liked posts & Messages should only be visible when logged in
        menu.findItem(R.id.nav_liked)?.isVisible = isLoggedIn
        menu.findItem(R.id.nav_chat_list)?.isVisible = isLoggedIn

        // Report Bugs should only be visible when logged in
        menu.findItem(R.id.nav_report_bug)?.isVisible = isLoggedIn
    }

    // =========================
    // TOP BAR MENU (LOGIN / LOGOUT)
    // =========================
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }


    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.action_logout)
        val isLoggedIn = firebaseAuth.currentUser != null

        item?.title = if (isLoggedIn) "LOG OUT" else "LOG IN"
        item?.isVisible = true

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                if (firebaseAuth.currentUser != null) {
                    LogoutDialogFragment()
                        .show(supportFragmentManager, "logout_dialog")
                } else {
                    navController?.navigate(R.id.nav_login)
                }
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
