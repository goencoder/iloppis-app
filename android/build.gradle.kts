// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.owasp.dependencycheck") version "11.1.1"
}

// OWASP Dependency Check configuration for security scanning
dependencyCheck {
    autoUpdate = true
    format = "HTML"
    outputDirectory = "build/reports/dependency-check"

    // Suppress false positives (add as needed)
    suppressionFile = file("dependency-check-suppressions.xml").takeIf { it.exists() }?.absolutePath

    // Fail build on CVSS score >= 7.0 (High severity)
    failBuildOnCVSS = 7.0f

    analyzers.apply {
        assemblyEnabled = false  // .NET not used
        nuspecEnabled = false    // .NET not used
    }
}

buildscript {
    dependencies {
        classpath(libs.secrets.gradle.plugin)
    }
}
