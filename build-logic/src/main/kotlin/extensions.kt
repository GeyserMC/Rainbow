import org.gradle.api.Project
import org.gradle.api.provider.Provider

// Nicely stolen from Geyser

fun buildNumber(): Int {
    return System.getenv()["BUILD_NUMBER"]?.let {Integer.parseInt(it)} ?: -1
}

fun projectVersion(project: Project): Provider<String> {
    return project.provider {
        project.version.toString().replace("SNAPSHOT", "b" + buildNumber())
    }
}

fun versionName(project: Project): Provider<String> {
    return projectVersion(project).map { "Rainbow-$it" }
}
