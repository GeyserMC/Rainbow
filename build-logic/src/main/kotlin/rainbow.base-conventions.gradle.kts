plugins {
    id("fabric-loom")
}

version = properties["mod_version"]!! as String
group = properties["maven_group"]!! as String

val archivesBaseName = properties["archives_base_name"]!! as String
val targetJavaVersion = 21

val buildNumber = System.getenv()["BUILD_NUMBER"]?: "DEV"
val fmjVersion = "$version-$buildNumber"

base {
    archivesName = archivesBaseName
}

repositories {
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }

    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
    }

    maven {
        name = "Open Collaboration"
        url = uri("https://repo.opencollab.dev/main")
    }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.layered {
        officialMojangMappings()
        parchment(libs.parchment)
    })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
}

tasks {
    processResources {
        inputs.property("version", fmjVersion)
        inputs.property("supported_versions", libs.versions.minecraft.supported.get())
        inputs.property("loader_version", libs.versions.fabric.loader.get())
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to fmjVersion,
                    "supported_versions" to libs.versions.minecraft.supported.get(),
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
