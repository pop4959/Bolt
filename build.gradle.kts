import java.io.ByteArrayOutputStream

plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

subprojects {
    plugins.apply("java-library")
    plugins.apply("maven-publish")
    plugins.apply("com.github.johnrengelman.shadow")

    group = "${project.property("group")}"
    version = "${project.property("version")}.${commitsSinceLastTag()}"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
        withSourcesJar()
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }
        jar {
            archiveClassifier.set("noshade")
        }
        shadowJar {
            archiveClassifier.set("")
            archiveFileName.set("${project.property("artifactName")}-${project.version}.jar")
        }
        build {
            dependsOn(shadowJar)
        }
    }

    publishing {
        repositories {
            if (project.hasProperty("mavenUsername") && project.hasProperty("mavenPassword")) {
                maven {
                    credentials {
                        username = "${project.property("mavenUsername")}"
                        password = "${project.property("mavenPassword")}"
                    }
                    url = uri("https://repo.codemc.io/repository/maven-releases/")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "${project.group}"
                artifactId = project.name
                version = "${project.version}"
                from(components["java"])
            }
        }
    }
}

fun commitsSinceLastTag(): String {
    val tagDescription = ByteArrayOutputStream()
    exec {
        commandLine("git", "describe", "--tags", "--always")
        standardOutput = tagDescription
    }
    if (tagDescription.toString().indexOf('-') < 0) {
        return "0"
    }
    return tagDescription.toString().split('-')[1]
}
