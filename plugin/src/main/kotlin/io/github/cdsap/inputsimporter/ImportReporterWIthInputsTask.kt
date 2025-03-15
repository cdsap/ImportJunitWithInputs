package io.github.cdsap.inputsimporter

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.JarFile
import kotlin.sequences.forEach


@CacheableTask
abstract class ImportReporterWIthInputsTask : DefaultTask() {

    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Classpath
    abstract val javaClasses: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val compileApp: DirectoryProperty

    @get:OutputDirectory
    abstract val compileDependencies: DirectoryProperty

    @get:OutputDirectory
    abstract val compileRuntimeApp: DirectoryProperty

    @get:OutputDirectory
    abstract val kotlinClasses: DirectoryProperty

    @get:OutputDirectory
    abstract val testJavaClasses: DirectoryProperty

    @TaskAction
    fun traverse() {
        val filePathInputs = mutableListOf<String>()
        compileApp.get().asFile.deleteRecursively()
        compileDependencies.get().asFile.deleteRecursively()
        compileRuntimeApp.get().asFile.deleteRecursively()
        kotlinClasses.get().asFile.deleteRecursively()
        testJavaClasses.get().asFile.deleteRecursively()

        compileApp.get().asFile.mkdir()
        compileDependencies.get().asFile.mkdir()
        compileRuntimeApp.get().asFile.mkdir()
        kotlinClasses.get().asFile.mkdir()
        testJavaClasses.get().asFile.mkdir()

        classpath.forEach {
            traverse(it, filePathInputs)
        }

        filePathInputs.filter { it.contains(COMPILE_AND_RUNTIME_NOT_NAMESPACED_R_CLASS_JAR) }
            .forEach {
                val libraryName = extractLibraryName(it)
                compileRuntimeApp.get().dir("$libraryName").asFile.mkdir()
                extractClasses(it, libraryName, compileRuntimeApp)
            }

        filePathInputs.filter { it.contains(COMPILE_APP_CLASSES_JAR) }.forEach {
            val libraryName = extractLibraryName(it)
            compileApp.get().dir("$libraryName").asFile.mkdir()
            extractClasses(it, libraryName, compileApp)
        }


        filePathInputs.filter { it.contains(COMPILE_LIBRARY_CLASSES_JAR) }.forEach {
            val libraryName = extractLibraryName(it)
            compileDependencies.get().dir("$libraryName").asFile.mkdir()
            extractClasses(it, libraryName, compileDependencies)
        }

        filePathInputs.filter { it.contains(KOTLIN_CLASSES) }.forEach {
            val path = File(it).name.replace(KOTLIN_CLASSES, "")
            copyRecursively(
                File(it).toPath(),
                File("${kotlinClasses.get().asFile}/$path").toPath()
            )
        }

        copyRecursively(
            File(javaClasses.asPath).toPath(),
            testJavaClasses.get().asFile.toPath()
        )
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

    fun extractClasses(
        string: String,
        libraryName: String?,
        inputDirectory: DirectoryProperty
    ) {
        JarFile(string).use { jar ->
            jar.entries().asSequence().filter { it.name.contains(".class") }.forEach { entry ->
                val outputFile = File("${inputDirectory.get()}/$libraryName", entry.name)

                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile.mkdirs()
                    jar.getInputStream(entry).use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }


    fun copyRecursively(source: Path, target: Path) {
        Files.walkFileTree(source, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(
                dir: Path,
                attrs: BasicFileAttributes?
            ): FileVisitResult {
                Files.createDirectories(target.resolve(source.relativize(dir)))
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                Files.copy(
                    file,
                    target.resolve(source.relativize(file)),
                    StandardCopyOption.REPLACE_EXISTING
                )
                return FileVisitResult.CONTINUE
            }
        })
    }

    companion object {
        const val KOTLIN_CLASSES = "kotlin-classes"

        const val COMPILE_AND_RUNTIME_NOT_NAMESPACED_R_CLASS_JAR =
            "compile_and_runtime_not_namespaced_r_class_jar"

        const val COMPILE_APP_CLASSES_JAR = "compile_app_classes_jar"

        const val COMPILE_LIBRARY_CLASSES_JAR = "compile_library_classes_jar"

    }
}

