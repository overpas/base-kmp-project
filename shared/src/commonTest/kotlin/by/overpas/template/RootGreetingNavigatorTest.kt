package by.overpas.template

import by.overpas.template.feature.greeting.GreetingNavigator
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs

internal class RootGreetingNavigatorTest {

    private val context = DefaultComponentContext(LifecycleRegistry())
    private val graph = createGraphFactory<TestRootGreetingNavigatorGraph.Factory>().create(
        componentContext = context,
        rootDependencies = FakeRootDependencies(),
    )

    @Inject
    private lateinit var rootNavigator: RootNavigator

    @Inject
    private lateinit var sut: GreetingNavigator

    @BeforeTest
    fun setup() {
        graph.inject(this)
    }

    @Test
    fun `navigateBack returns to Hello`() {
        graph.helloNavigator.openGreeting("Ada")

        sut.navigateBack()
        val actual = rootNavigator.stack.value
            .active
            .instance

        assertIs<RootComponent.Child.Hello>(actual)
    }
}
