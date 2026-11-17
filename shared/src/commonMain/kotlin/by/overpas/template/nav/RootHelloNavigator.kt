package by.overpas.template.nav

import by.overpas.template.DefaultRootComponent.Config
import by.overpas.template.di.RootScope
import by.overpas.template.feature.hello.HelloNavigator
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(RootScope::class)
internal class RootHelloNavigator(private val stackNavigation: StackNavigation<Config>) :
    HelloNavigator {

    override fun openGreeting(name: String) {
        stackNavigation.pushNew(Config.Greeting(name))
    }
}
