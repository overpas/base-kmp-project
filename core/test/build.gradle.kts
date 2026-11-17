plugins {
    id("kmp-lib")
}

kotlin {

    android {
        namespace = "by.overpas.template.core.test"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
