import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("code-coverage")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("static-analysis")
}

val libs = the<LibrariesForLibs>()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    applyDefaultHierarchyTemplate()

    iosArm64()
    iosSimulatorArm64()

    jvm()

    android {
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(providers.gradleProperty("jdk.version").get())
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        val uiTest = getByName("commonTest") {
            kotlin.setSrcDirs(listOf("src/uiTest/kotlin"))
        }
        val nonDeviceTest by creating {
            kotlin.setSrcDirs(listOf("src/commonTest/kotlin"))
            dependsOn(uiTest)
        }
        listOf("jvmTest", "iosArm64Test", "iosSimulatorArm64Test", "androidHostTest").forEach {
            getByName(it).dependsOn(nonDeviceTest)
        }
    }
}
