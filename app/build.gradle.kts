plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

configureJacoco(flavor = "dev")
setAsHiltModule()

android {
    compileSdk = Config.compileSdk
    buildToolsVersion = Config.buildTools

    defaultConfig {
        applicationId = Config.applicationId
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
        versionCode = Config.versionCode
        versionName = Config.versionName
        testInstrumentationRunner = Config.testInstrumentationRunner
    }

    buildTypes {
        debug {
            isDebuggable = true
            isTestCoverageEnabled = true
            postprocessing {
                isObfuscate = false
                isOptimizeCode = false
                isRemoveUnusedCode = false
                isRemoveUnusedResources = false
            }
        }
        release {
            isDebuggable = false
            isTestCoverageEnabled = false
            postprocessing {
                isObfuscate = false
                isOptimizeCode = true
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
            }
        }
    }

    flavorDimensions.add("default")
    productFlavors {
        create("dev") {
            applicationIdSuffix = ".dev"
            val gitHash = "git rev-parse --short HEAD".runCommand(workingDir = rootDir)
            versionNameSuffix = "-dev (${gitHash})"
        }
        create("prod") {
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildFeatures {
        compose = true
        // viewBinding = true // required by Core presentation
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.AndroidX.compose
    }

    hilt {
        enableAggregatingTask = true
    }

    packagingOptions {
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin", "src/uiTest/kotlin")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation(Dependencies.appLibs)

    implementation(project(":mail-message"))
    debugImplementation(Dependencies.appDebug)

    kapt(Dependencies.appAnnotationProcessors)

    testImplementation(Dependencies.testLibs)
    androidTestImplementation(Dependencies.androidTestLibs)
}
