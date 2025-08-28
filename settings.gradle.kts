pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()   // <- imprescindible para resolver KSP y otros plugins
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GuiasClinicas"
include(":app")
