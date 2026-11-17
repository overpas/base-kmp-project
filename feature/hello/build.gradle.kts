plugins {
    id("kmp-compose-lib")
    id("kmp-di")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {

    android {
        namespace = "by.overpas.template.feature.hello"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
        }
        commonMain.dependencies {
            implementation(projects.core.arch)
            implementation(projects.core.design)
            implementation(projects.core.strings)
            implementation(projects.feature.hello.tags)

            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.decompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.extensions.coroutines)
            implementation(libs.mvikotlin.main)
        }
        commonTest.dependencies {
            implementation(projects.core.test)
            implementation(projects.core.uiTest)
            implementation(projects.feature.hello.uiTest)

            implementation(libs.ultron.compose)
        }
    }
}
