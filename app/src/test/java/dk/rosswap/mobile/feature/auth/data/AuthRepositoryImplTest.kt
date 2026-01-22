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
 * Note: Repository-level Firebase testing is best done via:
 * - Integration tests with Firebase Emulator (tests actual Firebase interactions)
 * - Use case layer tests with mocked repository (tests business logic)
 * 
 * This test focuses on contract verification - ensuring the repository
 * implements the required interface and can be instantiated with dependencies.
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

    // ==================== CONTRACT COMPLIANCE ====================

    @Test
    fun repository_implements_auth_repository_interface() {
        assertTrue("Repository must implement AuthRepository interface", authRepository is AuthRepository)
    }

    @Test
    fun repository_can_be_instantiated_with_all_dependencies() {
        assertNotNull("Repository must be instantiable with mocked dependencies", authRepository)
    }

    @Test
    fun repository_is_correctly_named_firebase_implementation() {
        assertEquals("Repository should be FirebaseAuthRepository", "FirebaseAuthRepository", authRepository.javaClass.simpleName)
    }
}
