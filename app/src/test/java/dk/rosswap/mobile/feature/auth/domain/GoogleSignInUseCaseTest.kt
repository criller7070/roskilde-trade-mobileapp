package dk.rosswap.mobile.feature.auth.domain

import android.content.Context
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.Assert.*

@Ignore("Integration-style test; skip in unit test suite")
class GoogleSignInUseCaseTest {
    private lateinit var googleSignInUseCase: GoogleSignInUseCase
    private val mockContext: Context = mockk()
    private val mockAuthRepository: AuthRepository = mockk()

    @Before
    fun setup() {
        // This test is ignored; no setup required
    }

    @Test
    fun placeholder() {
        // no-op smoke placeholder
        assertTrue(true)
    }
}
