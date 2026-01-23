package dk.rosswap.mobile.feature.auth.presentation

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import dk.rosswap.mobile.feature.items.domain.AddItemUseCase
import dk.rosswap.mobile.feature.items.presentation.AddItemViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
class AddItemViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var addItemUseCase: AddItemUseCase
    private lateinit var viewModel: AddItemViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        addItemUseCase = mockk()
        viewModel = AddItemViewModel(addItemUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createPost - happy path posts success and resets loading`() = runTest {
        // Arrange
        coEvery { addItemUseCase(any(), any(), any(), any()) } returns Result.success(Unit)

        // Precondition
        Assert.assertFalse(viewModel.isLoading.value == true)

        // Act
        invokeCreatePost(viewModel, "t", "d", null, "type")
        advanceUntilIdle()

        // Assert createResult contains success
        val result = viewModel.createResult.getOrAwaitValue()
        Assert.assertTrue(result.isSuccess)
        // isLoading should be false after completion
        Assert.assertFalse(viewModel.isLoading.getOrAwaitValue())
        coVerify(exactly = 1) { addItemUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `createPost - prevents concurrent runs when already loading`() = runTest {
        // Arrange: make the use case suspend for a while to simulate long-running work
        coEvery { addItemUseCase(any(), any(), any(), any()) } coAnswers {
            delay(1000)
            Result.success(Unit)
        }

        // Act: call twice quickly
        invokeCreatePost(viewModel, "t1", "d1", null, "typeA")
        invokeCreatePost(viewModel, "t1", "d1", null, "typeA") // should be ignored while loading
        advanceUntilIdle()

        // Assert: use case should have been invoked only once
        coVerify(exactly = 1) { addItemUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `createPost - failure posts failure result and resets loading`() = runTest {
        // Arrange
        val ex = RuntimeException("network")
        coEvery { addItemUseCase(any(), any(), any(), any()) } returns Result.failure(ex)

        // Act
        invokeCreatePost(viewModel, "t", "d", null, "type")
        advanceUntilIdle()

        // Assert
        val result = viewModel.createResult.getOrAwaitValue()
        Assert.assertTrue(result.isFailure)
        Assert.assertFalse(viewModel.isLoading.getOrAwaitValue())
        coVerify(exactly = 1) { addItemUseCase(any(), any(), any(), any()) }
    }

    // -----------------------------------------
    // Helper: LiveData getOrAwaitValue (simple)
    // -----------------------------------------
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

    // Invoke the ViewModel.createPost via reflection to avoid compile-time unresolved-reference in test environment
    private fun invokeCreatePost(vm: AddItemViewModel, title: String, description: String, imageUri: Uri?, type: String) {
        val method = vm.javaClass.getMethod("createPost", String::class.java, String::class.java, Uri::class.java, String::class.java)
        method.invoke(vm, title, description, imageUri, type)
    }
}