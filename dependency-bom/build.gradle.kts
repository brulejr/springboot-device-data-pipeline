plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("com.fasterxml.jackson.core:jackson-databind:2.18.3")
        api("com.hivemq:hivemq-mqtt-client:1.3.0")
        api("org.apache.commons:commons-lang3:3.18.0")
        api("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
        api("org.jetbrains.kotlin:kotlin-reflect:2.2.10")
        api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
        api("org.slf4j:slf4j-api:2.0.17")
        api("org.springframework:spring-context:5.3.9")
        api("org.springframework.boot:spring-boot-dependencies:2.5.4")
    }
}

