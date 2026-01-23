package dk.rosswap.mobile.feature.auth.presentation

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import dk.rosswap.mobile.feature.items.domain.AddItemUseCase
import dk.rosswap.mobile.feature.items.presentation.AddItemViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var addItemUseCase: AddItemUseCase
    private lateinit var viewModel: AddItemViewModel

    @Before
    fun setup() {
        addItemUseCase = Mockito.mock(AddItemUseCase::class.java)
        viewModel = AddItemViewModel(addItemUseCase)
    }

    @Test
    fun `createItem - success posts success and resets loading`() = runTest {
        // Arrange
        whenever(
            addItemUseCase.invoke(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyString()
            )
        ).thenReturn(Result.success(Unit))

        val results = mutableListOf<Result<Unit>>()
        val loading = mutableListOf<Boolean>()

        viewModel.createResult.observeForever { results.add(it) }
        viewModel.isLoading.observeForever { loading.add(it) }

        // Act
        viewModel.createItem(
            "title",
            "desc",
            null, // ✅ FIX: no Android Uri in JVM test
            "type"
        )

        advanceUntilIdle()
        ArchTaskExecutor.getInstance().executeOnMainThread {}

        // Assert
        Assert.assertTrue(results.last().isSuccess)
        Assert.assertEquals(false, loading.last())

        verify(addItemUseCase).invoke(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyString()
        )
    }

    @Test
    fun `createItem - failure posts failure result and resets loading`() = runTest {
        // Arrange
        val ex = RuntimeException("network")

        whenever(
            addItemUseCase.invoke(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyString()
            )
        ).thenReturn(Result.failure(ex))

        val results = mutableListOf<Result<Unit>>()
        val loading = mutableListOf<Boolean>()

        viewModel.createResult.observeForever { results.add(it) }
        viewModel.isLoading.observeForever { loading.add(it) }

        // Act
        viewModel.createItem(
            "title",
            "desc",
            null, // ✅ FIX
            "type"
        )

        advanceUntilIdle()
        ArchTaskExecutor.getInstance().executeOnMainThread {}

        // Assert
        Assert.assertTrue(results.last().isFailure)
        Assert.assertEquals(false, loading.last())

        verify(addItemUseCase).invoke(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.anyString()
        )
    }
}
