plugins {
    id("revived.commons-conventions")
    application
}

application {
    mainClass.set("de.yyuh.example.CeleryExample")
}

dependencies {
    api(project(":celery"))
    api(project(":celery-platform:celery-mongodb"))
    api(project(":celery-platform:celery-redis"))
    api(project(":celery-platform:celery-redis-cache"))
    api(project(":celery-platform:celery-nats"))
    api(project(":celery-platform:celery-nats-cluster"))
    api(project(":celery-platform:celery-influxdb"))
    api(project(":celery-platform:celery-redis-cluster"))
    api(project(":celery-platform:celery-redis-cache-cluster"))

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

