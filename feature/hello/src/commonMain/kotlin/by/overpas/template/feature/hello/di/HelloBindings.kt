package by.overpas.template.feature.hello.di

import by.overpas.template.core.arch.mvi.ActionHandler
import by.overpas.template.core.arch.mvi.IntentHandler
import by.overpas.template.feature.hello.DefaultHelloComponent
import by.overpas.template.feature.hello.HelloComponent
import by.overpas.template.feature.hello.input.InputStore.Action
import by.overpas.template.feature.hello.input.InputStore.Intent
import by.overpas.template.feature.hello.input.InputStore.Label
import by.overpas.template.feature.hello.input.InputStore.Message
import by.overpas.template.feature.hello.input.InputStore.State
import by.overpas.template.feature.hello.input.handlers.CheckInputHandler
import by.overpas.template.feature.hello.input.handlers.OnGreetClickHandler
import by.overpas.template.feature.hello.input.handlers.OnNameChangeHandler
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ClassKey
import dev.zacsweers.metro.IntoMap

@BindingContainer
internal interface HelloBindings {

    @Binds
    val DefaultHelloComponent.Factory.bind: HelloComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(Intent.OnNameChange::class)
    val OnNameChangeHandler.bind: IntentHandler<State, Message, Action, Label, Intent>

    @Binds
    @IntoMap
    @ClassKey(Intent.OnGreetClick::class)
    val OnGreetClickHandler.bind: IntentHandler<State, Message, Action, Label, Intent>

    @Binds
    @IntoMap
    @ClassKey(Action.CheckInput::class)
    val CheckInputHandler.bind: ActionHandler<State, Message, Action, Label, Action>
}
