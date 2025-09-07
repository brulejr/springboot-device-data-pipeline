plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    implementation(platform(project(":dependency-bom")))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.slf4j:slf4j-api")
}
