plugins {
    id("kmp-di")
    id("kmp-lib")
}

kotlin {

    android {
        namespace = "by.overpas.template.core.arch"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.decompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.mvikotlin)
            implementation(libs.mvikotlin.extensions.coroutines)
            implementation(libs.mvikotlin.main)
        }
        commonTest.dependencies {
            implementation(projects.core.test)

            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}
