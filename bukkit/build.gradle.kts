repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://papermc.io/repo/repository/maven-public")
}

dependencies {
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.18.1-R0.1-SNAPSHOT")
    implementation("org.spongepowered", name = "configurate-yaml", version = "4.1.2")
    implementation(group = "net.kyori", name = "adventure-api", version = "4.9.3")
    implementation(group = "net.kyori", name = "adventure-text-minimessage", version = "4.1.0-SNAPSHOT")
    implementation(group = "net.kyori", name = "adventure-platform-bukkit", version = "4.0.1")
    implementation(group = "org.bstats", name = "bstats-bukkit", version = "2.2.1")
    implementation(project(":bolt-common"))
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
        relocate("net.kyori", "${project.group}.${rootProject.name}.lib.kyori")
        relocate("org.bstats", "${project.group}.${rootProject.name}.lib.bstats")
    }
}
