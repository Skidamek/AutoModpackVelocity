import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-velocity") version "3.0.2"
}

repositories {
    mavenCentral()

    // PaperMC Maven (https://docs.papermc.io/velocity/dev/creating-your-first-plugin/#setting-up-the-dependency)
    exclusiveContent {
        forRepository {
            maven {
                name = "PaperMC"
                url = uri("https://repo.papermc.io/repository/maven-public/")
            }
        }
        filter {
            includeGroup("com.velocitypowered")
        }
    }
}

val pinnedVelocityVersion = "3.5.0-SNAPSHOT"
val pinnedVelocityBuild = 593

val downloadVelocityBuild = tasks.register<DownloadVelocityBuild>("downloadVelocityBuild") {
    velocityVersion.set(pinnedVelocityVersion)
    velocityBuild.set(pinnedVelocityBuild)
    outputFile.set(layout.buildDirectory.file("velocity/$pinnedVelocityVersion/$pinnedVelocityBuild/velocity.jar"))
}

dependencies {
    // Velocity
    compileOnly("com.velocitypowered:velocity-api:$pinnedVelocityVersion")
    annotationProcessor("com.velocitypowered:velocity-api:$pinnedVelocityVersion")
    compileOnly(files(downloadVelocityBuild.flatMap { it.outputFile }))

    // bStats
    implementation("org.bstats:bstats-velocity:3.2.1")

    // Additional dependencies
    compileOnly("io.netty:netty-transport:4.2.10.Final")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        // Relocate bundled dependencies to avoid conflicts with other plugins
        // that might shade the same library. Replace with your group ID.
        relocate("org.bstats", project.group.toString() + "." + project.property("id") + ".libs.bstats")

        // Strip unnecessary files from the shadow jar
        minimize()

        // Makes this the primary jar instead of adding "-all"
        archiveClassifier.set("")
    }

    runVelocity {
        // Configure the Velocity version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        velocityVersion(pinnedVelocityVersion)
        build(pinnedVelocityBuild)

        downloadPlugins {
            modrinth("loginphaseproxy", "0.2.0-beta")
        }
    }
}

val templateSource = file("src/main/templates")
val templateDest = layout.buildDirectory.dir("generated/sources/templates")
val generateTemplates = tasks.register<Copy>("generateTemplates") {
    val props = mapOf(
        "name" to project.name,
        "description" to project.description,
        "id" to project.property("id"),
        "version" to project.version,
    )
    inputs.properties(props)

    from(templateSource)
    into(templateDest)
    expand(props)
}

sourceSets.main.configure { java.srcDir(generateTemplates.map { it.outputs }) }

tasks.compileJava {
    dependsOn(generateTemplates)
}
