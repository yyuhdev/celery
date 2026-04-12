plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "libs"

include("celery")
include("core")
include("celery-api")
include("celery-platform:celery-mongodb")
