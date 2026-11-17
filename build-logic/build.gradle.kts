import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

dependencies {
    // Exposes the `libs` version catalog to the precompiled script plugins
    // via the generated `LibrariesForLibs` accessor.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    // Plugin markers so the precompiled script plugins can apply these by id.
    // Versions come solely from the version catalog.
    implementation(libs.plugins.android.multiplatform.library.toDep())
    implementation(libs.plugins.compose.compiler.toDep())
    implementation(libs.plugins.compose.multiplatform.toDep())
    implementation(libs.plugins.detekt.toDep())
    implementation(libs.plugins.kotlin.multiplatform.toDep())
    implementation(libs.plugins.kover.toDep())
    implementation(libs.plugins.metro.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
