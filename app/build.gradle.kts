import com.gradle.develocity.agent.gradle.test.ImportJUnitXmlReports
import com.gradle.develocity.agent.gradle.test.JUnitXmlDialect

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

val buildLayout = project.layout.buildDirectory
afterEvaluate {
    val syntheticImportOutput = buildLayout.file("outputs/importJUnitXml/output")
    ImportJUnitXmlReports.register(
        tasks,
        tasks.named("connectedDebugAndroidTest"),
        JUnitXmlDialect.ANDROID_CONNECTED,
    ).configure {
        dialect = JUnitXmlDialect.ANDROID_CONNECTED
        inputs.create("javaSources", "intermediates/javac/debug/compileDebugJavaWithJavac/classes")
        inputs.create(
            "javaTestSources",
            "intermediates/javac/debugAndroidTest/compileDebugAndroidTestJavaWithJavac/classes"
        )
        inputs.create("kotlinSources", "tmp/kotlin-classes/debug")
        inputs.create("kotlinTestSources", "tmp/kotlin-classes/debugAndroidTest")
        inputs.create(
            "resourcesBinary",
            "intermediates/linked_resources_binary_format/debug/processDebugResources/linked-resources-binary-format-debug.ap_"
        )
        inputs.create(
            "resourcesRClass",
            "intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/"
        )
        reports.from(fileTree("${buildLayout.get()}/outputs/androidTest-results"))
        outputs.file(syntheticImportOutput)
        doLast {
            syntheticImportOutput.get().asFile.createNewFile()
        }
    }
}

fun org.gradle.api.internal.TaskInputsInternal.create(
    propertyName: String,
    path: String
) {
    val pathProperty = File("${buildLayout.get()}/$path")
    if (pathProperty.exists()) {
        if (pathProperty.isDirectory) {
            dir(pathProperty).withPropertyName(propertyName)
                .withNormalizer(ClasspathNormalizer::class)
        } else if (pathProperty.isFile) {
            file(pathProperty).withPropertyName(propertyName)
                .withNormalizer(ClasspathNormalizer::class)
        } else {

        }
    }

}
