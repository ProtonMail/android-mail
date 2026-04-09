import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.test")
    kotlin("android")
    id("app-config-plugin")
}

android {
    namespace = "ch.protonmail.android.e2e"
    compileSdk = AppConfiguration.compileSdk.get()
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = AppConfiguration.minSdk.get()
        testInstrumentationRunner = "ch.protonmail.android.e2e.runner.CucumberTestRunner"
        missingDimensionStrategy("default", "prod")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("17")
        }
    }
}

val appId = AppConfiguration.applicationId.get()

tasks.register<Exec>("runE2e") {
    description = "Installs app + test APKs, clears app data, and runs Cucumber e2e tests."
    group = "verification"
    dependsOn(":app:installProdDebug", ":e2e:installDebug")

    // Clear app data before instrumentation starts so tests begin from a logged-out state.
    commandLine(
        "sh", "-c",
        "adb shell pm clear $appId && " +
            "adb shell am instrument -w " +
            "-e features features " +
            "ch.protonmail.android.e2e/ch.protonmail.android.e2e.runner.CucumberTestRunner"
    )
}

// Proton's detekt plugin infers --jvm-target from compileSdk (→ 21), but detekt 1.23.x only supports up to 19.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
}

dependencies {
    implementation(libs.cucumber.android)
    implementation(libs.cucumber.java)
    implementation(libs.arrow.core)

    implementation(libs.androidx.test.core)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.compose.ui.test)
    implementation(libs.androidx.compose.ui.test.junit4)
}
