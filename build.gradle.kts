plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
    `maven-publish`
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.20"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    // id("io.gitlab.arturbosch.detekt") version "1.23.0" // Temporarily disabled due to version conflicts
    id("org.owasp.dependencycheck") version "8.4.3"
    id("com.github.ben-manes.versions") version "0.49.0"
}

group = "com.yourcompany"

version = "0.1.0-alpha"

java { sourceCompatibility = JavaVersion.VERSION_17 }

repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> { useJUnitPlatform() }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("CacheFlow Spring Boot Starter")
                description.set("Multi-level caching solution for Spring Boot applications")
                url.set("https://github.com/mmorrison/cacheflow")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("mmorrison")
                        name.set("Marcus Morrison")
                        email.set("marcus@example.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/mmorrison/cacheflow.git")
                    developerConnection.set("scm:git:ssh://github.com:mmorrison/cacheflow.git")
                    url.set("https://github.com/mmorrison/cacheflow")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.findProperty("OSSRH_USERNAME")?.toString() ?: ""
                password = project.findProperty("OSSRH_PASSWORD")?.toString() ?: ""
            }
        }
    }
}
