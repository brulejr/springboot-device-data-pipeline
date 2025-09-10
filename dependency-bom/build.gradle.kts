plugins {
    `java-platform`
}

dependencies {
    constraints {
        // development dependencies
        api("com.fasterxml.jackson.core:jackson-databind:2.18.3")
        api("com.hivemq:hivemq-mqtt-client:1.3.0")
        api("io.projectreactor:reactor-core:3.7.9")
        api("org.apache.commons:commons-lang3:3.18.0")
        api("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
        api("org.jetbrains.kotlin:kotlin-reflect:2.2.10")
        api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
        api("org.slf4j:slf4j-api:2.0.17")
        api("org.springframework:spring-context:6.2.10")
        api("org.springframework:spring-web:6.2.10")
        api("org.springframework:spring-webflux:6.2.10")
        api("org.springframework.boot:spring-boot:2.5.4")
        api("org.springframework.boot:spring-boot-dependencies:2.5.4")
        api("org.springframework.data:spring-data-mongodb:4.5.3")

        // test dependencies
        api("io.mockk:mockk:1.13.12")
        api("org.assertj:assertj-core:3.26.0")
        api("org.jetbrains.kotlin:kotlin-test-junit5:2.2.10")
        api("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        api("org.junit.jupiter:junit-jupiter-params:5.10.1")

        api("org.springframework.boot:spring-boot-starter-test:3.5.5")
        api("io.projectreactor:reactor-test:3.7.9")
        api("org.jetbrains.kotlin:kotlin-test-junit5:2.2.10")
        api("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        api("org.springframework.cloud:spring-cloud-stream-test-binder:4.3.0")
        api("org.junit.platform:junit-platform-launcher:1.12.2")
    }
}

