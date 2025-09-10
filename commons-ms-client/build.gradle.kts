plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    implementation(platform(project(":dependency-bom")))

    implementation(project(":commons-core"))

    api("org.springframework:spring-web")
}
