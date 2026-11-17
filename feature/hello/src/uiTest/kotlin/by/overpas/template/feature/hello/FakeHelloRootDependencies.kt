package by.overpas.template.feature.hello

import by.overpas.template.feature.hello.di.HelloRootDependencies

class FakeHelloRootDependencies(
    override val helloNavigator: HelloNavigator = FakeHelloNavigator(),
) : HelloRootDependencies
