plugins {
    id("net.fabricmc.fabric-loom")
}

version = properties["mod_version"]!! as String
group = properties["maven_group"]!! as String

val archivesBaseName = properties["archives_base_name"]!! as String
val targetJavaVersion = 25

val fmjVersion = projectVersion(project)

base {
    archivesName = archivesBaseName
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
        inputs.property("supported_versions", libs.versions.minecraft.release.get())
        inputs.property("loader_version", libs.versions.fabric.loader.get())
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to fmjVersion,
                    "minecraft_version" to libs.versions.minecraft.release.get(),
                    "loader_version" to libs.versions.fabric.loader.get()
                )
            )
        }
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${archivesBaseName}" }
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
            runDir = "run-server"
        }
    }
}
