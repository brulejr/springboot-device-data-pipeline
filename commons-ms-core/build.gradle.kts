plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
}

dependencies {
    implementation(platform(project(":dependency-bom")))

    api(project(":commons-core"))
    api("io.projectreactor:reactor-core")
    api("org.springframework:spring-context")
    api("org.springframework:spring-web")
    api("org.springframework.data:spring-data-mongodb")

    testImplementation(project(":commons-core"))
    testImplementation(project(":commons-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}