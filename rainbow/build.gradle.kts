plugins {
    id("rainbow.base-conventions")
    id("rainbow.publish-conventions")
}

loom {
    accessWidenerPath = file("src/main/resources/rainbow.accesswidener")

    runs.clear()
}

tasks.withType<Jar> {
    archiveAppendix = "core"
}
