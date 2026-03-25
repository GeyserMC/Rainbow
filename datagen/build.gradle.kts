plugins {
    id("rainbow.base-conventions")
    id("rainbow.publish-conventions")
}

dependencies {
    implementation(project(":rainbow"))
}

loom {
    accessWidenerPath = file("src/main/resources/rainbow-datagen.accesswidener")
}
