plugins {
    id("revived.bundle-conventions")
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

tasks.named("test") {
    dependsOn(tasks.named("jar"))
}

tasks.named("shadowJar") {
    dependsOn(tasks.named("jar"))
}

