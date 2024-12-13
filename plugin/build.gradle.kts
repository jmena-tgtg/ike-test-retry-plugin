import com.google.gson.Gson
import dk.tgtg.testretry.build.GradleVersionData
import dk.tgtg.testretry.build.GradleVersionsCommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    java
    groovy
    `java-gradle-plugin`
    `maven-publish`
    checkstyle
    codenarc
    `kotlin-dsl`
    signing
    id("com.gradle.plugin-publish") version "1.3.0"
    id("com.github.hierynomus.license") version "0.16.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dk.tgtg"
description = "Mitigate flaky tests by retrying tests when they fail"

val javaToolchainVersion: String? by project
val javaLanguageVersion = javaToolchainVersion?.let { JavaLanguageVersion.of(it) } ?: JavaLanguageVersion.of(21)

java {
    toolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    if (javaLanguageVersion >= JavaLanguageVersion.of(9)) {
        options.release.set(8)
    } else {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

val plugin: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations.compileOnly {
    extendsFrom(plugin)
}

dependencies {
    plugin(libs.asm)

    testImplementation(gradleTestKit())
    testImplementation(localGroovy())
    testImplementation(platform(libs.spock.bom))
    testImplementation(libs.spock.core)
    testImplementation(libs.spock.junit4)
    testImplementation(libs.nekohtml)
    testImplementation(libs.asm)
    testImplementation(libs.jetbrains.annotations)

    codenarc(libs.codenarc)
}

tasks.shadowJar {
    configurations = listOf(plugin)
    dependencies {
        include(dependency("org.ow2.asm:asm"))
    }
    relocate("org.objectweb.asm", "dk.tgtg.testretry.org.objectweb.asm")
    archiveClassifier.set("")
    into(".") {
        from(rootProject.layout.projectDirectory.file("LICENSE"))
    }
    archiveFileName.set("test-retry-tgtg-${archiveVersion.get()}.jar")
}

tasks.jar {
    enabled = false
    dependsOn(tasks.shadowJar)
}

gradlePlugin {
    website.set("https://github.com/toogoodtogo/test-retry-gradle-plugin-tgtg")
    vcsUrl.set("https://github.com/toogoodtogo/test-retry-gradle-plugin-tgtg.git")
    plugins {
        register("testRetry") {
            id = "dk.tgtg.test-retry-tgtg"
            displayName = "TooGoodToGo's fork of Gradle test retry plugin"
            description = project.description
            implementationClass = "dk.tgtg.testretry.TestRetryPlugin"
            tags.addAll("test", "flaky")
        }
    }
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(plugin)
}

license {
    header = rootProject.file("gradle/licenseHeader.txt")
    excludes(listOf("**/*.tokens", "LICENSE", "NOTICE.txt", "licenses/**"))
    mapping(
        mapOf(
            "java" to "SLASHSTAR_STYLE",
            "groovy" to "SLASHSTAR_STYLE",
            "kt" to "SLASHSTAR_STYLE"
        )
    )
    sourceSets = project.sourceSets
    strictCheck = true
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = "dk.tgtg"
            artifactId = "test-retry-tgtg"
            pom {
                name = "Test Retry Plugin TGTG"
                description = "A fork of Gradle's test-retry plugin with extended features"
                url = "https://github.com/too-good-to-go/test-retry-gradle-plugin-tgtg"
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }

    }

    repositories {
        maven("https://tgtg-artifacts-261167912015.d.codeartifact.eu-west-1.amazonaws.com/maven/tgtg-external/") {
            name = "codeArtifact"
            val codeArtifactPassword: String? by project
            if (codeArtifactPassword?.takeIf { it.isNotBlank() } == null) {
                logger.warn("No CodeArtifact token found")
            }
            credentials(PasswordCredentials::class)
        }
    }
}

signing {
    useInMemoryPgpKeys(System.getenv("PGP_SIGNING_KEY"), System.getenv("PGP_SIGNING_KEY_PASSPHRASE"))
}

tasks.withType<Sign>().configureEach {
    enabled = System.getenv("CI") != null
}

tasks.withType<Test>().configureEach {
    maxParallelForks = 4
    useJUnitPlatform()
}

tasks.withType<Test> {
    systemProperty(
        GradleVersionsCommandLineArgumentProvider.PROPERTY_NAME,
        project.findProperty("testedGradleVersion") ?: gradle.gradleVersion
    )
    systemProperty("junit4Version", libs.versions.junit4.get())
    systemProperty("junit5Version", libs.versions.junit5Jupiter.get())
    systemProperty("junitPlatformLauncherVersion", libs.versions.junitPlatformLauncher.get())
    systemProperty("mockitoVersion", libs.versions.mockito.get())
    systemProperty("spock1Version", libs.versions.spock1.get())
    systemProperty("spock2Version", libs.versions.spock2.get())
    systemProperty("testNgVersion", libs.versions.testNg.get())
}

listOf(5, 6, 7, 8).map { gradleMajorVersion ->
    tasks.register<Test>("testGradle${gradleMajorVersion}Releases") {
        jvmArgumentProviders.add(GradleVersionsCommandLineArgumentProvider {
            GradleVersionData.getReleasedVersions(
                gradleMajorVersion
            )
        })
    }
}

tasks.register<Test>("testGradleNightlies") {
    jvmArgumentProviders.add(GradleVersionsCommandLineArgumentProvider(GradleVersionData::getNightlyVersions))
}

private data class VersionDownloadInfo(val version: String, val downloadUrl: String)

tasks.register<Wrapper>("nightlyWrapper") {
    group = "wrapper"
    validateDistributionUrl = true
    doFirst {
        val jsonText = URI("https://services.gradle.org/versions/nightly").toURL().readText()
        val versionInfo = Gson().fromJson(jsonText, VersionDownloadInfo::class.java)
        distributionUrl = versionInfo.downloadUrl
    }
}
