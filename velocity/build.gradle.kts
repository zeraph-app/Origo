plugins {
    id("origo.kotlin")
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.velocity.api)
}
