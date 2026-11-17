package by.overpas.template.core.arch

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InstanceScopeTest {

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `instance scope cancelled on destroy`() =
        runTest {
            val sut = InstanceScope(Job())

            sut.onDestroy()

            assertFalse(sut.coroutineContext.isActive)
        }

    @Test
    fun `instance scope created once`() {
        val sut = DefaultComponentContext(LifecycleRegistry())

        val instanceScope1 = sut.instanceScope
        val instanceScope2 = sut.instanceScope

        assertTrue { instanceScope1 === instanceScope2 }
    }

    @Test
    fun `instance scope has main dispatcher`() =
        runTest {
            val sut = DefaultComponentContext(LifecycleRegistry())

            val instanceScope = sut.instanceScope
            val dispatcher = instanceScope.coroutineContext[ContinuationInterceptor]

            assertIs<MainCoroutineDispatcher>(dispatcher)
        }
}
