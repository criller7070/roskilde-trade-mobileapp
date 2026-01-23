package dk.rosswap.mobile.feature.account.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.feature.account.domain.AccountItem
import dk.rosswap.mobile.feature.account.domain.DeleteAccountUseCase
import dk.rosswap.mobile.feature.account.domain.ExportAccountUseCase
import dk.rosswap.mobile.feature.items.domain.DeleteItemUseCase
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var sessionManager: SessionManager
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private lateinit var itemsRepository: ItemsRepository
    private lateinit var deleteItemUseCase: DeleteItemUseCase
    private lateinit var exportAccountUseCase: ExportAccountUseCase
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        sessionManager = mockk()
        storage = mockk()
        firestore = mockk()
        itemsRepository = mockk()
        deleteItemUseCase = mockk()
        exportAccountUseCase = mockk()
        deleteAccountUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMyPosts - filters to current user's posts`() = runTest {
        val uid = "u1"
        val user = User(uid = uid, name = "User1")

        every { sessionManager.currentUserId() } returns uid
        every { sessionManager.authState } returns MutableStateFlow(AuthState.Authenticated(user))

        val mine = Item("1", "t1", "d1", "sell", "", uid, "me")
        val other = Item("2", "t2", "d2", "trade", "", "x", "them")

        coEvery { itemsRepository.getLatestItems(any()) } returns Result.success(listOf(mine, other))

        viewModel = ProfileViewModel(
            sessionManager = sessionManager,
            storage = storage,
            firestore = firestore,
            itemsRepository = itemsRepository,
            deleteItemUseCase = deleteItemUseCase,
            exportAccountUseCase = exportAccountUseCase,
            deleteAccountUseCase = deleteAccountUseCase
        )

        advanceUntilIdle()

        val posts = viewModel.posts.getOrAwaitValue()
        Assert.assertEquals(1, posts.size)
        Assert.assertEquals("1", (posts[0] as AccountItem).id)
    }


    @Test
    fun `deleteAccount - 401 triggers reauth`() = runTest {
        val uid = "u1"

        every { sessionManager.currentUserId() } returns uid
        every { sessionManager.authState } returns MutableStateFlow(
            AuthState.Authenticated(User(uid = uid))
        )

        coEvery { deleteAccountUseCase(uid) } returns Result.failure(Exception("401 unauthorized"))

        viewModel = ProfileViewModel(
            sessionManager = sessionManager,
            storage = storage,
            firestore = firestore,
            itemsRepository = itemsRepository,
            deleteItemUseCase = deleteItemUseCase,
            exportAccountUseCase = exportAccountUseCase,
            deleteAccountUseCase = deleteAccountUseCase
        )

        var reauth = false

        viewModel.deleteAccount(
            onSuccess = {},
            onReauthRequired = { reauth = true },
            onError = {}
        )

        advanceUntilIdle()

        Assert.assertTrue(reauth)
    }

    @Test
    fun `exportAccount - not signed in returns failure`() = runTest {
        every { sessionManager.currentUserId() } returns null
        every { sessionManager.authState } returns MutableStateFlow(AuthState.Unauthenticated)

        viewModel = ProfileViewModel(
            sessionManager = sessionManager,
            storage = storage,
            firestore = firestore,
            itemsRepository = itemsRepository,
            deleteItemUseCase = deleteItemUseCase,
            exportAccountUseCase = exportAccountUseCase,
            deleteAccountUseCase = deleteAccountUseCase
        )

        var result: Result<String>? = null
        viewModel.exportAccount("someone") { result = it }

        advanceUntilIdle()
        Assert.assertTrue(result?.isFailure == true)
    }

    // LiveData helper
    private fun <T> LiveData<T>.getOrAwaitValue(timeout: Long = 2_000L): T {
        var data: T? = null
        val latch = java.util.concurrent.CountDownLatch(1)

        val observer = object : Observer<T> {
            override fun onChanged(value: T) {
                data = value
                latch.countDown()
                removeObserver(this)
            }
        }

        observeForever(observer)

        if (!latch.await(timeout, java.util.concurrent.TimeUnit.MILLISECONDS)) {
            removeObserver(observer)
            throw RuntimeException("LiveData value was never set")
        }

        return data!!
    }
}
