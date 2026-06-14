import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ktlint)
}

// Load local.properties for API keys
val localProps = Properties().also { props ->
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { props.load(it) }
}

android {
    namespace = "com.wallora.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wallora.app"
        minSdk = 31
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API keys from local.properties — empty strings are safe defaults
        buildConfigField("String", "PEXELS_API_KEY",
            "\"${localProps.getProperty("PEXELS_API_KEY", "")}\"")
        buildConfigField("String", "UNSPLASH_ACCESS_KEY",
            "\"${localProps.getProperty("UNSPLASH_ACCESS_KEY", "")}\"")
        buildConfigField("String", "WALLHAVEN_API_KEY",
            "\"${localProps.getProperty("WALLHAVEN_API_KEY", "")}\"")
        buildConfigField("String", "PIXABAY_API_KEY",
            "\"${localProps.getProperty("PIXABAY_API_KEY", "")}\"")

        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    signingConfigs {
        create("release") {
            // Priority: local.properties → env vars (GitHub Actions secrets) → debug fallback.
            // The env-var path is used by CI via the "Decode release keystore" workflow step.
            val ksFile = localProps.getProperty("RELEASE_STORE_FILE")
                ?: System.getenv("RELEASE_STORE_FILE") ?: ""
            if (ksFile.isNotEmpty()) {
                storeFile = file(ksFile)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                    ?: System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                    ?: System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
                    ?: System.getenv("RELEASE_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val relCfg = signingConfigs.getByName("release")
            signingConfig = if (relCfg.storeFile != null) relCfg
                            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ktlint {
    version.set("1.3.1")
    android.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
}

dependencies {
    // Core
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.bundles.lifecycle)
    implementation(libs.activity.compose)

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.material3.adaptive.navigation.suite)
    implementation(libs.compose.windowsizeclass)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // Coil
    implementation(libs.coil.compose)

    // Network
    implementation(libs.bundles.network)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Paging 3
    implementation(libs.bundles.paging)

    // WorkManager
    implementation(libs.workmanager.ktx)

    // DataStore
    implementation(libs.datastore.preferences)

    // Glance widget
    implementation(libs.bundles.glance)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.runtime)   // for in-memory room tests
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.junit.android)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
