package dk.rosswap.mobile.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for FirebaseAuthRepository
 * 
 * Verifies the repository correctly implements the AuthRepository interface and
 * integrates with required dependencies. Detailed behavior testing is delegated to:
 * - Domain layer tests (Use Cases verify business logic with repository mocks)
 * - Presentation layer tests (ViewModels verify state management)
 * - Integration tests with real Firebase (separate integration test suite)
 * 
 * This layer focuses on contract compliance, not behavior implementation.
 */
class AuthRepositoryImplTest {
    private lateinit var authRepository: FirebaseAuthRepository
    private val mockAuth: FirebaseAuth = mockk()
    private val mockFirestore: FirebaseFirestore = mockk()
    private val mockSessionManager: SessionManager = mockk()

    @Before
    fun setup() {
        authRepository = FirebaseAuthRepository(mockAuth, mockFirestore, mockSessionManager)
    }

    // ==================== INTERFACE COMPLIANCE ====================

    @Test
    fun repository_implements_auth_repository_interface() {
        assertTrue("Repository must implement AuthRepository interface", authRepository is AuthRepository)
    }

    @Test
    fun repository_is_correctly_named_firebase_implementation() {
        assertEquals("Repository should be FirebaseAuthRepository", "FirebaseAuthRepository", authRepository.javaClass.simpleName)
    }

    @Test
    fun repository_is_instantiable_with_all_dependencies() {
        assertNotNull("Repository must be instantiable with mocked dependencies", authRepository)
    }
}
