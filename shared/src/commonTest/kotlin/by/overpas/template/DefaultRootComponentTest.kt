package by.overpas.template

import by.overpas.template.di.RootGraph
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertIs

class DefaultRootComponentTest {

    private val context = DefaultComponentContext(LifecycleRegistry())
    private val rootGraph = createGraphFactory<RootGraph.Factory>()
        .create(context, FakeRootDependencies())

    private val sut = rootGraph.rootComponent

    @Test
    fun `initial child is Hello`() {
        val actual = sut.stack.value
            .items
            .first()
            .instance

        assertIs<RootComponent.Child.Hello>(actual)
    }
}
