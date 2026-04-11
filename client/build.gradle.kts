plugins {
    id("rainbow.base-conventions")
    id("rainbow.publish-conventions")
    id("rainbow.modrinth-publish-conventions")
}

val rainbowJarName = "Rainbow.jar"

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
        rename {rainbowJarName}
        into(project.layout.buildDirectory.file("libs"))
    }

    named("build") {
        dependsOn(copyJarTask)
    }
}

modrinth {
    uploadFile.set(project.layout.buildDirectory.file("libs/$rainbowJarName"))
}
