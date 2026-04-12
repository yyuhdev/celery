plugins {
    id("revived.commons-conventions")
}

dependencies {
    api(libs.jetbrainsannotations)
    api(libs.gson)
    api(libs.protobuf.java)
    api(project(":core"))
}

tasks.test {
    useJUnitPlatform()
}

