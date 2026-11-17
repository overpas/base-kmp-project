plugins {
    id("kmp-di")
    id("kmp-lib")
    alias(libs.plugins.sqldelight)
}

kotlin {

    android {
        namespace = "by.overpas.template.core.db"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.sqldelight.driver.android)
        }
        commonMain.dependencies {
            implementation(projects.core.arch)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.coroutines.extensions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.driver.native)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.driver.jvm)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("by.overpas.template.core.db")
        }
    }
}
