import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("kmp-lib")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = the<LibrariesForLibs>()

kotlin {
    android {
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(libs.kotlin.test)
        }
        val nonAndroidHostTest by creating {
            dependsOn(commonTest.get())
        }
        listOf("jvmTest", "iosArm64Test", "iosSimulatorArm64Test", "androidDeviceTest").forEach {
            getByName(it).dependsOn(nonAndroidHostTest)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.robolectric)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.espresso.core)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}

// // Uncomment to generate stability reports
//composeCompiler {
//    reportsDestination = layout.buildDirectory.dir("compose_compiler")
//    metricsDestination = layout.buildDirectory.dir("compose_compiler")
//}

