buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.Gradle.androidGradlePlugin}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.Gradle.kotlinGradlePlugin}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Versions.Gradle.hiltAndroidGradlePlugin}")
        classpath("org.jacoco:org.jacoco.core:${Versions.Gradle.jacocoGradlePlugin}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    // Room 2.3 use a jdbc not compatible with arm so use the updated one to support
    // arm build. Room 2.4 should fix this issue (not stable yet)
    configurations.all {
        resolutionStrategy {
            force("org.xerial:sqlite-jdbc:3.34.0")
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

plugins {
    id("me.proton.core.gradle-plugins.detekt") version Versions.Gradle.protonDetektPlugin
}

kotlinCompilerArgs(
    // Enables experimental Coroutines (runBlockingTest).
    "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    // Enables experimental Time (Turbine).
    "-Xopt-in=kotlin.time.ExperimentalTime"
)

fun Project.kotlinCompilerArgs(vararg extraCompilerArgs: String) {
    for (sub in subprojects) {
        sub.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions { freeCompilerArgs = freeCompilerArgs + extraCompilerArgs }
        }
    }
}
