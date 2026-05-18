import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest

abstract class DownloadVelocityBuild : DefaultTask() {
    @get:Input
    abstract val velocityVersion: Property<String>

    @get:Input
    abstract val velocityBuild: Property<Int>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        val version = velocityVersion.get()
        val build = velocityBuild.get()
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
        val metadataUrl = "https://fill.papermc.io/v3/projects/velocity/versions/$version/builds/$build"
        val metadata = request(client, URI.create(metadataUrl))

        @Suppress("UNCHECKED_CAST")
        val json = JsonSlurper().parseText(metadata) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val downloads = json["downloads"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val serverDownload = downloads["server:default"] as Map<String, Any?>
        val downloadUrl = serverDownload["url"] as String
        @Suppress("UNCHECKED_CAST")
        val checksums = serverDownload["checksums"] as Map<String, Any?>
        val expectedSha256 = checksums["sha256"] as String

        val target = outputFile.get().asFile
        if (target.isFile && target.sha256().equals(expectedSha256, ignoreCase = true)) {
            logger.lifecycle("Using cached Velocity $version build $build at ${target.absolutePath}")
            return
        }

        target.parentFile.mkdirs()
        val temp = temporaryDir.resolve("velocity-$version-$build.jar")
        val bytes = requestBytes(client, URI.create(downloadUrl))
        temp.writeBytes(bytes)

        val actualSha256 = temp.sha256()
        check(actualSha256.equals(expectedSha256, ignoreCase = true)) {
            "Downloaded Velocity jar SHA-256 mismatch. Expected $expectedSha256, got $actualSha256."
        }

        temp.copyTo(target, overwrite = true)
    }

    private fun request(client: HttpClient, uri: URI): String =
        client.send(
            HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .header("User-Agent", "AutoModpack Gradle build")
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        ).let { response ->
            check(response.statusCode() in 200..299) {
                "GET $uri failed with HTTP ${response.statusCode()}: ${response.body()}"
            }
            response.body()
        }

    private fun requestBytes(client: HttpClient, uri: URI): ByteArray =
        client.send(
            HttpRequest.newBuilder(uri)
                .header("User-Agent", "AutoModpack Gradle build")
                .build(),
            HttpResponse.BodyHandlers.ofByteArray(),
        ).let { response ->
            check(response.statusCode() in 200..299) {
                "GET $uri failed with HTTP ${response.statusCode()}"
            }
            response.body()
        }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
