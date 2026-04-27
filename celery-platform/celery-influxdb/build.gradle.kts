plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.influxdb)
    api(project(":celery-api"))
}
