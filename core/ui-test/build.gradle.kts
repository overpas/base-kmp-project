plugins {
    id("kmp-lib")
    alias(libs.plugins.compose.compiler)
}

kotlin {

    android {
        namespace = "by.overpas.template.core.ui.test"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.robolectric.annotations)
            implementation(libs.ultron.allure)
        }
        commonMain.dependencies {
            implementation(libs.mvikotlin)

            implementation(libs.compose.ui.test)
            implementation(libs.ultron.compose)
        }
        jvmMain.dependencies {
            implementation(libs.allure.java.commons)
        }
    }
}
