plugins {
    `java-library`
    `maven-publish`
    id("eclipse")
}

group = "club.revived"
version = "1.0.3-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "libs"
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "revived"
            url = uri("http://mvn.int.revived.club/releases")
            isAllowInsecureProtocol = true
            credentials {
                username = project.findProperty("repoUser")?.toString() ?: ""
                password = project.findProperty("repoPass")?.toString() ?: ""
            }
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "club.revived.libs"
    version = rootProject.version.toString()

    java {
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = project.name
            }
        }

        repositories {
            maven {
                name = "revived"
                url = uri("https://mvn.int.revived.club/releases")
                isAllowInsecureProtocol = true
                credentials {
                    username = rootProject.findProperty("repoUser")?.toString() ?: ""
                    password = rootProject.findProperty("repoPass")?.toString() ?: ""
                }
            }
        }
    }
}
