group = "no.nav.syfo"
version = "1.0.0"

val coroutinesVersion = "1.10.2"
val jacksonVersion = "2.20.0"
val ktorVersion = "3.3.1"
val logbackVersion = "1.5.20"
val logstashEncoderVersion = "8.1"
val prometheusVersion = "0.16.0"
val kotlinVersion = "2.2.21"
val javaxAnnotationApiVersion = "1.3.2"
val jaxwsToolsVersion = "2.3.2"
val jaxwsApiVersion = "2.3.1"
val jaxbRuntimeVersion = "2.4.0-b180830.0438"
val jaxbApiVersion = "2.4.0-b180830.0359"
val javaxActivationVersion = "1.1.1"
val commonsTextVersion = "1.14.0"
val cxfVersion = "3.5.8"
val ktfmtVersion = "0.44"
val junitJupiterVersion = "6.0.0"


plugins {
    id("application")
    id("io.mateo.cxf-codegen") version "1.0.2"
    kotlin("jvm") version "2.2.21"
    id("com.diffplug.spotless") version "8.0.0"
}

application {
    mainClass.set("no.nav.syfo.BootstrapKt")

}

buildscript {
    dependencies {
        classpath("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
        classpath("org.glassfish.jaxb:jaxb-runtime:2.4.0-b180830.0438")
        classpath("com.sun.activation:javax.activation:1.2.0")
        classpath("com.sun.xml.ws:jaxws-tools:2.3.1") {
            exclude(group = "com.sun.xml.ws", module = "policy")
            exclude(group = "org.apache.commons", module = "commons-text")
        }
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}


dependencies {
    cxfCodegen("javax.annotation:javax.annotation-api:$javaxAnnotationApiVersion")
    cxfCodegen("javax.activation:activation:$javaxActivationVersion")
    cxfCodegen("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    cxfCodegen("javax.xml.bind:jaxb-api:$jaxbApiVersion")
    cxfCodegen ("javax.xml.ws:jaxws-api:$jaxwsApiVersion")
    cxfCodegen ("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
        exclude(group = "org.apache.commons", module = "commons-text")
    }
    cxfCodegen("com.sun.xml.bind:jaxb-impl:2.3.3")
    cxfCodegen("jakarta.xml.ws:jakarta.xml.ws-api:2.3.3")
    cxfCodegen("jakarta.annotation:jakarta.annotation-api:1.3.5")
    cxfCodegen("org.apache.commons:commons-text:$commonsTextVersion")
    cxfCodegen("org.apache.cxf:cxf-core:$cxfVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion") {
        exclude(group = "org.apache.velocity", module = "velocity")
    }

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")


    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("javax.xml.ws:jaxws-api:$jaxwsApiVersion")
    implementation("javax.annotation:javax.annotation-api:$javaxAnnotationApiVersion")
    implementation("javax.xml.bind:jaxb-api:$jaxbApiVersion")
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbRuntimeVersion")
    implementation("javax.activation:activation:$javaxActivationVersion")
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
        exclude(group = "org.apache.commons", module = "commons-text")
    }
    implementation("org.apache.commons:commons-text:$commonsTextVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


tasks {

    cxfCodegen {
        wsdl2java {
            register("subscription") {
                wsdl.set(file("$projectDir/src/main/resources/wsdl/subscription.wsdl"))
                bindingFiles.add("$projectDir/src/main/resources/xjb/binding.xml")
            }
        }
    }

    compileKotlin {
        dependsOn("wsdl2javaSubscription")
    }

    test {
        useJUnitPlatform {}
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }
}
