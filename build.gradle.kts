plugins {
    `java-library`
    checkstyle
    id("com.github.spotbugs") version "6.5.13"
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("signing")
}

group = "ai.webscraping"
version = "4.3.1"
description = "Official Java client for the WebScraping.AI API"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    // sources + javadoc JARs are added by the vanniktech-maven-publish plugin
    // automatically; declaring them here too would publish them twice.
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 11
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addBooleanOption("Xdoclint:none", true)
        addStringOption("Xmaxwarns", "1")
    }
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.18.12")

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.wiremock:wiremock:3.13.2")
    testImplementation("org.assertj:assertj-core:3.27.7")
    // WireMock drags in a newer jackson and older httpcomponents; pin tests to the
    // jackson we ship and to patched httpcomponents.
    testImplementation(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.18.12"))
    constraints {
        testImplementation("org.apache.httpcomponents.client5:httpclient5:5.6.3")
        testImplementation("org.apache.httpcomponents.core5:httpcore5:5.4.3")
        testImplementation("org.apache.httpcomponents.core5:httpcore5-h2:5.4.3")
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// CI matrix entries pass -Ptest.java=<11|17|21> to run the test JVM on a
// specific JDK toolchain while the Gradle daemon stays on JDK 21.
val testJavaProperty: String? = project.findProperty("test.java") as String?
if (testJavaProperty != null) {
    tasks.named<Test>("test").configure {
        javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(testJavaProperty.toInt())
        }
    }
}

// ---------- Smoke source set ----------
sourceSets {
    create("smoke") {
        java.srcDir("src/smoke/java")
        compileClasspath += sourceSets["main"].output + configurations.runtimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

tasks.register<JavaExec>("smoke") {
    description = "Hits the live WebScraping.AI API across all 10 endpoints. Costs ~76 credits."
    group = "verification"
    classpath = sourceSets["smoke"].runtimeClasspath
    mainClass = "ai.webscraping.smoke.Smoke"
    standardInput = System.`in`
}

// ---------- Lint ----------
checkstyle {
    toolVersion = "14.3.0"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.named<Checkstyle>("checkstyleTest").configure {
    enabled = false
}
tasks.matching { it.name == "checkstyleSmoke" }.configureEach {
    enabled = false
}

spotbugs {
    toolVersion = "4.10.5"
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
    excludeFilter = rootProject.file("config/spotbugs/exclude.xml")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html") { required = true }
    reports.create("xml") { required = false }
}
tasks.named("spotbugsTest").configure { enabled = false }
tasks.matching { it.name == "spotbugsSmoke" }.configureEach { enabled = false }

// ---------- Publishing (Sonatype Central Portal) ----------
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates("ai.webscraping", "webscraping-ai", project.version.toString())

    pom {
        name = "webscraping-ai"
        description = "Official Java client for the WebScraping.AI API"
        url = "https://github.com/webscraping-ai/webscraping-ai-java"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "webscraping-ai"
                name = "WebScraping.AI"
                url = "https://webscraping.ai"
            }
        }
        scm {
            url = "https://github.com/webscraping-ai/webscraping-ai-java"
            connection = "scm:git:https://github.com/webscraping-ai/webscraping-ai-java.git"
            developerConnection = "scm:git:ssh://git@github.com/webscraping-ai/webscraping-ai-java.git"
        }
    }
}
