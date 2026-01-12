plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
    `maven-publish`
    id("org.jetbrains.kotlin.plugin.allopen") version "2.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    // Detekt temporarily disabled - waiting for Gradle 9.1 + detekt 2.0.0-alpha.1
    // According to https://detekt.dev/docs/introduction/compatibility/,
    // detekt 2.0.0-alpha.1 supports Gradle 9.1.0 and JDK 25
    // id("io.gitlab.arturbosch.detekt") version "2.0.0-alpha.1"
    id("org.owasp.dependencycheck") version "8.4.3"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.sonarqube") version "4.4.1.3373"
    id("org.jetbrains.dokka") version "1.9.10"
    // JaCoCo temporarily disabled due to Java 25 compatibility issues
    // jacoco
}

group = "io.cacheflow"

version = "0.1.0-alpha"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    // Targeting Java 21 for compilation
    // Note: Java 24 not yet supported by Kotlin 2.1.0
}

repositories {
    mavenCentral()
    // For Detekt 2.0.0-alpha.1 (if available)
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

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
    // mockito-inline is deprecated - inline mocking enabled via mockito-extensions/org.mockito.plugins.MockMaker
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0") // Kotlin-specific mocking support
    testImplementation("net.bytebuddy:byte-buddy:1.15.11") // Latest ByteBuddy for Java 21+ support
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // JaCoCo temporarily disabled due to Java 25 compatibility issues
    // finalizedBy(tasks.jacocoTestReport)
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // JVM args for Mockito/ByteBuddy to work with Java 21+
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.text=ALL-UNNAMED",
        "--add-opens", "java.base/java.time=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens", "java.base/sun.util.resources=ALL-UNNAMED",
        "--add-opens", "java.base/sun.util.locale.provider=ALL-UNNAMED",
    )
}

// Detekt configuration - temporarily disabled
// According to https://detekt.dev/docs/introduction/compatibility/,
// detekt 2.0.0-alpha.1 supports Gradle 9.1.0 and JDK 25
// Once Gradle 9.1 is released, enable with: id("io.gitlab.arturbosch.detekt") version "2.0.0-alpha.1"
// detekt {
//     buildUponDefaultConfig = true
//     config.setFrom("$projectDir/config/detekt.yml")
//     parallel = true
//     autoCorrect = false
//     ignoreFailures = false
// }
//
// tasks.detekt {
//     jvmTarget = "21"
// }

// KtLint configuration
ktlint {
    version.set("1.5.0") // Use ktlint version compatible with Kotlin 2.2.0
    android.set(false)
    ignoreFailures.set(true) // Don't fail build on style violations - report only
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

// Dokka configuration
tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
    dokkaSourceSets {
        configureEach {
            includeNonPublic.set(false)
            reportUndocumented.set(true)
            skipEmptyPackages.set(true)
            jdkVersion.set(21)
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

// JaCoCo configuration - temporarily disabled due to Java 25 compatibility issues
// jacoco {
//     toolVersion = "0.8.12" // Updated for Java 21+ support
// }
//
// tasks.jacocoTestReport {
//     dependsOn(tasks.test)
//     reports {
//         xml.required.set(true)
//         html.required.set(true)
//         csv.required.set(false)
//     }
//     finalizedBy(tasks.jacocoTestCoverageVerification)
// }
//
// tasks.jacocoTestCoverageVerification {
//     dependsOn(tasks.jacocoTestReport)
//     violationRules {
//         rule {
//             limit {
//                 minimum = "0.25".toBigDecimal()
//             }
//         }
//         rule {
//             element = "CLASS"
//             excludes =
//                 listOf(
//                     "*.dto.*",
//                     "*.config.*",
//                     "*.exception.*",
//                     "*.example.*",
//                     "*.management.*",
//                     "*.aspect.*",
//                     "*.autoconfigure.*",
//                     "*DefaultImpls*",
//                 )
//             limit {
//                 counter = "LINE"
//                 value = "COVEREDRATIO"
//                 minimum = "0.30".toBigDecimal()
//             }
//         }
//     }
// }

// SonarQube configuration
sonar {
    properties {
        property("sonar.projectKey", "mmorrison_cacheflow")
        property("sonar.organization", "mmorrison")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", listOf("src/main/kotlin"))
        property("sonar.tests", listOf("src/test/kotlin"))
        property("sonar.coverage.jacoco.xmlReportPaths", listOf("build/reports/jacoco/test/jacocoTestReport.xml"))
        property("sonar.kotlin.detekt.reportPaths", listOf("build/reports/detekt/detekt.xml"))
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.exclusions", listOf("**/dto/**", "**/config/**", "**/exception/**"))
        property("sonar.cpd.exclusions", listOf("**/dto/**", "**/config/**"))
        property("sonar.duplicateCodeMinTokens", "50")
        property("sonar.issue.ignore.multicriteria", "e1")
        property("sonar.issue.ignore.multicriteria.e1.ruleKey", "kotlin:S107")
        property("sonar.issue.ignore.multicriteria.e1.resourceKey", "**/*Test.kt")
        property("sonar.gradle.skipCompile", "true")
    }
}

// OWASP Dependency Check configuration
// Note: NVD requires an API key since 2023. Set nvdApiKey property or NVD_API_KEY environment variable
// to enable CVE database updates. Without it, security scanning will be skipped.
// Get API key from: https://nvd.nist.gov/developers/request-an-api-key
dependencyCheck {
    format = "ALL"
    suppressionFile = "config/dependency-check-suppressions.xml"
    failBuildOnCVSS = 7.0f

    // Skip dependency check if no API key is available (NVD requires API key since 2023)
    skip = !(project.hasProperty("nvdApiKey") || System.getenv("NVD_API_KEY") != null)

    cveValidForHours = 24 * 7 // 7 days
    failOnError = false // Don't fail build on errors (e.g., network issues)
}

// Additional task configurations
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs all quality checks (excluding OWASP and JaCoCo)"
    // Note: detekt temporarily excluded due to Gradle 9.0 compatibility
    // Note: jacoco temporarily excluded due to Java 25 compatibility
    dependsOn("test")
}

tasks.register("qualityCheckWithSecurity") {
    group = "verification"
    description = "Runs all quality checks including OWASP security scanning"
    // Note: detekt temporarily excluded due to Gradle 9.0 compatibility
    // Note: jacoco temporarily excluded due to Java 25 compatibility
    dependsOn("test", "dependencyCheckAnalyze")
}

tasks.register("buildAndTest") {
    group = "build"
    description = "Builds the project and runs all tests"
    // Note: jacoco temporarily excluded due to Java 25 compatibility
    dependsOn("build", "test")
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
