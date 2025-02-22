import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.tasks.PackageApplication
import com.gradle.develocity.agent.gradle.test.ImportJUnitXmlReports
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.kotlin.dsl.named
import org.gradle.internal.extensions.stdlib.capitalized
import com.gradle.develocity.agent.gradle.test.JUnitXmlDialect


class JunitImportReporterWithInputsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.withType(AppPlugin::class.java) {
            registerImporterTask(target)
            registerJunitReportTask(target)
        }
    }

    fun registerImporterTask(
        target: Project
    ) {
        val androidComponents =
            target.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidComponents.onVariants(
            selector = androidComponents.selector().withBuildType("debug")
        ) { variant ->
            variant.nestedComponents.filter { it.name.contains("Android") }
                .forEach { component ->

                    val importerTask =
                        target.tasks.register<ImportReporterWIthInputsTask>("${variant.name}${component.name.capitalized()}Importer")

                    target.tasks.withType<PackageApplication>().configureEach {
                        finalizedBy(importerTask)
                    }
                    importerTask.configure {
                        rootComponent.from(component.compileClasspath)
                        compileApp.set(File("${project.layout.buildDirectory.get()}/import/compile-app"))
                        compileDependencies.set(File("${project.layout.buildDirectory.get()}/import/compile-dependencies"))
                        compileRuntimeApp.set(File("${project.layout.buildDirectory.get()}/import/compile-runtime-app"))
                        dependencies.set(File("${project.layout.buildDirectory.get()}/import/dependencies"))
                        kotlinClasses.set(File("${project.layout.buildDirectory.get()}/import/kotlin-classes"))
                        transforms.set(File("${project.layout.buildDirectory.get()}/import/transforms"))
                    }
                }
        }
    }

    fun registerJunitReportTask(target: Project) {
        val nameTask = "debugDebugAndroidTestImporter"
        val buildLayout = target.layout.buildDirectory
        target.afterEvaluate {
            val syntheticImportOutput = buildLayout.file("outputs/importJUnitXml/output")
            ImportJUnitXmlReports.register(
                tasks,
                tasks.named("connectedDebugAndroidTest"),
                JUnitXmlDialect.ANDROID_CONNECTED,
            ).configure {
                dialect.set(JUnitXmlDialect.ANDROID_CONNECTED)
                inputs.dir(
                    tasks.named<ImportReporterWIthInputsTask>(nameTask)
                        .flatMap { it.transforms }).withPropertyName("transforms")
                inputs.dir(
                    tasks.named<ImportReporterWIthInputsTask>(nameTask)
                        .flatMap { it.compileApp }).withPropertyName("compile-app")
                inputs.dir(
                    tasks.named<ImportReporterWIthInputsTask>(nameTask)
                        .flatMap { it.compileDependencies })
                    .withPropertyName("compile-dependencies")
                inputs.dir(
                    tasks.named<ImportReporterWIthInputsTask>(nameTask)
                        .flatMap { it.compileRuntimeApp }).withPropertyName("compile-runtime")
                inputs.dir(
                    tasks.named<ImportReporterWIthInputsTask>(nameTask)
                        .flatMap { it.dependencies }).withPropertyName("dependencies")
                inputs.dir(
                    tasks.named<ImportReporterWIthInputsTask>(nameTask)
                        .flatMap { it.kotlinClasses }).withPropertyName("kotlin-classes")
                reports.from(fileTree("${buildLayout.get()}/outputs/androidTest-results"))
                outputs.file(syntheticImportOutput)
                doLast {
                    syntheticImportOutput.get().asFile.createNewFile()
                }
            }
        }
    }
}




