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

tasks.named("build") {
    dependsOn("compileExamplesJava")
}
