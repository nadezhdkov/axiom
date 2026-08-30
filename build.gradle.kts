plugins {
    id("java-library")
    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("signing")
}

// Maven groupId — must be a namespace verified on Maven Central (io.github.<user> is granted by
// owning the GitHub account). Deliberately different from the Java package names, which stay
// io.axiom.*: Central only validates the groupId, and the two are independent.
group = "io.github.nadezhdkov"
version = "0.2.0"

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "signing")

    group = "io.github.nadezhdkov"
    version = "0.2.0"

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

    // ─────────────────────────────────────────────────────
    // GitHub Packages — Subprojects
    // ─────────────────────────────────────────────────────
    // Consumed by `./gradlew publish` (all repositories) or
    // `./gradlew publishAllPublicationsToGitHubPackagesRepository` (this one only).
    // Credentials: GITHUB_ACTOR/GITHUB_TOKEN are set automatically in GitHub Actions;
    // locally, set them as env vars or pass -Pgpr.user=... -Pgpr.token=... (a PAT with
    // read:packages/write:packages scope).
    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/nadezhdkov/axiom")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                        ?: findProperty("gpr.user") as String?
                    password = System.getenv("GITHUB_TOKEN")
                        ?: findProperty("gpr.token") as String?
                }
            }
        }
    }

    dependencies {
        testImplementation(platform(rootProject.libs.junitBom))
        testImplementation(rootProject.libs.junitJupiter)
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    // ─────────────────────────────────────────────────────
    // Maven Central (via Vanniktech) — Subprojects
    // ─────────────────────────────────────────────────────
    // Credentials are never stored in this repo: the plugin reads the properties
    // `mavenCentralUsername`/`mavenCentralPassword` (put them in ~/.gradle/gradle.properties)
    // or the env vars ORG_GRADLE_PROJECT_mavenCentralUsername/...Password (used in CI).
    //
    // Signing is conditional so local builds work without a GPG key, but Maven Central REJECTS
    // unsigned artifacts — an actual release requires the key to be present.
    val hasSigningKey = project.hasProperty("signing.gnupg.keyName") ||
        project.hasProperty("signing.keyId") ||
        System.getenv("ORG_GRADLE_PROJECT_signingKey") != null

    if (project.hasProperty("signing.gnupg.keyName")) {
        signing {
            useGpgCmd()
        }
    }

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
            url.set("https://github.com/nadezhdkov/axiom")

            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    distribution.set("repo")
                }
            }

            // Required by Maven Central validation — a release without it is rejected.
            developers {
                developer {
                    id.set("nadezhdkov")
                    name.set("nadezhdkov")
                    url.set("https://github.com/nadezhdkov")
                }
            }

            scm {
                url.set("https://github.com/nadezhdkov/axiom")
                connection.set("scm:git:https://github.com/nadezhdkov/axiom.git")
                developerConnection.set("scm:git:ssh://git@github.com/nadezhdkov/axiom.git")
            }
        }
    }
}
