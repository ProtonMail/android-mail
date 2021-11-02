import groovy.xml.slurpersupport.Node
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

fun Project.configureJacoco(flavor: String = "", srcFolder: String = "kotlin") {
    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = Versions.JaCoCo.tool
    }

    val taskName = if (flavor.isEmpty()) {
        "debug"
    } else {
        "${flavor}Debug"
    }

    tasks.withType<org.gradle.api.tasks.testing.Test> {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    tasks.create<JacocoReport>("jacocoTestReport") {

        reports {
            xml.required.set(true)
            html.required.set(true)
        }

        val fileFilter = listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "ch.protonmail.android.utils.nativelib",
            "**/ch/protonmail/**",
        )

        val debugTree = fileTree("$buildDir/tmp/kotlin-classes/$taskName") { exclude(fileFilter) }
        val mainSrc = "$projectDir/src/main/$srcFolder"

        sourceDirectories.setFrom(mainSrc)
        classDirectories.setFrom(debugTree)
        executionData.setFrom(fileTree(buildDir) { include(listOf("**/*.exec", "**/*.ec")) })
    }.dependsOn("test${taskName.capitalize(Locale.ENGLISH)}UnitTest")

    tasks.register("coverageReport") {
        dependsOn("jacocoTestReport")
        val reportFile = project.file("$buildDir/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
        inputs.file(reportFile)

        doLast {
            val slurper = groovy.xml.XmlSlurper()
            slurper.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            slurper.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false
            )
            val xml = slurper.parse(reportFile)
            val counter = (xml.childNodes().asSequence().first {
                (it as? Node)?.name() == "counter" && it.attributes()["type"] == "INSTRUCTION"
            } as Node)
            val missed = (counter.attributes()["missed"] as String).toLong()
            val covered = (counter.attributes()["covered"] as String).toLong()
            val total = missed + covered
            val percentage = (covered.toFloat() / total * 100).toInt()

            println("Missed %d branches".format(missed))
            println("Covered %d branches".format(covered))
            println("Total %d%%".format(percentage))
        }
    }

    tasks.register<Exec>("coberturaCoverageReport") {
        dependsOn("coverageReport")
        val outputDir = File(buildDir, "reports")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        workingDir = File(rootDir, "buildSrc")
        commandLine(
            "python3",
            "jacocoConverter.py",
            "$buildDir/reports/jacoco/jacocoTestReport/jacocoTestReport.xml",
            "$projectDir/src/main/$srcFolder"
        )
        standardOutput = FileOutputStream(File(outputDir, "cobertura-coverage.xml"))
    }
}
