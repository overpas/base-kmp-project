import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("code-coverage")
    id("static-analysis")
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.metro)
}

val jdkVersion = properties["jdk.version"].toString()
val appVersionCode = properties["app.version.code"].toString().toInt()
val appVersionName = properties["app.version.name"].toString()

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jdkVersion)
    }
}

dependencies {
    implementation(projects.core.platform)
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.decompose)

    debugImplementation(libs.compose.ui.tooling)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(projects.core.uiTest)
    androidTestImplementation(projects.core.uiTest.camera)
    androidTestImplementation(projects.core.uiTest.gallery)
    androidTestImplementation(projects.feature.greeting)
    androidTestImplementation(projects.feature.greeting.uiTest)
    androidTestImplementation(projects.feature.hello)
    androidTestImplementation(projects.feature.hello.uiTest)

    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.ultron.allure)
    androidTestImplementation(libs.ultron.compose)

    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)
}

android {
    namespace = "by.overpas.template"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "by.overpas.template"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.target.sdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "com.atiurin.ultron.allure.UltronAllureTestRunner"
    }
    testOptions {
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(jdkVersion)
        targetCompatibility = JavaVersion.toVersion(jdkVersion)
    }
}
