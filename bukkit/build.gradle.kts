plugins {
    id("com.modrinth.minotaur") version "2.+"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly(group = "io.papermc.paper", name = "paper-api", version = "1.21.9-R0.1-SNAPSHOT")
    compileOnly(group = "net.kyori", name = "adventure-api", version = "4.24.0")
    compileOnly(group = "net.kyori", name = "adventure-text-minimessage", version = "4.24.0")
    implementation(group = "net.kyori", name = "event-api", version = "3.0.0") {
        exclude(module = "guava")
        exclude(module = "checker-qual")
    }
    implementation(group = "org.bstats", name = "bstats-bukkit", version = "3.0.2")
    implementation(group = "org.popcraft", name = "chunky-nbt", version = "1.3.127")
    api(project(":bolt-common"))
    implementation(project(":bolt-folia"))
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("plugin.yml") {
            expand(
                "name" to project.property("artifactName"),
                "version" to project.version,
                "group" to project.group,
                "description" to project.property("description"),
            )
        }
    }
    shadowJar {
        minimize {
            exclude(project(":bolt-common"))
            exclude(project(":bolt-folia"))
        }
        relocate("net.kyori.event", "${project.group}.${rootProject.name}.lib.net.kyori.event")
        relocate("org.bstats", "${project.group}.${rootProject.name}.lib.org.bstats")
        relocate("org.popcraft.chunky.nbt", "${project.group}.${rootProject.name}.lib.org.popcraft.chunky.nbt")
        manifest {
            attributes("paperweight-mappings-namespace" to "mojang")
        }
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("bolt")
    versionName.set("${project.property("artifactName")} ${project.version}")
    versionNumber.set("${project.version}")
    versionType.set("release")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(
        "1.18.2",
        "1.19",
        "1.19.1",
        "1.19.2",
        "1.19.3",
        "1.19.4",
        "1.20",
        "1.20.1",
        "1.20.2",
        "1.20.3",
        "1.20.4",
        "1.20.5",
        "1.20.6",
        "1.21",
        "1.21.1",
        "1.21.2",
        "1.21.3",
        "1.21.4"
    )
    loaders.addAll("bukkit", "spigot", "paper", "folia")
}
