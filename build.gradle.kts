plugins {
    id("origo.kotlin")
    alias(libs.plugins.shadow)
}

val platforms = listOf(":common", ":spigot", ":paper", ":bungeecord", ":velocity", ":fabric", ":neoforge")

dependencies {
    platforms.forEach { implementation(project(it)) }
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveBaseName = "Origo"
        archiveClassifier = ""
        from("LICENSE")
        relocate("kotlin", "app.zeraph.origo.libs.kotlin")
        exclude("META-INF/*.kotlin_module", "META-INF/versions/**", "META-INF/maven/**", "org/intellij/**", "org/jetbrains/**")
        minimize {
            platforms.forEach { exclude(project(it)) }
        }
    }

    assemble {
        dependsOn(shadowJar)
    }
}
