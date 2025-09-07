plugins {
    kotlin("jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(platform(project(":dependency-bom")))
    implementation(project(":commons-core"))
    implementation(project(":messages"))
    implementation("org.springframework.boot:spring-boot-starter")
}