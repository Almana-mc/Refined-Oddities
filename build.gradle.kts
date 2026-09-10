plugins {
    `java-library`
    `maven-publish`
    idea
    id("net.neoforged.moddev") version "2.0.146"
}

val minecraftVersion = property("minecraft_version") as String
val minecraftVersionRange = property("minecraft_version_range") as String
val neoVersion = property("neo_version") as String
val neoVersionRange = property("neo_version_range") as String
val loaderVersionRange = property("loader_version_range") as String
val modId = property("mod_id") as String
val modName = property("mod_name") as String
val modLicense = property("mod_license") as String
val modVersion = property("mod_version") as String
val modGroupId = property("mod_group_id") as String
val modAuthors = property("mod_authors") as String
val modDescription = property("mod_description") as String

version = modVersion
group = modGroupId

repositories {
    mavenLocal()
}

base {
    archivesName.set(modId)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

neoForge {
    version = neoVersion

    parchment {
        mappingsVersion = project.property("parchment_mappings_version") as String
        minecraftVersion = project.property("parchment_minecraft_version") as String
    }

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }

        create("data") {
            data()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.main.get().resources.srcDir("src/generated/resources")

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "neo_version_range" to neoVersionRange,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = project.projectDir.resolve("repo").toURI()
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
