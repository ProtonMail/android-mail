import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

configureJacoco(flavor = "dev")
setAsHiltModule()

val privateProperties = Properties().apply {
    try {
        load(FileInputStream("private.properties"))
    } catch (exception: java.io.FileNotFoundException) {
        // Provide empty properties to allow the app to be built without secrets
        Properties()
    }
}

val sentryDSN: String = privateProperties.getProperty("sentryDSN") ?: ""
val proxyToken: String? = privateProperties.getProperty("PROXY_TOKEN")

android {
    compileSdk = Config.compileSdk

    defaultConfig {
        applicationId = Config.applicationId
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
        versionCode = Config.versionCode
        versionName = Config.versionName
        testInstrumentationRunner = Config.testInstrumentationRunner

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"] = "true"
            }
        }

        buildConfigField("String", "SENTRY_DSN", sentryDSN.toBuildConfigValue())
        buildConfigField("String", "PROXY_TOKEN", proxyToken.toBuildConfigValue())
        buildConfigField("String", "HUMAN_VERIFICATION_HOST", "verify.protonmail.com".toBuildConfigValue())
    }

    signingConfigs {
        register("release") {
            storeFile = file("$rootDir/keystore/ProtonMail.keystore")
            storePassword = "${privateProperties["keyStorePassword"]}"
            keyAlias = "ProtonMail"
            keyPassword = "${privateProperties["keyStoreKeyPassword"]}"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isTestCoverageEnabled = false
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
                file("proguard").listFiles()?.forEach { proguardFile(it) }
            }
            signingConfig = signingConfigs["release"]
        }
    }

    flavorDimensions.add("default")
    productFlavors {
        val gitHash = "git rev-parse --short HEAD".runCommand(workingDir = rootDir)
        create("dev") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev+$gitHash"
            buildConfigField("Boolean", "USE_DEFAULT_PINS", "false")
            buildConfigField("String", "HOST", "\"proton.black\"")
            buildConfigField("String", "HUMAN_VERIFICATION_HOST", "\"verify.proton.black\"")
        }
        create("alpha") {
            applicationIdSuffix = ".alpha"
            versionNameSuffix = "-alpha.${Config.versionCode}+$gitHash"
            buildConfigField("Boolean", "USE_DEFAULT_PINS", "true")
            buildConfigField("String", "HOST", "\"protonmail.ch\"")
        }
        create("prod") {
            buildConfigField("Boolean", "USE_DEFAULT_PINS", "true")
            buildConfigField("String", "HOST", "\"protonmail.ch\"")
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
        getByName("androidTest").assets.srcDirs("src/uiTest/assets")
        getByName("dev").res.srcDirs("src/dev/res")
        getByName("alpha").res.srcDirs("src/alpha/res")
    }
}

dependencies {
    implementation(files("../proton-libs/gopenpgp/gopenpgp.aar"))
    implementation(Dependencies.appLibs)

    implementation(project(":mail-pagination"))
    implementation(project(":mail-message"))
    implementation(project(":mail-conversation"))
    implementation(project(":mail-mailbox"))
    implementation(project(":mail-settings"))
    debugImplementation(Dependencies.appDebug)

    kapt(Dependencies.appAnnotationProcessors)

    testImplementation(Dependencies.testLibs)
    testImplementation(project(":test-data"))

    androidTestImplementation(Dependencies.androidTestLibs)
}

fun String?.toBuildConfigValue() = if (this != null) "\"$this\"" else "null"
