import java.time.Duration
import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val moblinVersion: String = Properties().apply { file("moblin-version.properties").reader().use { load(it) } }
    .getProperty("MARKETING_VERSION")
    ?: error("MARKETING_VERSION not found in moblin-version.properties")

val releaseVersionCode: Provider<Int> = providers.exec {
    commandLine("git", "rev-parse", "--is-shallow-repository")
}.standardOutput.asText.zip(
    providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }.standardOutput.asText,
) { shallow, count ->
    check(shallow.trim() == "false") {
        "The release versionCode is 1000 plus the number of commits of HEAD and needs the full git history"
    }
    1000 + count.trim().toInt()
}

val uploadKeyPropertiesPath: String? = providers.gradleProperty("moblin.uploadKeystoreProperties").orNull
    ?: Properties().apply { rootProject.file("local.properties").takeIf { it.isFile }?.reader()?.use { load(it) } }
        .getProperty("moblin.uploadKeystoreProperties")

val uploadKeyProperties: File? = uploadKeyPropertiesPath?.let { file(it) }?.takeIf { properties ->
    properties.isFile.also { if (!it) logger.warn("moblin.uploadKeystoreProperties: $properties does not exist") }
}

val uploadKey = Properties().apply { uploadKeyProperties?.reader()?.use { load(it) } }

fun uploadKeySetting(name: String): String? =
    (providers.environmentVariable(name).orNull ?: uploadKey.getProperty(name))?.takeIf { it.isNotBlank() }

val uploadKeystore: File? = uploadKeySetting("MOBLIN_UPLOAD_KEYSTORE_BASE64")?.let { encoded ->
    layout.buildDirectory.file("upload-key/upload-keystore.jks").get().asFile.apply {
        parentFile.mkdirs()
        writeBytes(Base64.getMimeDecoder().decode(encoded))
    }
} ?: uploadKeySetting("MOBLIN_UPLOAD_KEYSTORE_FILE")?.let { (uploadKeyProperties?.parentFile ?: rootDir).resolve(it) }
    ?: uploadKeyProperties?.resolveSibling("upload-keystore.jks")

android {
    namespace = "com.moblin.android"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.loxen.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = moblinVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = Regex("""val youTubeRedirectUri =\s*"([^":]+):""")
            .find(file("src/main/java/com/moblin/android/streamingplatforms/youtube/YouTubeAuth.kt").readText())
            ?.groupValues?.get(1)
            ?: error("youTubeRedirectUri not found in YouTubeAuth.kt")
        buildConfigField(
            "boolean",
            "YCBCR_INGEST",
            (project.findProperty("moblin.ycbcrIngest")?.toString()?.toBooleanStrictOrNull() ?: true).toString(),
        )
        buildConfigField(
            "boolean",
            "POOL_TRIM",
            (project.findProperty("moblin.poolTrim")?.toString()?.toBooleanStrictOrNull() ?: true).toString(),
        )
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_static")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = uploadKeystore
                storePassword = uploadKeySetting("MOBLIN_UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = uploadKeySetting("MOBLIN_UPLOAD_KEY_ALIAS") ?: "upload"
                keyPassword = uploadKeySetting("MOBLIN_UPLOAD_KEY_PASSWORD") ?: storePassword
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("upload")
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets["test"].resources.srcDir("src/main/assets")
    sourceSets["test"].resources.exclude("fonts/**")

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { it.versionCode.set(releaseVersionCode) }
    }
}

tasks.register("printReleaseVersion") {
    val versionCode = releaseVersionCode
    val versionName = moblinVersion
    doLast {
        println("versionCode=${versionCode.get()}")
        println("versionName=$versionName")
    }
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(20))
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.crypto.tink:tink-android:1.8.0")
    implementation("com.google.zxing:core:3.5.4")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("androidx.webkit:webkit:1.17.1")
    implementation("com.github.bumptech.glide:gifdecoder:4.16.0")
    implementation("org.maplibre.gl:android-sdk:13.6.1")
    implementation("com.google.mediapipe:tasks-vision:0.10.35")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.filament:filament-android:1.74.1")
    implementation("com.google.android.filament:gltfio-android:1.74.1")
    implementation("com.google.android.filament:filament-utils-android:1.74.1")
    implementation("net.openid:appauth:0.11.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(platform("androidx.compose:compose-bom:2025.03.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

abstract class PageAlignmentCheck : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val packages: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val checker: RegularFileProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @get:javax.inject.Inject
    abstract val execOperations: org.gradle.process.ExecOperations

    @TaskAction
    fun check() {
        val python = listOf("python3", "python", "py").firstOrNull { candidate ->
            runCatching {
                val process = ProcessBuilder(candidate, "--version").redirectErrorStream(true).start()
                process.inputStream.readBytes()
                process.waitFor() == 0
            }.getOrDefault(false)
        } ?: throw GradleException("Python 3 is needed to check that the native libraries are 16 KB page aligned")
        val reportFile = report.get().asFile
        val result = reportFile.outputStream().use { output ->
            execOperations.exec {
                commandLine(listOf(python, checker.get().asFile.path) + packages.files.map { it.path })
                standardOutput = output
                errorOutput = output
                isIgnoreExitValue = true
            }
        }
        val text = reportFile.readText()
        if (result.exitValue != 0) {
            val failures = text.lines().filter { !it.startsWith("ok ") && it.isNotBlank() }
            throw GradleException("Native libraries are not 16 KB page aligned:\n" + failures.joinToString("\n"))
        }
    }
}

androidComponents {
    onVariants { variant ->
        val suffix = variant.name.replaceFirstChar { it.uppercase() }
        val script = rootProject.layout.projectDirectory.file("tools/check_16kb.py")
        val apkCheck = tasks.register<PageAlignmentCheck>("check${suffix}ApkPageAlignment") {
            packages.from(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK))
            checker.set(script)
            report.set(layout.buildDirectory.file("reports/page-alignment/${variant.name}-apk.txt"))
        }
        val bundleCheck = tasks.register<PageAlignmentCheck>("check${suffix}BundlePageAlignment") {
            packages.from(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.BUNDLE))
            checker.set(script)
            report.set(layout.buildDirectory.file("reports/page-alignment/${variant.name}-bundle.txt"))
        }
        tasks.named { it == "assemble$suffix" }.configureEach { dependsOn(apkCheck) }
        tasks.named { it == "bundle$suffix" }.configureEach { dependsOn(bundleCheck) }
    }
}
