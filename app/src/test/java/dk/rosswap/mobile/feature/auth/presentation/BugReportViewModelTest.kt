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
class BugReportViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

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

        Assert.assertFalse(viewModel.isLoading.value == true)

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
    }

    @Test
    fun `submitBug - failure publishes failure and resets loading`() = runTest {
        val ex = RuntimeException("network")
        coEvery { reportBugUseCase(any(), any()) } returns Result.failure(ex)

        viewModel.submitBug("d2", null)
        advanceUntilIdle()

        val result = viewModel.submitResult.getOrAwaitValue()
        Assert.assertTrue(result.isFailure)
        Assert.assertFalse(viewModel.isLoading.getOrAwaitValue())
        coVerify(exactly = 1) { reportBugUseCase(any(), any()) }
    }

    // LiveData helper (same as other tests)
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