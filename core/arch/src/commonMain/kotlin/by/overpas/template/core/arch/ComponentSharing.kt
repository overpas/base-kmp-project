package by.overpas.template.core.arch

import kotlinx.coroutines.flow.SharingStarted

val componentSharing: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L)
