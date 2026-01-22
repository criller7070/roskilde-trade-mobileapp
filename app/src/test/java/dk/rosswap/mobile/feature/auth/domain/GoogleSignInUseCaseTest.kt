package dk.rosswap.mobile.feature.auth.domain

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for GoogleSignInUseCase
 * Verifies Google authentication delegation to repository and error handling
 */
class GoogleSignInUseCaseTest {
    private lateinit var googleSignInUseCase: GoogleSignInUseCase
    private val mockContext: Context = mockk()
    private val mockAuthRepository: AuthRepository = mockk()

    @Before
    fun setup() {
        googleSignInUseCase = GoogleSignInUseCase(mockContext, mockAuthRepository)
    }

    // ==================== HAPPY PATH ====================

    @Test
    fun invoke_withValidIdToken_shouldDelegateToRepositoryAndReturnSuccess() = runTest {
        // Arrange
        val idToken = "valid.id.token.abc123"
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.success(Unit)

        // Act
        val result = googleSignInUseCase(idToken)

        // Assert - Use case properly delegates and returns success
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockAuthRepository.signInWithGoogle(idToken) }
    }

    // ==================== ERROR HANDLING ====================

    @Test
    fun invoke_withInvalidToken_shouldReturnFailure() = runTest {
        // Arrange
        val idToken = "invalid.token"
        val exception = Exception("Invalid ID token")
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.failure(exception)

        // Act
        val result = googleSignInUseCase(idToken)

        // Assert - Use case propagates repository failure
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { mockAuthRepository.signInWithGoogle(idToken) }
    }

    @Test
    fun invoke_withEmptyToken_shouldReturnFailure() = runTest {
        // Arrange
        val idToken = ""
        val exception = IllegalArgumentException("Token cannot be empty")
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.failure(exception)

        // Act
        val result = googleSignInUseCase(idToken)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun invoke_whenRepositoryThrowsException_shouldPropagateError() = runTest {
        // Arrange
        val idToken = "valid.token"
        val exception = RuntimeException("Network error during Google sign-in")
        coEvery { mockAuthRepository.signInWithGoogle(idToken) } returns Result.failure(exception)

        // Act
        val result = googleSignInUseCase(idToken)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    // ==================== REUSABILITY ====================

    @Test
    fun invoke_multipleTimesWithDifferentTokens_shouldWorkCorrectly() = runTest {
        // Arrange - Repository accepts any token and returns success
        coEvery { mockAuthRepository.signInWithGoogle(any()) } returns Result.success(Unit)

        // Act & Assert - First sign in
        val result1 = googleSignInUseCase("token1")
        assertTrue(result1.isSuccess)

        // Act & Assert - Second sign in
        val result2 = googleSignInUseCase("token2")
        assertTrue(result2.isSuccess)

        // Assert - Repository was called exactly twice with different tokens
        coVerify(exactly = 1) { mockAuthRepository.signInWithGoogle("token1") }
        coVerify(exactly = 1) { mockAuthRepository.signInWithGoogle("token2") }
    }

    // ==================== PARAMETER PASSING ====================

    @Test
    fun invoke_shouldPassTokenExactlyToRepository() = runTest {
        // Arrange
        val specificToken = "specific.token.xyz789"
        coEvery { mockAuthRepository.signInWithGoogle(specificToken) } returns Result.success(Unit)

        // Act
        googleSignInUseCase(specificToken)

        // Assert - Repository received exact token
        coVerify(exactly = 1) { mockAuthRepository.signInWithGoogle(specificToken) }
    }
}
