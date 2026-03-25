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

        val jarTask = getByName<Jar>("jar")
        dependsOn(jarTask)

        from(jarTask.archiveFile)
        rename {
            "Rainbow.jar"
        }
        into(project.layout.buildDirectory.file("libs"))
    }

    named("build") {
        dependsOn(copyJarTask)
    }
}
