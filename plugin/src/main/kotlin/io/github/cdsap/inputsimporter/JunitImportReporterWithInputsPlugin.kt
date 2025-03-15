package io.github.cdsap.inputsimporter

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.named
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.api.tasks.compile.JavaCompile


class JunitImportReporterWithInputsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.withType(AppPlugin::class.java) {
            registerImporterTask(target)
        }
    }

    fun registerImporterTask(target: Project) {
        val androidComponents =
            target.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidComponents.onVariants { variant ->
            // nestedComponent contains the variant with its unitTests, androidTests, and testFixtures
            // by default only "debug" is provided for AndroidTests, so we need to filter only by
            // "Android" to retrieve the appropriate nested component representing Android Tests
            variant.nestedComponents.filter { it.name.contains("Android") }
                .forEach { component ->
                    val componentName = component.name.capitalized()

                    val importerTask =
                        target.tasks.register<ImportReporterWIthInputsTask>("importInputs$componentName")
                    importerTask.configure {
                        // compile classpath will give us the components
                        // required for compile and runtime of the different modules and app
                        classpath.from(component.compileClasspath)
                        // Additionally, we need to track the java test sources.
                        // Kotlin test classes are included in the `kotlin-classes` compile classpath
                        javaClasses.from(
                            target.tasks.named<JavaCompile>("compile${componentName}JavaWithJavac")
                                .flatMap {
                                    it.destinationDirectory
                                }
                        )
                        compileApp.set(File("${project.layout.buildDirectory.get()}/import/compile-app"))
                        compileDependencies.set(File("${project.layout.buildDirectory.get()}/import/compile-dependencies"))
                        compileRuntimeApp.set(File("${project.layout.buildDirectory.get()}/import/compile-runtime-app"))
                        kotlinClasses.set(File("${project.layout.buildDirectory.get()}/import/kotlin-classes"))
                        testJavaClasses.set(File("${project.layout.buildDirectory.get()}/import/java-classes"))
                    }
                }
        }
    }
}
