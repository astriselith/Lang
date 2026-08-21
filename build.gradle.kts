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