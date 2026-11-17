@file:OptIn(RequiresIdeSupport::class)

import dev.zacsweers.metro.gradle.RequiresIdeSupport

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("dev.zacsweers.metro")
}

metro {
    generateAssistedFactories.set(true)
}
