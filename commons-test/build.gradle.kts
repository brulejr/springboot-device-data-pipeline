plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    implementation(platform(project(":dependency-bom")))

    api("io.mockk:mockk")
    api("org.apache.commons:commons-lang3")
    api("org.assertj:assertj-core")
}
