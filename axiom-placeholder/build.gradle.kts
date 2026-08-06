plugins {
    java
}

dependencies {
    implementation(project(":axiom-core"))
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
