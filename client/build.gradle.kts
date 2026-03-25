import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("rainbow.base-conventions")
    id("rainbow.publish-conventions")
}

dependencies {
    implementation(project(":rainbow"))
    include(project(":rainbow"))
}

tasks {
    val copyJarTask = register<Copy>("copyRainbowClientJar") {
        group = "build"

        val remapJarTask = getByName<RemapJarTask>("remapJar")
        dependsOn(remapJarTask)

        from(remapJarTask.archiveFile)
        rename {
            "Rainbow.jar"
        }
        into(project.layout.buildDirectory.file("libs"))
    }

    named("build") {
        dependsOn(copyJarTask)
    }
}
