import org.gradle.api.Project

// Nicely stolen from Geyser

fun buildNumber(): Int {
    return System.getenv()["BUILD_NUMBER"]?.let {Integer.parseInt(it)} ?: -1
}

fun projectVersion(project: Project): String {
    return project.version.toString().replace("SNAPSHOT", "b" + buildNumber())
}

fun versionName(project: Project): String {
    return "Rainbow-${projectVersion(project)}"
}
