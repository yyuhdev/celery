plugins {
    `java-library`
}

group = "club.revived"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.revived.club/releases")
}

dependencies {
    implementation("club.revived:celery:1.0.0-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets {
    main {
        java {
            srcDir("${rootProject.projectDir}/proto/gen/java")
        }
    }
}

val generateProtoTask = rootProject.tasks.findByName("generateProto") ?: rootProject.tasks.register<Exec>("generateProto") {
    workingDir(rootProject.projectDir.resolve("proto"))
    commandLine("buf", "generate", "proto")
}.get()

tasks.named("compileJava") {
    dependsOn(generateProtoTask)
}
