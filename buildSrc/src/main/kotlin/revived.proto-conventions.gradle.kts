plugins {
    id("revived.commons-conventions")
}

// Source generated proto files from the proto submodule
sourceSets {
    main {
        java.srcDir("src/main")
        java.srcDir("${rootProject.projectDir}/proto/gen/java")
    }
}

// Register or find the proto generation task at root level
val generateProtoTask = rootProject.tasks.findByName("generateProto") 
    ?: rootProject.tasks.register<Exec>("generateProto") {
        workingDir(rootProject.projectDir.resolve("proto"))
        commandLine("buf", "generate", "proto")
        
        // Only run if proto files have changed
        inputs.dir(rootProject.projectDir.resolve("proto/proto"))
        outputs.dir(rootProject.projectDir.resolve("proto/gen"))
    }.get()

tasks.named("compileJava") {
    dependsOn(generateProtoTask)
}
