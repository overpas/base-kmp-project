plugins {
    id("kmp-compose-lib")
}

kotlin {

    android {
        namespace = "by.overpas.template.feature.greeting.ui.test"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.uiTest)
            implementation(projects.feature.greeting)
            implementation(projects.feature.greeting.tags)

            implementation(libs.compose.foundation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)

            implementation(libs.compose.ui.test)
            implementation(libs.ultron.compose)
        }
    }
}
