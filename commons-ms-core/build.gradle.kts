plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    implementation(platform(project(":dependency-bom")))

    api(project(":commons-core"))
    api("org.springframework:spring-context")
}
