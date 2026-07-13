plugins {
    id("rainbow.base-conventions")
    id("rainbow.publish-conventions")
}

loom {
    runs.clear()
}

tasks.withType<Jar> {
    archiveAppendix = "core"
}
