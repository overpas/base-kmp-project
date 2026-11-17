package by.overpas.template

import by.overpas.template.DefaultRootComponent.Config
import by.overpas.template.feature.hello.HelloNavigator
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RootHelloNavigatorTest {

    private val context = DefaultComponentContext(LifecycleRegistry())
    private val graph = createGraphFactory<TestRootHelloNavigatorGraph.Factory>().create(
        componentContext = context,
        rootDependencies = FakeRootDependencies(),
    )

    @Inject
    private lateinit var rootNavigator: RootNavigator

    @Inject
    private lateinit var sut: HelloNavigator

    @BeforeTest
    fun setup() {
        graph.inject(this)
    }

    @Test
    fun `openGreeting pushes the greeting for the name over Hello`() {
        val expected = listOf(Config.Hello, Config.Greeting("Ada"))

        sut.openGreeting("Ada")
        val actual = rootNavigator.stack.value
            .items
            .map { child -> child.configuration }

        assertEquals(expected, actual)
    }
}
