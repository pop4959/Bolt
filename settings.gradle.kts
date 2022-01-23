rootProject.name = "bolt"

sequenceOf(
    "common",
    "bukkit",
    "paper"
).forEach {
    include("${rootProject.name}-$it")
    project(":${rootProject.name}-$it").projectDir = file(it)
}
