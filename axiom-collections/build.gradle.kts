plugins {
    java
}

dependencies {
    // property-based tests for HAMT invariants (path copying, structural equality, collisions)
    testImplementation(rootProject.libs.jqwik)
}

sourceSets {
    create("examples") {
        java.srcDir("examples/src/main/java")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

tasks.named("build") {
    dependsOn("compileExamplesJava")
}
