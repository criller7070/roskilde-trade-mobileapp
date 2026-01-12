package dk.rosswap.mobile.feature.auth.presentation

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.common.User
import dk.rosswap.mobile.feature.auth.domain.EnrichUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var enrichUserUseCase: EnrichUserUseCase

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    @Mock
    private lateinit var mockPhotoUri: Uri

    private lateinit var viewModel: AuthViewModel
    private var authStateListenerCaptor: FirebaseAuth.AuthStateListener? = null
    private lateinit var closeable: AutoCloseable

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Capture the auth state listener when addAuthStateListener is called
        whenever(firebaseAuth.addAuthStateListener(any())).thenAnswer { invocation ->
            authStateListenerCaptor = invocation.getArgument(0)
            null
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        closeable.close()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel = AuthViewModel(firebaseAuth, enrichUserUseCase)
        
        viewModel.authState.test {
            val state = awaitItem()
            assertIs<AuthState.Loading>(state)
        }
    }

    @Test
    fun `state transitions to Unauthenticated when no user`() = runTest {
        viewModel = AuthViewModel(firebaseAuth, enrichUserUseCase)
        
        viewModel.authState.test {
            // Skip initial Loading state
            awaitItem()
            
            // Simulate Firebase auth state change with no user
            whenever(firebaseAuth.currentUser).thenReturn(null)
            authStateListenerCaptor?.onAuthStateChanged(firebaseAuth)
            
            val state = awaitItem()
            assertIs<AuthState.Unauthenticated>(state)
        }
    }

    @Test
    fun `state transitions to Authenticated when user enrichment succeeds`() = runTest {
        val userId = "test-uid"
        val userName = "Test User"
        val userEmail = "test@example.com"
        
        // Setup Firebase user
        whenever(firebaseUser.uid).thenReturn(userId)
        whenever(firebaseUser.displayName).thenReturn(userName)
        whenever(firebaseUser.email).thenReturn(userEmail)
        whenever(firebaseUser.photoUrl).thenReturn(null)
        whenever(firebaseUser.isAnonymous).thenReturn(false)
        
        val baseUser = User(
            uid = userId,
            name = userName,
            email = userEmail,
            photoURL = "",
            createdAt = null,
            gdprConsent = false,
            consentedAt = null,
            likedItemIds = emptyList(),
            dislikedItemIds = emptyList(),
            isAnonymous = false
        )
        
        val enrichedUser = baseUser.copy(gdprConsent = true)
        whenever(enrichUserUseCase(any())).thenReturn(enrichedUser)
        
        viewModel = AuthViewModel(firebaseAuth, enrichUserUseCase)
        
        viewModel.authState.test {
            // Skip initial Loading state
            awaitItem()
            
            // Simulate Firebase auth state change with user
            whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
            authStateListenerCaptor?.onAuthStateChanged(firebaseAuth)
            
            // Wait for async enrichment to complete
            testDispatcher.scheduler.advanceUntilIdle()
            
            val state = awaitItem()
            assertIs<AuthState.Authenticated>(state)
            assertEquals(enrichedUser, state.user)
        }
    }

    @Test
    fun `state transitions to Error when user enrichment fails`() = runTest {
        val userId = "test-uid"
        val exception = Exception("Enrichment failed")
        
        // Setup Firebase user
        whenever(firebaseUser.uid).thenReturn(userId)
        whenever(firebaseUser.displayName).thenReturn("Test User")
        whenever(firebaseUser.email).thenReturn("test@example.com")
        whenever(firebaseUser.photoUrl).thenReturn(null)
        whenever(firebaseUser.isAnonymous).thenReturn(false)
        
        whenever(enrichUserUseCase(any())).thenThrow(exception)
        
        viewModel = AuthViewModel(firebaseAuth, enrichUserUseCase)
        
        viewModel.authState.test {
            // Skip initial Loading state
            awaitItem()
            
            // Simulate Firebase auth state change with user
            whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
            authStateListenerCaptor?.onAuthStateChanged(firebaseAuth)
            
            // Wait for async enrichment to complete
            testDispatcher.scheduler.advanceUntilIdle()
            
            val state = awaitItem()
            assertIs<AuthState.Error>(state)
            assertEquals(exception, state.exception)
        }
    }

    @Test
    fun `signOut calls Firebase signOut`() = runTest {
        viewModel = AuthViewModel(firebaseAuth, enrichUserUseCase)
        
        viewModel.signOut()
        
        verify(firebaseAuth).signOut()
    }

    @Test
    fun `signOut sets Error state when Firebase signOut throws exception`() = runTest {
        val exception = Exception("Sign out failed")
        whenever(firebaseAuth.signOut()).thenThrow(exception)
        
        viewModel = AuthViewModel(firebaseAuth, enrichUserUseCase)
        
        viewModel.authState.test {
            // Skip initial Loading state
            awaitItem()
            
            viewModel.signOut()
            
            val state = awaitItem()
            assertIs<AuthState.Error>(state)
            assertEquals(exception, state.exception)
        }
    }

    @Test
    fun `handles anonymous user correctly`() = runTest {
        val userId = "anonymous-uid"
        
        // Setup anonymous Firebase user
        whenever(firebaseUser.uid).thenReturn(userId)
        whenever(firebaseUser.displayName).thenReturn(null)
        whenever(firebaseUser.email).thenReturn(null)
        whenever(firebaseUser.photoUrl).thenReturn(null)
        whenever(firebaseUser.isAnonymous).thenReturn(true)
        
        val baseUser = User(
            uid = userId,
            name = "",
            email = "",
            photoURL = "",
            createdAt = null,
            gdprConsent = false,
            consentedAt = null,
            likedItemIds = emptyList(),
            dislikedItemIds = emptyList(),
            isAnonymous = true
        )
        
        val enrichedUser = baseUser.copy(gdprConsent = false)
        whenever(enrichUserUseCase(any())).thenReturn(enrichedUser)
        
        viewModel = AuthViewModel(firebaseAuth, enrichUserUseCase)
        
        viewModel.authState.test {
            // Skip initial Loading state
            awaitItem()
            
            // Simulate Firebase auth state change with anonymous user
            whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
            authStateListenerCaptor?.onAuthStateChanged(firebaseAuth)
            
            // Wait for async enrichment to complete
            testDispatcher.scheduler.advanceUntilIdle()
            
            val state = awaitItem()
            assertIs<AuthState.Authenticated>(state)
            assertEquals(true, state.user.isAnonymous)
        }
    }

    @Test
    fun `enrichUserUseCase is called with correct base user data`() = runTest {
        val userId = "test-uid"
        val userName = "Test User"
        val userEmail = "test@example.com"
        val photoUrl = "https://example.com/photo.jpg"
        
        // Setup Firebase user with photo URL
        whenever(mockPhotoUri.toString()).thenReturn(photoUrl)
        whenever(firebaseUser.uid).thenReturn(userId)
        whenever(firebaseUser.displayName).thenReturn(userName)
        whenever(firebaseUser.email).thenReturn(userEmail)
        whenever(firebaseUser.photoUrl).thenReturn(mockPhotoUri)
        whenever(firebaseUser.isAnonymous).thenReturn(false)
        
        val expectedBaseUser = User(
            uid = userId,
            name = userName,
            email = userEmail,
            photoURL = photoUrl,
            createdAt = null,
            gdprConsent = false,
            consentedAt = null,
            likedItemIds = emptyList(),
            dislikedItemIds = emptyList(),
            isAnonymous = false
        )
        
        whenever(enrichUserUseCase(expectedBaseUser)).thenReturn(expectedBaseUser)
        
        viewModel = AuthViewModel(firebaseAuth, enrichUserUseCase)
        
        // Simulate Firebase auth state change with user
        whenever(firebaseAuth.currentUser).thenReturn(firebaseUser)
        authStateListenerCaptor?.onAuthStateChanged(firebaseAuth)
        
        // Wait for async enrichment to complete
        testDispatcher.scheduler.advanceUntilIdle()
        
        verify(enrichUserUseCase).invoke(expectedBaseUser)
    }
}
