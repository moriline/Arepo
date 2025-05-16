plugins {
    id("java")
}

group = "org.arepo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation ("info.picocli:picocli:4.7.4")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation("org.springframework:spring-jdbc:6.0.9")
    implementation("io.github.java-diff-utils:java-diff-utils:4.15")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
// run Build->jar task
/*
tasks.withType<Jar>() {
    manifest {
        //attributes["Main-Class"] = "org.atask.Main"
        attributes["Main-Class"] = "org.arepo.Main"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}
 */

tasks.jar{
    manifest.attributes["Main-Class"] = "org.arepo.Main"
    val dependencies = "deps"
    manifest.attributes["Class-Path"] = configurations
        .runtimeClasspath
        .get()
        .joinToString(separator = " ") { file ->
            println(file)

            file.copyTo(File(layout.buildDirectory.dir("libs").get().toString()+"/"+dependencies+"/"+file.name), true, 500)
            dependencies+"/${file.name}"
        }
}