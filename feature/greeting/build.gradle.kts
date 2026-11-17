plugins {
    id("kmp-compose-lib")
    id("kmp-di")
}

kotlin {

    android {
        namespace = "by.overpas.template.feature.greeting"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
        }
        commonMain.dependencies {
            implementation(projects.core.design)
            implementation(projects.core.strings)
            implementation(projects.feature.greeting.tags)

            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.decompose)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(projects.core.uiTest)
            implementation(projects.feature.greeting.uiTest)

            implementation(libs.ultron.compose)
        }
    }
}
