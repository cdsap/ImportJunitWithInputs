/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.gradle.publish)
}

dependencies {
    compileOnly("com.android.tools.build:gradle:8.8.0")
    testImplementation(platform(libs.junit))
    testImplementation(libs.junit.jupiter)

}
version = "0.0.1"
group = "io.github.cdsap"

tasks.test {
    useJUnitPlatform()
}
gradlePlugin {
    plugins {
        register("junitImporter") {
            website = "https://github.com/cdsap/ImportJunitWithInputs"
            vcsUrl = "https://github.com/cdsap/ImportJunitWithInputs.git"
            id = "io.github.cdsap.import-inputs"
            implementationClass =
                "io.github.cdsap.inputsimporter.JunitImportReporterWithInputsPlugin"
            displayName = "Import Instrumentation test inputs"
            description = "This plugin collects the relevant AndroidTest inputs for test/tested APKs in the Instrumentation variants"

            tags = listOf("instrumentation")
        }
    }
}
