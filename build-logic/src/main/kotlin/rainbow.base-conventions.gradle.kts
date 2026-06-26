plugins {
    id("net.fabricmc.fabric-loom")
}

// Needed because these properties STILL aren't lazy
version = StringProvider(providers.gradleProperty("mod_version"))
group = StringProvider(providers.gradleProperty("maven_group"))

val targetJavaVersion = 25

val fmjVersion = projectVersion(project)

base {
    archivesName = "rainbow"
}

repositories {}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
}

tasks {
    processResources {
        inputs.property("version", fmjVersion)
        inputs.property("minecraft_version", libs.versions.minecraft.supported.base.get())
        inputs.property("loader_version", libs.versions.fabric.loader.get())
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to fmjVersion.get(),
                    "minecraft_version" to libs.versions.minecraft.supported.base.get(),
                    "loader_version" to libs.versions.fabric.loader.get()
                )
            )
        }
    }

    jar {
        from(rootDir.resolve("LICENSE")) {
            rename { "${it}_${base.archivesName}" }
        }
        from(rootDir.resolve("LICENSE.LESSER")) {
            rename { "${it}_${base.archivesName}" }
        }
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = targetJavaVersion
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
    withSourcesJar()
}

loom {
    runs {
        named("server") {
            runDirectory = project.file("run-server")
        }
    }
}
