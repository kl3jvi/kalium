import org.codehaus.groovy.runtime.ProcessGroovyMethods
import org.gradle.api.Project
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Convenience method to obtain a property from `$projectRoot/local.properties` file
 * without passing the project param
 */
fun <T> Project.getLocalProperty(propertyName: String, defaultValue: T): T {
    return getLocalProperty(propertyName, defaultValue, this)
}

/**
 * Util to obtain property declared on `$projectRoot/local.properties` file or default
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> getLocalProperty(propertyName: String, defaultValue: T, project: Project): T {
    val localProperties = Properties().apply {
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }

    val localValue = localProperties.getOrDefault(propertyName, defaultValue) as? T ?: defaultValue
    if (localValue != null) {
        println("> Reading local prop '$propertyName'")
    }
    return localValue
}

/**
 * Run command and return the [Process]
 */
fun String.execute(): Process = ProcessGroovyMethods.execute(this).also {
    it.waitFor(30, TimeUnit.SECONDS)
}

/**
 * Run command and return the output as text
 */
fun Process.text(): String = ProcessGroovyMethods.getText(this)