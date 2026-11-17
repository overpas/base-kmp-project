plugins {
    id("kmp-di")
    id("kmp-lib")
}

kotlin {

    android {
        namespace = "by.overpas.template.core.system"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}
