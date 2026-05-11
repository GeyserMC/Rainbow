plugins {
    id("com.modrinth.minotaur")
}

tasks.modrinth.get().dependsOn(tasks.modrinthSyncBody)

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN") ?: "")
    debugMode.set(System.getenv("MODRINTH_TOKEN") == null)
    projectId.set("QD7c9rxP")
    versionName.set(versionName(project))
    versionNumber.set(projectVersion(project))
    versionType.set("beta")
    changelog.set(System.getenv("CHANGELOG") ?: "")
    gameVersions.addAll(libs.versions.minecraft.supported.modrinth.get().split(","))
    loaders.add("fabric")

    dependencies {
        required.project("P7dR8mSH") // Fabric API
    }

    syncBodyFrom.set(rootProject.file("README.md").readText())
}
