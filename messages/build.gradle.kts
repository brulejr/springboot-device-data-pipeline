plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    implementation(platform(project(":dependency-bom")))

    implementation("com.fasterxml.jackson.core:jackson-databind")
}
