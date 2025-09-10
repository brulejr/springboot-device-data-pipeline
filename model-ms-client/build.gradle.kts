plugins {
    kotlin("jvm")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(platform(project(":dependency-bom")))

    implementation(project(":commons-ms-client"))
    implementation(project(":messages"))

    api("org.springframework.boot:spring-boot")
    api("org.springframework:spring-webflux")
}