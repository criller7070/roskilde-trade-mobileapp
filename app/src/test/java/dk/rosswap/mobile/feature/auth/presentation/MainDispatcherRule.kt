package dk.rosswap.mobile.feature.auth.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit4 Test Rule that sets Dispatchers.Main to a [TestDispatcher] for unit tests and resets it afterward.
 * Use by adding `@get:Rule val mainDispatcherRule = MainDispatcherRule()` in your test.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Suppress("unused")
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
