plugins {
    id("java")
    id("application")
}

group = "org.komamitsu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.scalar-labs:scalardl-java-client-sdk:3.6.0")
    implementation("info.picocli:picocli:4.6.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

application {
    mainClass.set("org.komamitsu.bank.Main")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

sourceSets {
    create("intTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val intTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

val intTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.runtimeOnly.get())
}

dependencies {
    intTestImplementation("com.scalar-labs:scalardb-schema-loader:3.7.0")

    intTestImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    intTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

val integrationTest = task<Test>("integrationTest") {
    useJUnitPlatform()

    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath
}

