package dk.rosswap.mobile.feature.auth.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import dk.rosswap.mobile.feature.bugreport.domain.ReportBugUseCase
import dk.rosswap.mobile.feature.bugreport.presentation.BugReportViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class BugReportViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var reportBugUseCase: ReportBugUseCase
    private lateinit var viewModel: BugReportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        reportBugUseCase = mockk()
        viewModel = BugReportViewModel(reportBugUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitBug - happy path publishes success and resets loading`() = runTest {
        coEvery { reportBugUseCase(any(), any()) } returns Result.success(Unit)

        Assert.assertFalse(viewModel.isLoading.getOrAwaitValue())

        viewModel.submitBug("desc", null)
        advanceUntilIdle()

        val result = viewModel.submitResult.getOrAwaitValue()
        Assert.assertTrue(result.isSuccess)
        Assert.assertFalse(viewModel.isLoading.getOrAwaitValue())

        coVerify(exactly = 1) { reportBugUseCase(any(), any()) }
    }

    @Test
    fun `submitBug - prevents concurrent submissions`() = runTest {
        coEvery { reportBugUseCase(any(), any()) } coAnswers {
            delay(1000)
            Result.success(Unit)
        }

        viewModel.submitBug("d1", null)
        viewModel.submitBug("d1", null)

        advanceUntilIdle()

        coVerify(exactly = 1) { reportBugUseCase(any(), any()) }
        Assert.assertFalse(viewModel.isLoading.getOrAwaitValue())
    }

    @Test
    fun `submitBug - failure publishes failure and resets loading`() = runTest {
        val exception = RuntimeException("network error")
        coEvery { reportBugUseCase(any(), any()) } returns Result.failure(exception)

        viewModel.submitBug("d2", null)
        advanceUntilIdle()

        val result = viewModel.submitResult.getOrAwaitValue()
        Assert.assertTrue(result.isFailure)
        Assert.assertFalse(viewModel.isLoading.getOrAwaitValue())

        coVerify(exactly = 1) { reportBugUseCase(any(), any()) }
    }

    // --- LiveData test helper ---
    private fun <T> LiveData<T>.getOrAwaitValue(
        time: Long = 2_000L
    ): T {
        val data = arrayOfNulls<Any>(1)
        val latch = CountDownLatch(1)

        var observer: Observer<T>? = null
        observer = Observer { value ->
            data[0] = value
            latch.countDown()
            observer?.let { removeObserver(it) }
        }

        observeForever(observer)

        if (!latch.await(time, TimeUnit.MILLISECONDS)) {
            removeObserver(observer)
            throw RuntimeException("LiveData value was never set.")
        }

        @Suppress("UNCHECKED_CAST")
        return data[0] as T
    }
}
