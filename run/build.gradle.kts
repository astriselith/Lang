plugins {
    `java-library`
    application
}

application {
    mainClass.set("com.lang.Main")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

/*tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
    options.compilerArgs.addAll(listOf("-Xlint:-options", "-Xlint:-processing"))
}
*/
tasks.named<JavaExec>("run") {
    args("-w", "form", "-e", "main.lang")
}