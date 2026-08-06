plugins {
    id("java-library")
    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("signing")
}

group = "io.axiom"
version = "0.1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "signing")

    group = "io.axiom"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.matching { it.name == "generateMetadataFileForMavenPublication" }.configureEach {
        dependsOn(tasks.matching { it.name == "plainJavadocJar" })
        dependsOn(tasks.matching { it.name == "plainSourcesJar" })
    }

    dependencies {
        testImplementation(platform(rootProject.libs.junitBom))
        testImplementation(rootProject.libs.junitJupiter)
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    // ─────────────────────────────────────────────────────
    // Maven Central (via Vanniktech) — Subprojects
    // ─────────────────────────────────────────────────────
    val hasSigningKey = project.hasProperty("signing.gnupg.keyName") ||
        project.hasProperty("signing.keyId") ||
        System.getenv("ORG_GRADLE_PROJECT_signingKey") != null

    mavenPublishing {
        publishToMavenCentral()
        if (hasSigningKey) signAllPublications()

        coordinates(
            project.group.toString(),
            project.name,
            project.version.toString()
        )

        pom {
            name.set("Axiom - ${project.name}")
            description.set("Axiom module: ${project.name}.")
            inceptionYear.set("2026")
            url.set("https://github.com/rickmvi/axiom")

            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    distribution.set("repo")
                }
            }

            scm {
                url.set("https://github.com/rickmvi/axiom")
                connection.set("scm:git:https://github.com/rickmvi/axiom.git")
                developerConnection.set("scm:git:ssh://git@github.com/rickmvi/axiom.git")
            }
        }
    }
}
