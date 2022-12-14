import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val coroutinesVersion = "1.6.4"
val jacksonVersion = "2.14.1"
val kluentVersion = "1.72"
val ktorVersion = "2.2.1"
val logbackVersion = "1.4.5"
val logstashEncoderVersion = "7.2"
val prometheusVersion = "0.16.0"
val smCommonVersion = "1.1490275"
val mockkVersion = "1.13.2"
val testContainerKafkaVersion = "1.17.6"
val kotlinVersion = "1.7.22"
val kotestVersion = "5.5.4"
val javaxAnnotationApiVersion = "1.3.2"
val jaxwsToolsVersion = "2.3.2"
val jaxwsApiVersion = "2.3.1"
val jaxbRuntimeVersion = "2.4.0-b180830.0438"
val jaxbApiVersion = "2.4.0-b180830.0359"
val javaxActivationVersion = "1.1.1"
val commonsTextVersion = "1.10.0"
val nettyCodecVersion = "4.1.86.Final"
val cxfVersion = "3.5.5"

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "no.nav.syfo.BootstrapKt"
}

plugins {
    id("io.mateo.cxf-codegen") version "1.0.2"
    id("org.jmailen.kotlinter") version "3.12.0"
    kotlin("jvm") version "1.7.22"
    id("com.diffplug.spotless") version "6.5.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
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

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/syfosm-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
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

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    // This is to override version that is in io.ktor:ktor-server-netty
    // https://www.cve.org/CVERecord?id=CVE-2022-41915
    implementation("io.netty:netty-codec:$nettyCodecVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("no.nav.helse:syfosm-common-rest-sts:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-ws:$smCommonVersion")

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

    testImplementation("org.amshove.kluent:kluent:$kluentVersion") 
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.testcontainers:kafka:$testContainerKafkaVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty") 
    }
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}


tasks {

    create("printVersion") {
        println(project.version)
    }

    cxfCodegen {
        wsdl2java {
            register("subscription") {
                wsdl.set(file("$projectDir/src/main/resources/wsdl/subscription.wsdl"))
                bindingFiles.add("$projectDir/src/main/resources/xjb/binding.xml")
            }
        }
    }

    withType<KotlinCompile> {
        dependsOn("wsdl2javaSubscription")
        kotlinOptions.jvmTarget = "17"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Test> {
        useJUnitPlatform {
        }
        testLogging.showStandardStreams = true
    }

    "check" {
        dependsOn("formatKotlin")
    }
}
