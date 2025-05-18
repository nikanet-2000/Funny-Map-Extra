import dev.architectury.pack200.java.Pack200Adapter

plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("gg.essential.loom") version "1.3.12"
    idea
    java
}

val modName: String by project
val modID: String by project
val modVersion: String by project

version = modVersion
group = modID

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.sk1er.club/repository/maven-public")
}

val packageLib: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly("org.spongepowered:mixin:0.8.5")

    packageLib("gg.essential:loader-launchwrapper:1.1.3")
    implementation("gg.essential:essential-1.8.9-forge:12132+g6e2bf4dc5")
}

sourceSets.main {
    output.setResourcesDir(file("${layout.buildDirectory.asFile.get()}/classes/kotlin/main"))
}

loom {
    silentMojangMappingsLicense()
    runConfigs {
        getByName("client") {
            property("mixin.debug.verbose", "true")
            property("asmhelper.verbose", "true")
            programArgs("--tweakClass", "gg.essential.loader.stage0.EssentialSetupTweaker")
            programArgs("--mixin", "mixins.${modID}.json")
            isIdeConfigGenerated = true
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(Pack200Adapter())
        mixinConfig("mixins.${modID}.json")
    }
    mixin.defaultRefmapName.set("mixins.${modID}.refmap.json")
}

tasks {
    processResources {
        inputs.property("modname", modName)
        inputs.property("modid", modID)
        inputs.property("version", project.version)
        inputs.property("mcversion", "1.8.9")

        filesMatching(listOf("mcmod.info", "mixins.${modID}.json")) {
            expand(
                mapOf(
                    "modname" to modName,
                    "modid" to modID,
                    "version" to project.version,
                    "mcversion" to "1.8.9"
                )
            )
        }
        dependsOn(compileJava)
    }
    jar {
        manifest.attributes(
            "MixinConfigs" to "mixins.${modID}.json",
            "ModSide" to "CLIENT",
            "TweakClass" to "gg.essential.loader.stage0.EssentialSetupTweaker",
            "TweakOrder" to "0"
        )
        dependsOn(shadowJar)
        enabled = false
    }
    remapJar {
        archiveBaseName.set(modName)
        inputFile.set(shadowJar.get().archiveFile)
    }
    shadowJar {
        archiveBaseName.set(modName)
        archiveClassifier.set("dev")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        configurations = listOf(packageLib)
        mergeServiceFiles()
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))
kotlin.jvmToolchain(8)
