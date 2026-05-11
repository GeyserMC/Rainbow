plugins {
    `kotlin-dsl`
}

repositories {
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    gradlePluginPortal()
}

dependencies {
    // Very ugly... https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(libs.fabric.loom)
    implementation(libs.minotaur)
}
