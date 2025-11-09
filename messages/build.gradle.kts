plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    implementation(platform(project(":dependency-bom")))

    implementation(project(":commons-ms-client"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
}
