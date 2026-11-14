plugins {
    id("origo.kotlin")
}

dependencies {
    compileOnlyApi(libs.netty.codec)
    compileOnly(libs.snakeyaml)

    testImplementation(kotlin("test"))
    testImplementation(libs.netty.codec)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
