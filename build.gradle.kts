import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    id("java") // Java support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    id("jacoco") // Code coverage
    id("maven-publish")
    alias(libs.plugins.gradleNexusPublishPlugin)
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
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, platformVersion)

        // Bundled Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        // for local plugin -> https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin-faq.html#how-to-add-a-dependency-on-a-plugin-available-in-the-file-system
        //plugins.set(listOf(file("/path/to/plugin/")))

        instrumentationTools()

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
    testImplementation(libs.mockito.inline)
    testImplementation(libs.opentest4j) // known issue: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#missing-opentest4j-dependency-in-test-framework

}

intellijPlatform {
    buildSearchableOptions = false
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    test {
        useJUnit()
        systemProperty("tools.dl.path", temporaryDir)
        jvmArgs("-Djava.awt.headless=true")
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

    withType<GenerateModuleMetadata> {
        enabled = false
    }

    printProductsReleases {
        channels = listOf(ProductRelease.Channel.EAP)
        types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
        untilBuild = provider { null }
    }

    // infamous hack, see https://github.com/gradle-nexus/publish-plugin/issues/354
    assemble {
        doLast {
            println("************************ GENERATING FAKE JAR FOR PUBLISHING TO NEXUS *************************************************")
            file("${layout.buildDirectory.get().asFile.absolutePath}/libs/intellij-common-${version}-searchableOptions.jar").writeText("...")
        }
    }

}

nexusPublishing {
    packageGroup.set("JBoss Releases Staging Profile")
    repositories {
        create("jbossNexus") {
            nexusUrl.set(uri("https://repository.jboss.org/nexus/service/local/"))
            snapshotRepositoryUrl.set(uri("https://repository.jboss.org/nexus/content/repositories/snapshots/"))
            username.set(project.properties["nexusUser"].toString()) // defaults to project.properties["myNexusUsername"]
            password.set(project.properties["nexusPassword"].toString()) // defaults to project.properties["myNexusPassword"]
        }
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
            from(components["java"])
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
}
