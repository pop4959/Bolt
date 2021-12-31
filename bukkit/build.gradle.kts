repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://papermc.io/repo/repository/maven-public")
}

dependencies {
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.18.1-R0.1-SNAPSHOT")
    compileOnly(group = "org.xerial", name = "sqlite-jdbc", version = "3.36.0.3")
    compileOnly(group = "mysql", name = "mysql-connector-java", version = "8.0.27")
    implementation(group = "net.kyori", name = "adventure-api", version = "4.9.3")
    implementation(group = "net.kyori", name = "adventure-text-minimessage", version = "4.1.0-SNAPSHOT")
    implementation(group = "net.kyori", name = "adventure-platform-bukkit", version = "4.0.0")
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
