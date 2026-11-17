plugins {
    id("kmp-lib")
}

kotlin {

    android {
        namespace = "by.overpas.template.core.ui.test.camera"
    }

    sourceSets {
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.lifecycle.runtime)

            implementation(libs.androidx.test.rules)
            implementation(libs.androidx.test.runner)
            implementation(libs.junit)
        }
        androidMain.dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.junit)
        }
    }
}
