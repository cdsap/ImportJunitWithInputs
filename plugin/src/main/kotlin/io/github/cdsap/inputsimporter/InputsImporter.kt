package io.github.cdsap.inputsimporter

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.kotlin.dsl.named

class InputsImporter {
    companion object {
        fun add(
            target: Project,
            nameTask: String,
            task: Task
        ) {
            task.inputs.dir(
                target.tasks.named<ImportReporterWIthInputsTask>(nameTask)
                    .flatMap { it.compileDependencies })
                .withPropertyName("compile-dependencies")
                .withNormalizer(ClasspathNormalizer::class.java)
            task.inputs.dir(
                target.tasks.named<ImportReporterWIthInputsTask>(nameTask)
                    .flatMap { it.compileApp })
                .withPropertyName("compile-app")
                .withNormalizer(ClasspathNormalizer::class.java)
            task.inputs.dir(
                target.tasks.named<ImportReporterWIthInputsTask>(nameTask)
                    .flatMap { it.compileRuntimeApp })
                .withPropertyName("compile-runtime-app")
                .withNormalizer(ClasspathNormalizer::class.java)
        }

        fun addWithClasses(
            target: Project,
            nameTask: String,
            task: Task
        ) {
            task.inputs.dir(
                target.tasks.named<ImportReporterWIthInputsTask>(nameTask)
                    .flatMap { it.compileDependencies })
                .withPropertyName("compile-dependencies")
                .withNormalizer(ClasspathNormalizer::class.java)
            task.inputs.dir(
                target.tasks.named<ImportReporterWIthInputsTask>(nameTask)
                    .flatMap { it.compileApp })
                .withPropertyName("compile-app")
                .withNormalizer(ClasspathNormalizer::class.java)
            task.inputs.dir(
                target.tasks.named<ImportReporterWIthInputsTask>(nameTask)
                    .flatMap { it.compileRuntimeApp })
                .withPropertyName("compile-runtime-app")
                .withNormalizer(ClasspathNormalizer::class.java)
            task.inputs.dir(
                target.tasks.named<ImportReporterWIthInputsTask>(nameTask)
                    .flatMap { it.kotlinClasses }).withPropertyName("kotlin-classes")
                .withNormalizer(ClasspathNormalizer::class.java)
            task.inputs.dir(
                target.tasks.named<ImportReporterWIthInputsTask>(nameTask)
                    .flatMap { it.testJavaClasses }).withPropertyName("java-classes")
                .withNormalizer(ClasspathNormalizer::class.java)

        }
    }
}
