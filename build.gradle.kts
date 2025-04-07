import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    id("java") // Java support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    id("jacoco") // Code coverage
    id("maven-publish")
}

group = "com.redhat.devtools.intellij"
version = providers.gradleProperty("projectVersion").get() // Plugin version
val platformVersion = providers.gradleProperty("ideaVersion").get()

// Set the JVM language level used to build the project.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenLocal()
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
    // Local repository to publish to
    maven {
        name = "LocalRepo"
        url = uri("file://${layout.buildDirectory}/local-repository")
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(platformVersion)

        // Bundled Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        val platformBundledPlugins =  ArrayList<String>()
        platformBundledPlugins.addAll(providers.gradleProperty("platformBundledPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }.get())
        /*
         * starting from 2024.3, all json related code is know on its own plugin
         */
        if (platformVersion.startsWith("2024.3") || platformVersion.substring(2).startsWith("25")) {
            platformBundledPlugins.add("com.intellij.modules.json")
        }
        println("use bundled Plugins: $platformBundledPlugins")
        bundledPlugins(platformBundledPlugins)

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        // for local plugin -> https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin-faq.html#how-to-add-a-dependency-on-a-plugin-available-in-the-file-system
        //plugins.set(listOf(file("/path/to/plugin/")))

        testFramework(TestFrameworkType.Platform)
    }

    implementation(libs.kubernetes.client)
    implementation(libs.openshift.client)
    implementation(libs.kubernetes.httpclient.okhttp)
    implementation(libs.jackson.core)
    implementation(libs.commons.exec)
    implementation(libs.commons.lang3)
    implementation(libs.common.lang)

    // for unit tests
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.opentest4j) // known issue: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#missing-opentest4j-dependency-in-test-framework
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    test {
        useJUnit()
        systemProperty("tools.dl.path", temporaryDir)
        jvmArgs("-Djava.awt.headless=true")
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    withType<Test> {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    register("packageTests", Jar::class) {
        from(sourceSets.test.get().output)
        archiveClassifier.set("test")
    }

    printProductsReleases {
        channels = listOf(ProductRelease.Channel.EAP)
        types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
        untilBuild = provider { null }
    }

}

val testJarFile = file("${layout.buildDirectory.get().asFile.absolutePath}/libs/intellij-common-${version}-test.jar")
val testArtifact = artifacts.add("archives", testJarFile) {
    type = "test"
    classifier = "test"
    builtBy("packageTests")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["intellijPlatform"])
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            artifact(testArtifact)
            pom {
                name.set("IntelliJ Common")
                description.set("Common utilities for IntelliJ plugins")
                url.set("https://github.com/redhat-developer/intellij-common")
                licenses {
                    license {
                        name.set("Eclipse Public License 2.0")
                        url.set("https://www.eclipse.org/legal/epl-v20.html")
                    }
                }
                developers {
                    developer {
                        name.set("Red Hat Developer")
                        email.set("devtools-team@redhat.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/redhat-developer/intellij-common.git")
                    developerConnection.set("scm:git:ssh://git@github.com:redhat-developer/intellij-common.git")
                    url.set("https://github.com/redhat-developer/intellij-common/")
                }
            }
        }
    }
    repositories {
        maven {
            val baseUrl = layout.buildDirectory.dir("repository/").get()
            val releasesRepoUrl = baseUrl.dir("releases")
            val snapshotsRepoUrl = baseUrl.dir("snapshots")
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
        }
    }
}
