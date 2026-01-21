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
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.core.ui.components.popup.PopupHost
import dk.rosswap.mobile.databinding.ActivityMainBinding
import dk.rosswap.mobile.feature.auth.presentation.LogoutDialogFragment
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var navController: NavController? = null
    @Inject lateinit var firebaseAuth: FirebaseAuth
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private val authRequiredDestinations = setOf(
        R.id.nav_profile,
        R.id.nav_liked,
        R.id.nav_chat_list,
        R.id.nav_report_bug
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // set up...
        super.onCreate(savedInstanceState)

        // ...binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ...popup
        PopupHost.install(this)
        setSupportActionBar(binding.appBarMain.toolbar)

        // ...app bar
        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.fab)
                .show()
        }

        // ...drawer
        val drawerLayout: DrawerLayout = binding.drawerLayout

        // ... and a whole lot of nav stuff
        val navView: NavigationView = binding.navView
        val navHostFragment =
            (supportFragmentManager.primaryNavigationFragment as? NavHostFragment)
                ?: supportFragmentManager.fragments.filterIsInstance<NavHostFragment>().firstOrNull()
                ?: throw IllegalStateException("NavHostFragment not found")

        navController = navHostFragment.navController

        // ...graph
        val graph = navController!!.navInflater.inflate(R.navigation.mobile_navigation)
        graph.setStartDestination(R.id.nav_home)
        navController!!.graph = graph

        navController!!.addOnDestinationChangedListener { controller, destination, _ ->
            val isLoggedIn = firebaseAuth.currentUser != null
            if (!isLoggedIn && destination.id in authRequiredDestinations) {
                val options = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(destination.id, true)
                    .build()
                controller.navigate(R.id.nav_login_required, null, options)
            }

            if (destination.id == R.id.nav_chatconvo) {
                binding.appBarMain.toolbar.navigationIcon = null
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.setHomeButtonEnabled(false)
                supportActionBar?.setDisplayShowHomeEnabled(false)
            }

            if (destination.id == R.id.nav_home) {
                supportActionBar?.title = getString(R.string.swipe_title)
            }

        }

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
                R.id.nav_terms,
                R.id.nav_login_required,
                R.id.action_nav_home_to_see_new_posts,
                R.id.nav_item_detail,
                R.id.nav_disliked,
                R.id.nav_chatconvo,
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController!!, appBarConfiguration)

        NavigationUI.setupWithNavController(navView, navController!!)

        // Ensure navigation with drawer item
        // this looks patchy because it is, but think of it as a safe-guard
        navView.setNavigationItemSelectedListener { item ->
            val handled = try {
                when (item.itemId) {
                    // do the standard navigation
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
                            // First pop back to the graph start to remove transient detail screens
                            nc.popBackStack(nc.graph.startDestinationId, false)
                            // Then navigate to the wall list
                            nc.navigate(R.id.nav_wall)
                        }
                        true
                    }

                    // Ensure 'Profile' always lands on the profile (ProfileFragment)
                    R.id.nav_profile -> {
                        navController?.let { nc ->
                            nc.popBackStack(nc.graph.startDestinationId, false)
                            nc.navigate(R.id.nav_profile)
                        }
                        true
                    }

                    // Ensure 'Liked posts' always lands on the liked list (LikedFragment)
                    R.id.nav_liked -> {
                        navController?.let { nc ->
                            nc.popBackStack(nc.graph.startDestinationId, false)
                            nc.navigate(R.id.nav_liked)
                        }
                        true
                    }

                    // Ensure 'Messages' always lands on the chat list (ChatListFragment)
                    R.id.nav_chat_list -> {
                        navController?.let { nc ->
                            // Remove transient/detail screens (e.g. an open conversation) before navigating
                            nc.popBackStack(nc.graph.startDestinationId, false)
                            nc.navigate(R.id.nav_chat_list)
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

        // Listener for login/logout. A lot of this is for patchy bug fixes
        authStateListener = FirebaseAuth.AuthStateListener {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                updateDrawerMenu()
                invalidateOptionsMenu()

                // If the user logged out while they were on a protected screen, kick them out.
                val isLoggedIn = firebaseAuth.currentUser != null
                val currentDestId = navController?.currentDestination?.id
                if (!isLoggedIn && currentDestId != null && currentDestId in authRequiredDestinations) {
                    navController?.navigate(R.id.nav_login_required)
                }

                // If the user just became logged in while on a login screen, navigate to home.
                if (isLoggedIn && currentDestId != null && currentDestId in setOf(
                        R.id.nav_login,
                        R.id.nav_login_required,
                        R.id.nav_create_account
                    )) {
                    // Global navigation
                    val options = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_login_required, true)
                        .build()
                    try {
                        navController?.navigate(R.id.action_global_nav_home, null, options)
                    } catch (_: IllegalArgumentException) {
                        // Fallback: directly navigate to destination id
                        navController?.navigate(R.id.nav_home, null, options)
                    }
                }

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

        // Create post should only be visible when logged in
        menu.findItem(R.id.nav_createpost)?.isVisible = isLoggedIn
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
        return NavigationUI.navigateUp(navController!!, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
