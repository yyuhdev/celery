plugins {
    id("revived.commons-conventions")
}

dependencies {
  api(project(":celery"))
}

tasks.test {
    useJUnitPlatform()
}

