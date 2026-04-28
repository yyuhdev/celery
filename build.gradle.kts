plugins {
    `java-library`
    `maven-publish`
    id("eclipse")
}

group = "de.yyuh"
version = "1.0.0-SNAPSHOT"

subprojects {
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "eclipse")

    group = buildString {
        append("de.yyuh.libs")

        var p = project.parent
        while (p != null && p != rootProject) {
            append(".${p.name}")
            p = p.parent
        }
    }

    java {
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])

                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
            }
        }

        repositories {
            maven {
                name = "revived"
                url = uri("http://mvn.int.revived.club/releases")
                isAllowInsecureProtocol = true
                credentials {
                    username = rootProject.findProperty("repoUser")?.toString() ?: ""
                    password = rootProject.findProperty("repoPass")?.toString() ?: ""
                }
            }
        }
    }
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
                username = findProperty("repoUser")?.toString() ?: ""
                password = findProperty("repoPass")?.toString() ?: ""
            }
        }
    }
}
