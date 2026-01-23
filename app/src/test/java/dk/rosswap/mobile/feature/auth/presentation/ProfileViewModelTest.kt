package dk.rosswap.mobile.feature.auth.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.feature.account.domain.AccountItem
import dk.rosswap.mobile.feature.account.presentation.ProfileViewModel
import dk.rosswap.mobile.feature.items.domain.GetItemsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var sessionManager: SessionManager
    private lateinit var getItemsUseCase: GetItemsUseCase
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionManager = mockk()
        getItemsUseCase = mockk<GetItemsUseCase>()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMyPosts - when no current user posts becomes empty`() = runTest {
        // Arrange: currentUserId returns null
        coEvery { sessionManager.currentUserId() } returns null
        // authState must be present to satisfy init user read
        coEvery { sessionManager.authState } returns MutableStateFlow(AuthState.Unauthenticated)

        // Act: create ViewModel (init will call loadMyPosts)
        viewModel = ProfileViewModel(sessionManager, mockk(), getItemsUseCase)
        advanceUntilIdle()

        // Assert: posts should be empty and getItemsUseCase must not be called
        val posts = viewModel.posts.getOrAwaitValue()
        Assert.assertTrue(posts.isEmpty())
        coVerify(exactly = 0) { getItemsUseCase(any<Long>()) }
    }

    @Test
    fun `loadMyPosts - filters to current user's posts`() = runTest {
        // Arrange: current user id
        val uid = "user123"
        coEvery { sessionManager.currentUserId() } returns uid
        val user = User(uid = uid, name = "u", email = "e@example.com")
        coEvery { sessionManager.authState } returns MutableStateFlow(AuthState.Authenticated(user))

        val itemMine = Item(
            id = "1",
            title = "a",
            description = "d",
            mode = "sell",
            imageUrl = "",
            userId = uid,
            userName = "me"
        )
        val itemOther = Item(
            id = "2",
            title = "b",
            description = "d2",
            mode = "trade",
            imageUrl = "",
            userId = "other",
            userName = "them"
        )

        coEvery { getItemsUseCase(any<Long>()) } returns Result.success(listOf(itemMine, itemOther))

        // Act
        viewModel = ProfileViewModel(sessionManager, mockk(), getItemsUseCase)
        advanceUntilIdle()

        // Assert: posts should contain only mapped AccountItem for itemMine
        val posts = viewModel.posts.getOrAwaitValue()
        Assert.assertEquals(1, posts.size)
        val p: AccountItem = posts[0]
        Assert.assertEquals("1", p.id)
        Assert.assertEquals("a", p.title)
        coVerify(exactly = 1) { getItemsUseCase(any<Long>()) }
    }

    @Test
    fun `deleteAccount - on success calls onSuccess callback`() = runTest {
        // Arrange
        coEvery { sessionManager.currentUserId() } returns null
        coEvery { sessionManager.authState } returns MutableStateFlow(AuthState.Unauthenticated)
        coEvery { sessionManager.deleteAccount() } returns Result.success(Unit)

        viewModel = ProfileViewModel(sessionManager, mockk(), getItemsUseCase)

        var called = false

        // Act
        viewModel.deleteAccount(onSuccess = { called = true }, onReauthRequired = { }, onError = {})
        advanceUntilIdle()

        // Assert
        Assert.assertTrue(called)
        coVerify(exactly = 1) { sessionManager.deleteAccount() }
    }

    @Test
    fun `deleteAccount - on failure calls onError callback`() = runTest {
        // Arrange
        coEvery { sessionManager.currentUserId() } returns null
        coEvery { sessionManager.authState } returns MutableStateFlow(AuthState.Unauthenticated)
        val ex = RuntimeException("boom")
        coEvery { sessionManager.deleteAccount() } returns Result.failure(ex)

        viewModel = ProfileViewModel(sessionManager, mockk(), getItemsUseCase)

        var gotError: Exception? = null

        // Act
        viewModel.deleteAccount(
            onSuccess = {},
            onReauthRequired = {},
            onError = { e -> gotError = e })
        advanceUntilIdle()

        // Assert
        Assert.assertTrue(gotError === ex || gotError?.message == ex.message)
        coVerify(exactly = 1) { sessionManager.deleteAccount() }
    }

    // LiveData helper
    private fun <T> LiveData<T>.getOrAwaitValue(
        time: Long = 2_000L
    ): T {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)

        var observer: Observer<T>? = null
        observer = Observer { o ->
            data[0] = o
            latch.countDown()
            observer?.let { this.removeObserver(it) }
        }

        this.observeForever(observer)

        if (!latch.await(time, TimeUnit.MILLISECONDS)) {
            this.removeObserver(observer)
            throw java.lang.RuntimeException("LiveData value was never set.")
        }

        @Suppress("UNCHECKED_CAST")
        return data[0] as T
    }
}