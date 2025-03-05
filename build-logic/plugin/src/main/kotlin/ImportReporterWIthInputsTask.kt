import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

@CacheableTask
abstract class ImportReporterWIthInputsTask : DefaultTask() {

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val compileApp: DirectoryProperty

    @get:OutputDirectory
    abstract val compileDependencies: DirectoryProperty

    @get:OutputDirectory
    abstract val compileRuntimeApp: DirectoryProperty

    @get:OutputDirectory
    abstract val kotlinClasses: DirectoryProperty


    @TaskAction
    fun traverse() {
        val filePathInputs = mutableListOf<String>()
        compileApp.get().asFile.deleteRecursively()
        compileDependencies.get().asFile.deleteRecursively()
        compileRuntimeApp.get().asFile.deleteRecursively()
        kotlinClasses.get().asFile.deleteRecursively()
        compileApp.get().asFile.mkdir()

        compileDependencies.get().asFile.mkdir()
        compileRuntimeApp.get().asFile.mkdir()
        kotlinClasses.get().asFile.mkdir()
        classpath.forEach {
            traverse(it, filePathInputs)
        }

        // libraries
        filePathInputs.filter { it.contains("compile_library_classes_jar") }.forEach {
            // extract the library name
            val libraryName = extractLibraryName(it)
            compileDependencies.get().dir("$libraryName").asFile.mkdir()

            val oldFile = File(it)
            val newFile = File("${compileDependencies.get()}/$libraryName/${it.split("/").last()}")
            oldFile.copyTo(newFile)
        }

        // kotlin classes
        filePathInputs.filter { it.contains("kotlin-classes") }.forEach {
            // todo
        }

        // libraries
        filePathInputs.filter { it.contains("compile_app_classes_jar") }.forEach {
            // todo

        }
        filePathInputs.filter { it.contains("compile_and_runtime_not_namespaced_r_class_jar") }
            .forEach {
                // todo
            }


    }

    fun extractLibraryName(path: String): String? {
        val segments = path.split("/")
        val buildIndex = segments.indexOf("build")
        return if (buildIndex > 0) segments[buildIndex - 1] else null
    }


    fun traverse(file: File, filePathInputs: MutableList<String>) {
        if (file.isDirectory) {
            file.listFiles().forEach {
                traverse(it, filePathInputs)
            }
        } else {
            filePathInputs.add(file.path)
        }
    }

}


