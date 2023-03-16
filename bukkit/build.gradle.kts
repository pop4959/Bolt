repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.19.4-R0.1-SNAPSHOT")
    implementation(group = "net.kyori", name = "adventure-api", version = "4.13.0")
    implementation(group = "net.kyori", name = "adventure-text-minimessage", version = "4.13.0")
    implementation(group = "net.kyori", name = "adventure-platform-bukkit", version = "4.3.0")
    implementation(group = "org.bstats", name = "bstats-bukkit", version = "3.0.1")
    implementation(project(":bolt-common"))
    implementation(project(":bolt-paper"))
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "name" to rootProject.name.capitalize(),
                "version" to project.version,
                "group" to project.group,
                "author" to project.property("author"),
                "description" to project.property("description"),
            )
        }
    }
    shadowJar {
        minimize {
            exclude(project(":bolt-common"))
        }
        relocate("org.yaml", "${project.group}.${rootProject.name}.lib.org.yaml")
        relocate("io.leangen", "${project.group}.${rootProject.name}.lib.io.leangen")
        relocate("net.kyori", "${project.group}.${rootProject.name}.lib.net.kyori")
        relocate("org.bstats", "${project.group}.${rootProject.name}.lib.org.bstats")
    }
}
