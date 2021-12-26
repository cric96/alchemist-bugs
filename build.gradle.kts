import java.io.ByteArrayOutputStream
import java.awt.GraphicsEnvironment

val scalaLib: String = "2.13.2"
val scala: String = "2.13"

plugins {
    application
    scala
    kotlin("jvm")
}

repositories {
    mavenCentral()
}
/*
 * Only required if you plan to use Protelis, remove otherwise
 */
sourceSets {
    main {
        resources {
            srcDir("src/main/protelis")
        }
    }
}

fun onJava16AndAbove(body: () -> Unit) {
    if (JavaVersion.current() >= JavaVersion.VERSION_16) {
        body()
    }
}

dependencies {
    implementation("it.unibo.alchemist:alchemist:_")
    implementation("it.unibo.alchemist:alchemist-incarnation-protelis:_")
    implementation("it.unibo.alchemist:alchemist-incarnation-scafi:_")
    implementation("it.unibo.alchemist:alchemist-swingui:_")

    implementation(kotlin("stdlib-jdk8"))
    onJava16AndAbove {
        runtimeOnly("com.google.inject:guice:_")
        runtimeOnly("org.eclipse.xtext:org.eclipse.xtext:_")
        runtimeOnly("org.eclipse.xtext:org.eclipse.xtext.xbase:_")
    }
    // Scala Deps
    implementation("org.scala-lang:scala-library:$scalaLib")
    implementation("it.unibo.scafi:scafi-core_${scala}:_")
}

// Heap size estimation for batches
val maxHeap: Long? by project
val heap: Long = maxHeap ?:
if (System.getProperty("os.name").toLowerCase().contains("linux")) {
    ByteArrayOutputStream().use { output ->
        exec {
            executable = "bash"
            args = listOf("-c", "cat /proc/meminfo | grep MemAvailable | grep -o '[0-9]*'")
            standardOutput = output
        }
        output.toString().trim().toLong() / 1024
    }
        .also { println("Detected ${it}MB RAM available.") }  * 9 / 10
} else {
    // Guess 16GB RAM of which 2 used by the OS
    14 * 1024L
}
val taskSizeFromProject: Int? by project
val taskSize = taskSizeFromProject ?: 512
val threadCount = maxOf(1, minOf(Runtime.getRuntime().availableProcessors(), heap.toInt() / taskSize ))

val alchemistGroup = "Run Alchemist"
/*
 * This task is used to run all experiments in sequence
 */
val runAllGraphic by tasks.register<DefaultTask>("runAllGraphic") {
    group = alchemistGroup
    description = "Launches all simulations with the graphic subsystem enabled"
}
val runAllBatch by tasks.register<DefaultTask>("runAllBatch") {
    group = alchemistGroup
    description = "Launches all experiments"
}
/*
 * Scan the folder with the simulation files, and create a task for each one of them.
 */
File(rootProject.rootDir.path + "/src/main/yaml").listFiles()
    ?.filter { it.extension == "yml" }
    ?.sortedBy { it.nameWithoutExtension }
    ?.forEach {
        fun basetask(name: String, additionalConfiguration: JavaExec.() -> Unit = {}) = tasks.register<JavaExec>(name) {
            group = alchemistGroup
            description = "Launches graphic simulation ${it.nameWithoutExtension}"
            onJava16AndAbove {
                jvmArgs("--illegal-access=permit")
            }
            main = "it.unibo.alchemist.Alchemist"
            classpath = sourceSets["main"].runtimeClasspath
            args("-y", it.absolutePath)
            if (System.getenv("CI") == "true") {
                args("-hl", "-t", "2")
            } else {
                args("-g", "effects/${it.nameWithoutExtension}.aes")
            }
            this.additionalConfiguration()
        }
        val capitalizedName = it.nameWithoutExtension.capitalize()
        val graphic by basetask("run${capitalizedName}Graphic")
        runAllGraphic.dependsOn(graphic)
        val batch by basetask("run${capitalizedName}Batch") {
            description = "Launches batch experiments for $capitalizedName"
            jvmArgs("-XX:+AggressiveHeap")
            maxHeapSize = "${minOf(heap.toInt(), Runtime.getRuntime().availableProcessors() * taskSize)}m"
            File("data").mkdirs()
            args(
                "-e", "data/${it.nameWithoutExtension}",
                "-b",
                "-p", threadCount,
                "-i", 1
            )
        }
        runAllBatch.dependsOn(batch)
    }

