plugins {
    id("origo.kotlin")
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.fabric.loader)
    compileOnly(libs.mixin)
    compileOnly(libs.slf4j.api)
}
