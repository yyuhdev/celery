plugins {
    id("revived.commons-conventions")
}

dependencies {
    api(libs.jetbrainsannotations)
}

tasks.test {
    useJUnitPlatform()
}