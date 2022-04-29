import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

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
}

plugins {
    id("me.proton.core.gradle-plugins.detekt") version Versions.Gradle.protonDetektPlugin
    id("com.github.ben-manes.versions") version Versions.Gradle.benManesVersionsPlugin
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

setupDependenciesPlugin()

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

fun Project.setupDependenciesPlugin() {
    // https://github.com/ben-manes/gradle-versions-plugin
    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
