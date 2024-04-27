pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        jcenter()  // Keep this if you are using libraries that are only available on jcenter
    }
}


rootProject.name = "woobipass_test"
include(":app")
 