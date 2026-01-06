plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.spring") version "1.9.0"
    kotlin("plugin.jpa") version "1.9.0"
    `maven-publish`
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
    id("org.owasp.dependencycheck") version "8.4.3"
    id("com.github.ben-manes.versions") version "0.49.0"
    id("org.sonarqube") version "4.4.1.3373"
    id("org.jetbrains.dokka") version "1.9.10"
    jacoco
}

group = "io.cacheflow"

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

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/config/detekt.yml")
    parallel = true
    autoCorrect = false
    ignoreFailures = false
    baseline = file("$projectDir/config/detekt-baseline.xml")
}

tasks.detekt {
    jvmTarget = "17"
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "17"
}

// Dokka configuration
tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    dokkaSourceSets {
        configureEach {
            includeNonPublic.set(false)
            reportUndocumented.set(true)
            skipEmptyPackages.set(true)
            jdkVersion.set(17)
            suppressObviousFunctions.set(true)
            suppressInheritedMembers.set(true)
            skipDeprecated.set(false)
            perPackageOption {
                matchingRegex.set("io.cacheflow.spring.*")
                reportUndocumented.set(true)
                skipEmptyPackages.set(true)
            }
        }
    }
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.25".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            excludes = listOf(
                "*.dto.*",
                "*.config.*",
                "*.exception.*",
                "*.example.*",
                "*.management.*",
                "*.aspect.*",
                "*.autoconfigure.*",
                "*DefaultImpls*"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.30".toBigDecimal()
            }
        }
    }
}

// SonarQube configuration
sonar {
    properties {
        property("sonar.projectKey", "mmorrison_cacheflow")
        property("sonar.organization", "mmorrison")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "src/main/kotlin")
        property("sonar.tests", "src/test/kotlin")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.kotlin.detekt.reportPaths", "build/reports/detekt/detekt.xml")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.exclusions", "**/dto/**,**/config/**,**/exception/**")
        property("sonar.cpd.exclusions", "**/dto/**,**/config/**")
        property("sonar.duplicateCodeMinTokens", "50")
        property("sonar.issue.ignore.multicriteria", "e1")
        property("sonar.issue.ignore.multicriteria.e1.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.e1.resourceKey", "**/*Test.kt")
        property("sonar.gradle.skipCompile", "true")
    }
}

// OWASP Dependency Check configuration
dependencyCheck {
    format = "ALL"
    suppressionFile = "config/dependency-check-suppressions.xml"
    failBuildOnCVSS = 7.0f
    skip = false
    autoUpdate = false
    cveValidForHours = 24 * 7 // 7 days
    failOnError = if (project.hasProperty("owasp.failOnError")) {
        project.property("owasp.failOnError").toString().toBoolean()
    } else {
        false
    }
}

// Additional task configurations
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs all quality checks (excluding OWASP)"
    dependsOn("detekt", "test", "jacocoTestReport")
}

tasks.register("qualityCheckWithSecurity") {
    group = "verification"
    description = "Runs all quality checks including OWASP security scanning"
    dependsOn("detekt", "test", "jacocoTestReport", "dependencyCheckAnalyze")
}

tasks.register("buildAndTest") {
    group = "build"
    description = "Builds the project and runs all tests"
    dependsOn("build", "test", "jacocoTestReport")
}

tasks.register("fullCheck") {
    group = "verification"
    description = "Runs all checks including quality, security, and documentation"
    dependsOn("qualityCheck", "dokkaHtml")
}

tasks.register("fullCheckWithSecurity") {
    group = "verification"
    description = "Runs all checks including security scanning and documentation"
    dependsOn("qualityCheckWithSecurity", "dokkaHtml")
}

tasks.register("securityCheck") {
    group = "verification"
    description = "Runs only OWASP security vulnerability scanning"
    dependsOn("dependencyCheckAnalyze")
}

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
