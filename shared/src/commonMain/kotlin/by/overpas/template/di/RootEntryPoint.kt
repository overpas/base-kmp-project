package by.overpas.template.di

import by.overpas.template.RootComponent
import com.arkivanov.decompose.ComponentContext

interface RootEntryPoint {

    val rootComponent: RootComponent

    fun interface Factory {

        fun create(componentContext: ComponentContext): RootEntryPoint
    }
}
