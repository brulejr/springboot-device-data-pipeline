plugins {
    kotlin("jvm") version "2.2.10" apply false
    kotlin("plugin.allopen") version "2.2.10" apply false
    kotlin("plugin.spring") version "2.2.10" apply false
    id("com.google.cloud.tools.jib") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.springframework.boot") version "3.5.5" apply false
}

allprojects {
    group = "io.jrb.labs"
    version = "0.1.0"
    repositories {
        mavenCentral()
    }
}
