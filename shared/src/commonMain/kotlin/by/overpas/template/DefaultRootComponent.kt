package by.overpas.template

import by.overpas.template.di.RootScope
import by.overpas.template.feature.greeting.GreetingComponent
import by.overpas.template.feature.hello.HelloComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.serialization.Serializable

interface RootComponent {

    val stack: Value<ChildStack<*, Child>>

    sealed interface Child {

        class Hello(val component: HelloComponent) : Child

        class Greeting(val component: GreetingComponent) : Child
    }
}

@Inject
@ContributesBinding(RootScope::class, binding<RootComponent>())
internal class DefaultRootComponent(
    context: ComponentContext,
    rootNavigator: RootNavigator,
) : RootComponent,
    ComponentContext by context {

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        rootNavigator.stack

    @Serializable
    internal sealed interface Config {

        @Serializable
        data object Hello : Config

        @Serializable
        data class Greeting(val name: String) : Config
    }
}
