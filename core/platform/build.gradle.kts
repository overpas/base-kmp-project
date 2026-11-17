plugins {
    id("kmp-di")
    id("kmp-lib")
}

kotlin {

    android {
        namespace = "by.overpas.template.core.platform"
    }

    sourceSets {
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(projects.core.test)

            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.decompose)
            implementation(libs.kotlinx.serialization.json)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}
