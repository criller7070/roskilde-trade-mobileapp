package dk.rosswap.mobile.feature.account.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.feature.account.domain.AccountItem
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import dk.rosswap.mobile.feature.items.domain.DeleteItemUseCase
import dk.rosswap.mobile.feature.account.domain.ExportAccountUseCase
import dk.rosswap.mobile.feature.account.domain.DeleteAccountUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var sessionManager: SessionManager
    private lateinit var itemsRepository: ItemsRepository
    private lateinit var deleteItemUseCase: DeleteItemUseCase
    private lateinit var exportAccountUseCase: ExportAccountUseCase
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase
    private lateinit var storage: com.google.firebase.storage.FirebaseStorage
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionManager = mockk()
        itemsRepository = mockk()
        deleteItemUseCase = mockk()
        exportAccountUseCase = mockk()
        deleteAccountUseCase = mockk()
        storage = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMyPosts - filters to current user's posts`() = runTest {
        val uid = "u1"
        every { sessionManager.currentUserId() } returns uid
        val user = User(uid = uid, name = "User1")
        every { sessionManager.authState } returns MutableStateFlow(AuthState.Authenticated(user))

        val mine = Item(id = "1", title = "t1", description = "d1", mode = "sell", imageUrl = "", userId = uid, userName = "me")
        val other = Item(id = "2", title = "t2", description = "d2", mode = "trade", imageUrl = "", userId = "x", userName = "them")

        coEvery { itemsRepository.getLatestItems(any()) } returns Result.success(listOf(mine, other))

        viewModel = ProfileViewModel(sessionManager, storage, itemsRepository, deleteItemUseCase, exportAccountUseCase, deleteAccountUseCase)
        advanceUntilIdle()

        val posts = viewModel.posts.getOrAwaitValue()
        assertEquals(1, posts.size)
        val p: AccountItem = posts[0]
        assertEquals("1", p.id)
    }

    @Test
    fun `deleteAccount - on 401 triggers reauth`() = runTest {
        val uid = "u1"
        every { sessionManager.currentUserId() } returns uid
        every { sessionManager.authState } returns MutableStateFlow(AuthState.Authenticated(User(uid = uid)))

        coEvery { deleteAccountUseCase.invoke(uid) } returns Result.failure(Exception("401 unauthorized"))

        viewModel = ProfileViewModel(sessionManager, storage, itemsRepository, deleteItemUseCase, exportAccountUseCase, deleteAccountUseCase)

        var reauth = false

        viewModel.deleteAccount(onSuccess = { }, onReauthRequired = { reauth = true }, onError = { })
        advanceUntilIdle()

        assertTrue(reauth)
    }

    @Test
    fun `exportAccount - not signed in returns failure`() = runTest {
        every { sessionManager.currentUserId() } returns null
        every { sessionManager.authState } returns MutableStateFlow(AuthState.Unauthenticated)

        viewModel = ProfileViewModel(sessionManager, storage, itemsRepository, deleteItemUseCase, exportAccountUseCase, deleteAccountUseCase)

        var got: Result<String>? = null
        viewModel.exportAccount("someone") { res -> got = res }
        advanceUntilIdle()

        assertTrue(got?.isFailure == true)
    }

    // LiveData helper
    private fun <T> androidx.lifecycle.LiveData<T>.getOrAwaitValue(time: Long = 2_000L): T {
        val data = arrayOfNulls<Any>(1)
        val latch = java.util.concurrent.CountDownLatch(1)
        var observer: androidx.lifecycle.Observer<T>? = null
        observer = androidx.lifecycle.Observer { o ->
            data[0] = o
            latch.countDown()
            observer?.let { this.removeObserver(it) }
        }
        this.observeForever(observer)
        if (!latch.await(time, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            this.removeObserver(observer)
            throw java.lang.RuntimeException("LiveData value was never set.")
        }
        @Suppress("UNCHECKED_CAST")
        return data[0] as T
    }
}
