plugins {
    id("revived.bundle-conventions")
}

dependencies {
    api(libs.jetbrainsannotations)
    api(libs.mongo)
    api(libs.influxdb)
    api(libs.nats)
    api(libs.lettuce)
    api(libs.gson)
    api(libs.protobuf.java)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mongodb)
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

