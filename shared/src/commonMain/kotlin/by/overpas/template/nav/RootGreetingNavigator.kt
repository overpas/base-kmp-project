package by.overpas.template.nav

import by.overpas.template.DefaultRootComponent.Config
import by.overpas.template.di.RootScope
import by.overpas.template.feature.greeting.GreetingNavigator
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(RootScope::class)
internal class RootGreetingNavigator(private val stackNavigation: StackNavigation<Config>) :
    GreetingNavigator {

    override fun navigateBack() {
        stackNavigation.pop()
    }
}
