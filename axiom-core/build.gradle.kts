plugins {
    java
}

sourceSets {
    create("examples") {
        java.srcDir("examples/src/main/java")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

tasks.named("compileJava") {
    // examples/ must compile as part of the normal build so it can never drift from the API (axiom.md §12)
}

tasks.named("build") {
    dependsOn("compileExamplesJava")
}
