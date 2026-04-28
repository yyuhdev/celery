plugins {
    id("revived.bundle-conventions")
}

dependencies {
    api(project(":celery-api"))
    api(libs.lettuce)
}
