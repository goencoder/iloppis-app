import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.isFile) {
        releaseKeystorePropertiesFile.inputStream().use(::load)
    }
}

fun requiredSigningProperty(name: String): String =
    releaseKeystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: error("Missing '$name' in ${releaseKeystorePropertiesFile.absolutePath}")

gradle.taskGraph.whenReady {
    val signedReleaseRequested = allTasks.any { task ->
        task.name.matches(Regex("(bundle|assemble).+Release"))
    }
    if (signedReleaseRequested && !releaseKeystorePropertiesFile.isFile) {
        error(
            "Release signing requires ${releaseKeystorePropertiesFile.absolutePath}. " +
                "Copy keystore.properties.example, fill in the upload-key values, and keep it out of Git."
        )
    }
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
            resValue("string", "app_name", "iLoppis (Staging)")
            buildConfigField("String", "APP_ENVIRONMENT", "\"staging\"")
            buildConfigField("String", "API_BASE_URL", "\"https://iloppis-staging.fly.dev/\"")
            buildConfigField("boolean", "ENABLE_NETWORK_DEBUG_LOGGING", "true")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "APP_ENVIRONMENT", "\"production\"")
            buildConfigField("String", "API_BASE_URL", "\"https://iloppis.se/\"")
            buildConfigField("boolean", "ENABLE_NETWORK_DEBUG_LOGGING", "false")
        }
    }

    defaultConfig {
        applicationId = "se.iloppis.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0"
    }

    signingConfigs {
        create("release") {
            if (releaseKeystorePropertiesFile.isFile) {
                storeFile = file(requiredSigningProperty("storeFile"))
                storePassword = requiredSigningProperty("storePassword")
                keyAlias = requiredSigningProperty("keyAlias")
                keyPassword = requiredSigningProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
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
    implementation(libs.androidx.core.splashscreen)
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

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.browser)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.zxing.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
