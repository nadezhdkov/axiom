plugins {
    java
}

dependencies {
    implementation(project(":axiom-core"))
    implementation(project(":axiom-reflect"))
    // used by: io.axiom.json.internal.gson.* — engine hidden behind the internal package boundary
    implementation(libs.gson)
}

sourceSets {
    create("examples") {
        java.srcDir("examples/src/main/java")
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
}

tasks.named("build") {
    dependsOn("compileExamplesJava")
}
