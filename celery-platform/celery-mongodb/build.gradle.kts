plugins {
    id("java")
}

group = "de.yyuh"
version = "1.0.3-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(libs.jetbrainsannotations)
    api(libs.mongo)
    api(libs.gson)
    api(libs.protobuf.java)
    api(project(":core"))
    api(project(":celery-api"))
}

tasks.test {
    useJUnitPlatform()
}