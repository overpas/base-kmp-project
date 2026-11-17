plugins {
    id("kmp-compose-lib")
    id("kmp-di")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(projects.feature.greeting.tags)
            export(projects.feature.hello.tags)
            export(libs.decompose)
            export(libs.essenty.lifecycle)
            export(libs.essenty.state.keeper)
            export(libs.essenty.instance.keeper)
            export(libs.essenty.back.handler)
        }
    }

    android {
        namespace = "by.overpas.template.shared"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
        }
        commonMain.dependencies {
            implementation(projects.core.arch)
            implementation(projects.core.design)
            implementation(projects.core.platform)
            implementation(projects.core.system)
            implementation(projects.feature.greeting)
            implementation(projects.feature.hello)

            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.mvikotlin)

            api(projects.feature.greeting.tags)
            api(projects.feature.hello.tags)

            api(libs.decompose)
        }
        commonTest.dependencies {
            implementation(projects.core.uiTest)
            implementation(projects.feature.greeting.uiTest)
            implementation(projects.feature.hello.uiTest)

            implementation(libs.mvikotlin.main)

            implementation(libs.ultron.compose)
        }
        iosMain.dependencies {
            api(libs.essenty.back.handler)
            api(libs.essenty.instance.keeper)
            api(libs.essenty.lifecycle)
            api(libs.essenty.state.keeper)
        }
    }
}
