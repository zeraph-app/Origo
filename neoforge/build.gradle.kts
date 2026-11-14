plugins {
    id("origo.kotlin")
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.fancymodloader) { isTransitive = false }
    compileOnly(libs.mergetool) { isTransitive = false }
    compileOnly(libs.mixin)
    compileOnly(libs.slf4j.api)
}
