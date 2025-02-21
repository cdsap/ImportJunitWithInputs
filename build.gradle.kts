plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    id("io.github.cdsap.gradleprocess") version "0.1.3"
    id("io.github.cdsap.kotlinprocess") version "0.1.7"
}

