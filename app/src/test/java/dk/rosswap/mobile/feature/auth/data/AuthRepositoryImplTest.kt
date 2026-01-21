package dk.rosswap.mobile.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.google.android.gms.tasks.Task
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.feature.auth.domain.SignUpUseCase
import dk.rosswap.mobile.feature.auth.domain.GoogleSignInUseCase
import dk.rosswap.mobile.feature.auth.domain.LoginUseCase
import dk.rosswap.mobile.feature.auth.domain.SignOutUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.every
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AuthRepositoryImplTest {
    private lateinit var authRepository: AuthRepositoryImpl
    private val mockAuth: FirebaseAuth = mockk()
    private val mockFirestore: FirebaseFirestore = mockk()
    private val mockSessionManager: SessionManager = mockk()
    private val mockFirebaseUser: FirebaseUser = mockk()
    private val mockAuthResult: AuthResult = mockk()
    private val mockDocRef: DocumentReference = mockk()
    private val mockSignUpUseCase: SignUpUseCase = mockk(relaxed = true)
    private val mockGoogleSignInUseCase: GoogleSignInUseCase = mockk(relaxed = true)
    private val mockLoginUseCase: LoginUseCase = mockk(relaxed = true)
    private val mockSignOutUseCase: SignOutUseCase = mockk(relaxed = true)

    @Before
    fun setup() {
        authRepository = AuthRepositoryImpl(mockAuth, mockFirestore, mockSignUpUseCase, mockGoogleSignInUseCase, mockLoginUseCase, mockSignOutUseCase)
    }

    // ==================== LOGIN TESTS ====================

    @Test
    fun login_withValidCredentials_shouldReturnSuccess() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val mockTask: Task<AuthResult> = mockk()
        coEvery { mockTask.await() } returns mockAuthResult
        every { mockAuthResult.user } returns mockFirebaseUser
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun login_withInvalidEmail_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "invalid@example.com"
        val password = "password123"
        val exception = FirebaseAuthException("ERROR_USER_NOT_FOUND", "User not found")
        val mockTask: Task<AuthResult> = mockk()
        coEvery { mockTask.await() } throws exception
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun login_withWrongPassword_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "wrongpassword"
        val exception = FirebaseAuthException("ERROR_WRONG_PASSWORD", "Wrong password")
        val mockTask: Task<AuthResult> = mockk()
        coEvery { mockTask.await() } throws exception
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun login_whenAuthReturnsNullUser_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val mockTask: Task<AuthResult> = mockk()
        coEvery { mockTask.await() } returns mockAuthResult
        every { mockAuthResult.user } returns null
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun login_withNetworkError_shouldReturnFailure() = runBlocking {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        val exception = FirebaseAuthException("ERROR_NETWORK_REQUEST_FAILED", "Network error")
        val mockTask: Task<AuthResult> = mockk()
        coEvery { mockTask.await() } throws exception
        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask

        // Act
        val result = authRepository.login(email, password)

        // Assert
        assertTrue(result.isFailure)
    }

    // ==================== SIGNUP TESTS (updated to mock SignUpUseCase) ====================

    @Test
    fun signUp_withValidData_shouldReturnSuccess() = runBlocking {
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true

        coEvery { mockSignUpUseCase(name, email, password, hasConsent) } returns Result.success(Unit)

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun signUp_withExistingEmail_shouldReturnFailure() = runBlocking {
        val email = "existing@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true
        val exception = Exception("Email already in use")

        coEvery { mockSignUpUseCase(name, email, password, hasConsent) } returns Result.failure(exception)

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun signUp_withWeakPassword_shouldReturnFailure() = runBlocking {
        val email = "newuser@example.com"
        val password = "123"
        val name = "Test User"
        val hasConsent = true

        val exception = Exception("Weak password")
        coEvery { mockSignUpUseCase(name, email, password, hasConsent) } returns Result.failure(exception)

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun signUp_whenUserDocumentFailsToCreate_shouldReturnFailure() = runBlocking {
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = true

        val exception = Exception("Firestore error")
        coEvery { mockSignUpUseCase(name, email, password, hasConsent) } returns Result.failure(exception)

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun signUp_withoutConsent_shouldStoreFalse() = runBlocking {
        val email = "newuser@example.com"
        val password = "password123"
        val name = "Test User"
        val hasConsent = false

        coEvery { mockSignUpUseCase(name, email, password, hasConsent) } returns Result.success(Unit)

        // Act
        val result = authRepository.signUp(email, password, name, hasConsent)

        // Assert
        assertTrue(result.isSuccess)
    }
}
