import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask
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
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.internal.work.WorkerLeaseService
import org.gradle.invocation.DefaultGradle


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
                    importerTask.configure {
                        classpath.from(component.compileClasspath)
                        compileApp.set(File("${project.layout.buildDirectory.get()}/import/compile-app"))
                        compileDependencies.set(File("${project.layout.buildDirectory.get()}/import/compile-dependencies"))
                        compileRuntimeApp.set(File("${project.layout.buildDirectory.get()}/import/compile-runtime-app"))
                        kotlinClasses.set(File("${project.layout.buildDirectory.get()}/import/kotlin-classes"))
                        val map =
                            mutableMapOf("root" to "${project.layout.buildDirectory.get()}/import/deps-root")
                    }
                    target.tasks.withType<PackageApplication>().configureEach {
                        finalizedBy(importerTask)
                    }
                }




        }
    }

    fun registerJunitReportTask(target: Project) {
        val nameTask = "debugDebugAndroidTestImporter"
        val buildLayout = target.layout.buildDirectory

        // example configuring the Device Instrumentation test task
        target.tasks.withType<DeviceProviderInstrumentTestTask>().configureEach {
            inputs.dir(
                target.tasks.named<ImportReporterWIthInputsTask>(nameTask)
                    .flatMap { it.compileDependencies })
                .withPropertyName("compile-dependencies")
                .withNormalizer(ClasspathNormalizer::class.java)
        }

        // example configuring the Import Junit Report task
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




