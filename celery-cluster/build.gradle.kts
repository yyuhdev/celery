plugins {
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.guava)
    api(project(":core"))
    api(project(":celery-api"))
}
