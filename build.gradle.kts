import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.10"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.4.10"
}

group = "com.naskar"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.hsqldb:hsqldb:2.5.1")
    implementation("com.github.naskarlab:fluent-query-jpa-metamodel:master-SNAPSHOT")
    implementation("com.github.naskarlab:fluent-query-jpa:master-SNAPSHOT")

    testImplementation("junit:junit:4.12")
    testImplementation("javax.persistence:javax.persistence-api:2.2")
    testImplementation("org.hibernate:hibernate-entitymanager:5.4.21.Final")
    testImplementation("org.apache.openjpa:openjpa:3.1.2")
    testImplementation("org.eclipse.persistence:eclipselink:2.7.7")
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
        kotlinOptions.languageVersion = "1.4"
    }

    withType<Test> {
        maxParallelForks = (Runtime.getRuntime().availableProcessors()).takeIf { it > 0 } ?: 1
        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

}

allOpen {
    annotation("javax.persistence.Entity")
}