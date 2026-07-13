import org.gradle.api.publish.maven.internal.publication.MavenPomInternal

plugins {
    java
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "geysermc"
            url = uri(
                when {
                    version.toString().endsWith("-SNAPSHOT") -> "https://repo.opencollab.dev/maven-snapshots"
                    else -> "https://repo.opencollab.dev/maven-releases"
                }
            )
            credentials(PasswordCredentials::class)
        }
    }

    publications {
        register("publish", MavenPublication::class) {
            val jarTask = tasks.getByName<Jar>("jar")

            // This is the only way to use artifactId as a provider for now...
            (pom as MavenPomInternal).coordinates.artifactId = provider {
                "${jarTask.archiveBaseName.get()}-${jarTask.archiveAppendix.get()}"
            }

            from(project.components["java"])
        }
    }
}
