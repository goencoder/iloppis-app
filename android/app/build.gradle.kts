plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "se.iloppis.app"
    compileSdk = 36

    flavorDimensions += "environment"
    productFlavors {
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            resValue("string", "app_name", "iLoppis ( Staging )")
        }
        create("production") {
            dimension = "environment"
        }
    }

    defaultConfig {
        applicationId = "se.iloppis.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true  // Generate BuildConfig.DEBUG for conditional logging
    }
    lint {
        // Disable buggy detector (crashes in Kotlin 2.1)
        disable += "NullSafeMutableLiveData"

        // Enable critical checks for store submission
        checkReleaseBuilds = true
        abortOnError = true  // Block build on any issues - maintain zero tolerance

        // Security and privacy - these are CRITICAL for store approval
        fatal += listOf(
            "SetJavaScriptEnabled",      // WebView JavaScript enabled
            "HardcodedDebugMode"         // android:debuggable="true"
        )
        error += listOf(
            "VulnerableCordovaVersion",
            "ExportedService",           // Exported components without permissions
            "ExportedReceiver",
            "ExportedContentProvider",
            "PermissionImpliesUnsupportedChromeOsHardware"
        )

        // Warnings we care about
        warning += listOf(
            "UnsafeOptInUsageError",     // Experimental APIs
            "ObsoleteSdkInt"             // Old SDK checks
        )

        // Allow missing translations for now (can add later)
        disable += "MissingTranslation"
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.maps.compose)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.multiplatform.markdown.renderer.m3)

    // Protobuf Lite runtime (Java proto classes in src/main/java/com/iloppis/v1/)
    implementation(libs.protobuf.javalite)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Background work (offline sync)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.navigation3.ui)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
