plugins {
    id("kmp-lib")
}

kotlin {

    android {
        namespace = "by.overpas.template.core.ui.test.gallery"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.activity)
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.androidx.test.runner)
            implementation(libs.junit)
        }
        androidMain.dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.uiautomator)
            implementation(libs.junit)
        }
    }
}
