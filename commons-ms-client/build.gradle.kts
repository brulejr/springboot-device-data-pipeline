plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    implementation(platform(project(":dependency-bom")))

    implementation(project(":commons-core"))

    api("com.fasterxml.jackson.core:jackson-databind")
    api("org.springframework:spring-web")
}
