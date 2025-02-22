import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

@CacheableTask
abstract class ImportReporterWIthInputsTask : DefaultTask() {

    @get:Classpath
    abstract val rootComponent: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val compileApp: DirectoryProperty

    @get:OutputDirectory
    abstract val compileDependencies: DirectoryProperty

    @get:OutputDirectory
    abstract val compileRuntimeApp: DirectoryProperty

    @get:OutputDirectory
    abstract val dependencies: DirectoryProperty

    @get:OutputDirectory
    abstract val kotlinClasses: DirectoryProperty

    @get:OutputDirectory
    abstract val transforms: DirectoryProperty

    val inputs = mutableMapOf<String, String>()

    @TaskAction
    fun traverse() {
        compileApp.get().asFile.deleteRecursively()
        compileDependencies.get().asFile.deleteRecursively()
        compileRuntimeApp.get().asFile.deleteRecursively()
        dependencies.get().asFile.deleteRecursively()
        kotlinClasses.get().asFile.deleteRecursively()
        transforms.get().asFile.deleteRecursively()

        compileApp.get().asFile.mkdir()
        compileDependencies.get().asFile.mkdir()
        compileRuntimeApp.get().asFile.mkdir()
        dependencies.get().asFile.mkdir()
        kotlinClasses.get().asFile.mkdir()
        transforms.get().asFile.mkdir()

        rootComponent.forEach {
            traverse(it)
        }

        // transforms
        inputs.filter { it.key.contains("transformed") }.forEach {
            transforms.get().dir("transforms").asFile.mkdir()
            transforms.get()
                .file("transforms/${it.key.split("/").last()}-${it.value}").asFile.createNewFile()
        }

        // dependencies
        inputs.filter { it.key.contains("caches/modules-2/files-2.1") }.forEach {
            dependencies.get().dir("dependencies").asFile.mkdir()
            dependencies.get()
                .file("dependencies/${it.key.split("/").last()}-${it.value}").asFile.createNewFile()
        }

        // kotlin classes
        inputs.filter { it.key.contains("kotlin-classes") }.forEach {
            kotlinClasses.get().dir("kotlin-classes").asFile.mkdir()
            kotlinClasses.get().file(
                "kotlin-classes/${
                    it.key.split("/").last()
                }-${it.value}"
            ).asFile.createNewFile()
        }

        // libraries
        inputs.filter { it.key.contains("compile_library_classes_jar") }.forEach {
            // extract the library name
            val libraryName = extractLibraryName(it.key)
            compileDependencies.get().dir("compile-dependencies").asFile.mkdir()
            compileDependencies.get().dir("compile-dependencies/$libraryName").asFile.mkdir()
            compileDependencies.get().file(
                "compile-dependencies/$libraryName/${
                    it.key.split("/").last()
                }-${it.value}"
            ).asFile.createNewFile()
        }

        // libraries
        inputs.filter { it.key.contains("compile_app_classes_jar") }.forEach {
            // extract the library name
            val libraryName = extractLibraryName(it.key)
            compileApp.get().dir("compile-app").asFile.mkdir()
            compileApp.get().dir("compile-app/$libraryName").asFile.mkdir()
            compileApp.get().file(
                "compile-app/$libraryName/${
                    it.key.split("/").last()
                }-${it.value}"
            ).asFile.createNewFile()

        }
        inputs.filter { it.key.contains("compile_and_runtime_not_namespaced_r_class_jar") }
            .forEach {
                val libraryName = extractLibraryName(it.key)
                compileRuntimeApp.get().dir("compile-runtime-app").asFile.mkdir()
                compileRuntimeApp.get().dir("compile-runtime-app/$libraryName").asFile.mkdir()
                compileRuntimeApp.get().file(
                    "compile-runtime-app/$libraryName/${
                        it.key.split("/").last()
                    }-${it.value}"
                ).asFile.createNewFile()
            }


    }

    fun extractLibraryName(path: String): String? {
        val segments = path.split("/")
        val buildIndex = segments.indexOf("build")
        return if (buildIndex > 0) segments[buildIndex - 1] else null
    }


    fun traverse(file: File) {
        if (file.isDirectory) {
            file.listFiles().forEach {
                traverse(it)
            }
        } else {
            hashingInputs(file)
        }
    }

    fun hashingInputs(file: File) {
        inputs["$file"] = hashFile(file)
    }

    fun hashFile(file: File, algorithm: String = "SHA-256"): String {
        val md = MessageDigest.getInstance(algorithm)
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}


