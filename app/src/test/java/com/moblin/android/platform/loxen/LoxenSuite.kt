package com.moblin.android.platform.loxen

import com.moblin.android.localized
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class LoxenSuite {
    private val catalogStrings = listOf(
        "\"Official\" uses the widely supported libSRT (version 1.5.3) and \"Moblin\" uses a more energy efficient custom implementation." to "\"Official\" uses the widely supported libSRT (version 1.5.3) and \"Loxen\" uses a more energy efficient custom implementation.",
        "2. Connect Moblin to the computer with a USB cable." to "2. Connect Loxen to the computer with a USB cable.",
        "4. Press Go live in Moblin to start the stream to OBS." to "4. Press Go live in Loxen to start the stream to OBS.",
        "Can be used when using Moblin as a server at home with stable internet connection." to "Can be used when using Loxen as a server at home with stable internet connection.",
        "Connect this device to a computer with a USB cable and run the Moblin Mobcam host tool on the computer to receive the stream. The computer connects to the port above over the cable." to "Connect this device to a computer with a USB cable and run the Moblin Mobcam host tool on the computer to receive the stream. The computer connects to the port above over the cable.",
        "Control Moblin with gimbals that supports DockKit." to "Control Loxen with gimbals that supports DockKit.",
        "Control and monitor Moblin from another device." to "Control and monitor Loxen from another device.",
        "Feel free to join Moblin Discord server or write an issue on Github if you need help or want to give feedback." to "Feel free to join Moblin Discord server or write an issue on Github if you need help or want to give feedback.",
        "Give the webpage access to various data in Moblin, for example chat messages and your location." to "Give the webpage access to various data in Loxen, for example chat messages and your location.",
        "Make sure the Moblin device is relatively near the vehicle." to "Make sure the Loxen device is relatively near the vehicle.",
        "Make sure your DJI device is powered on and that no other apps are connected to it via Bluetooth. Make sure the Moblin device is relatively near the DJI device. If you still dont see your DJI device, turn your DJI device off and then on again." to "Make sure your DJI device is powered on and that no other apps are connected to it via Bluetooth. Make sure the Loxen device is relatively near the DJI device. If you still dont see your DJI device, turn your DJI device off and then on again.",
        "Many thanks from the Moblin developers!" to "Many thanks from the Loxen developers!",
        "Moblin" to "Loxen",
        "Moblin access" to "Loxen access",
        "Moblin identifies itself to the vehicle with this key. Tap the button below to add it to your vehicle." to "Loxen identifies itself to the vehicle with this key. Tap the button below to add it to your vehicle.",
        "Moblin in mouth" to "Loxen in mouth",
        "Moblin is running in background" to "Loxen is running in background",
        "Moblin sends these credentials securely to the paired GoPro over Bluetooth." to "Loxen sends these credentials securely to the paired GoPro over Bluetooth.",
        "Moblin widget." to "Loxen widget.",
        "Moblin will periodically try to switch to the BRB scene if the stream is likely broken, and back to the main scene once everything seems to work again." to "Loxen will periodically try to switch to the BRB scene if the stream is likely broken, and back to the main scene once everything seems to work again.",
        "Moblin will switch to the BRB scene configured above when the current scene's SRT(LA) or RTMP video source is disconnected. Typically enable when using Moblin as SRT(LA) server at home, streaming to OBS on the same computer." to "Loxen will switch to the BRB scene configured above when the current scene's SRT(LA) or RTMP video source is disconnected. Typically enable when using Loxen as SRT(LA) server at home, streaming to OBS on the same computer.",
        "Moblin's web browser and browser widgets use the proxy." to "Loxen's web browser and browser widgets use the proxy.",
        "Moblink" to "Moblink",
        "Moblink relay" to "Moblink relay",
        "Powered by Moblin" to "Powered by Loxen",
        "Press \"Add automatically to Moblin\" on https://cloud.belabox.net SRT(LA) relays (requires login). See screenshot below." to "Press \"Add automatically to Moblin\" on https://cloud.belabox.net SRT(LA) relays (requires login). See screenshot below.",
        "Select %@ if you want the DJI camera to stream to Moblin's RTMP server on this device. Select %@ to make the DJI camera stream to any destination." to "Select %@ if you want the DJI camera to stream to Loxen's RTMP server on this device. Select %@ to make the DJI camera stream to any destination.",
        "Select %@ if you want the GoPro camera to stream to Moblin's RTMP server on this device. Select %@ to make the GoPro camera stream to any destination." to "Select %@ if you want the GoPro camera to stream to Loxen's RTMP server on this device. Select %@ to make the GoPro camera stream to any destination.",
        "Select Server to stream into Moblin, or Custom to stream directly to another RTMP destination." to "Select Server to stream into Loxen, or Custom to stream directly to another RTMP destination.",
        "Send Go live notification to [Moblin website](https://moblin.app/#streamers)" to "Send Go live notification to [Moblin website](https://moblin.app/#streamers)",
        "Source name is the name of the Source in OBS that receives the stream from Moblin." to "Source name is the name of the Source in OBS that receives the stream from Loxen.",
        "Start a screen capture by long-pressing the record button in iOS Control Center and select Moblin." to "Start a screen capture by long-pressing the record button in iOS Control Center and select Loxen.",
        "Support Moblin developers by buying icons. ❤️" to "Support Loxen developers by buying icons. ❤️",
        "The RIST server allows Moblin to receive video streams over the network." to "The RIST server allows Loxen to receive video streams over the network.",
        "The RTMP server allows Moblin to receive video streams over the network. This allows the use of some drones and other cameras as sources." to "The RTMP server allows Loxen to receive video streams over the network. This allows the use of some drones and other cameras as sources.",
        "The WHEP client allows Moblin to receive video streams from a WHEP endpoint." to "The WHEP client allows Loxen to receive video streams from a WHEP endpoint.",
        "The WHIP server allows Moblin to receive video streams over the network." to "The WHIP server allows Loxen to receive video streams over the network.",
        "The name of the Source in OBS that receives the stream from Moblin." to "The name of the Source in OBS that receives the stream from Loxen.",
        "The name of your BRB scene in OBS. Moblin will periodically try to switch from your main scene to this scene if the stream is likely broken." to "The name of your BRB scene in OBS. Loxen will periodically try to switch from your main scene to this scene if the stream is likely broken.",
        "The name of your main scene in OBS. Moblin will periodically try to switch to this scene from your BRB scene if the stream is likely working." to "The name of your main scene in OBS. Loxen will periodically try to switch to this scene from your BRB scene if the stream is likely working.",
        "Use Moblin as a low latency camera in OBS Studio over USB." to "Use Loxen as a low latency camera in OBS Studio over USB.",
        "Use phones as additional SRTLA and RIST bonding connections. Install Moblink on Android phones to use them." to "Use phones as additional SRTLA and RIST bonding connections. Install Moblink on Android phones to use them.",
        "Widgets in selected scene will be shown on the Moblin device the remote control assistant is connected to." to "Widgets in selected scene will be shown on the Loxen device the remote control assistant is connected to.",
        "[Moblin website](https://moblin.app/#streamers)" to "[Moblin website](https://moblin.app/#streamers)",
        "⚠️ Allow Moblin to access your location in iOS Settings to see the current WiFi network." to "⚠️ Allow Loxen to access your location and turn on location in Android settings to see the current WiFi network.",
        "⚠️ Allow Moblin to access your location in iOS Settings to use location." to "⚠️ Allow Loxen to access your location and turn on location in Android settings to use location.",
        "⚠️ MetalPetal does not work when Moblin is in background." to "⚠️ MetalPetal does not work when Loxen is in background.",
        "⚠️ Moblin is not configured to stream to this stream." to "⚠️ Loxen is not configured to stream to this stream.",
        "⚠️ The \"Moblin\" implementation does not perform well with low latency. Select the \"Official\" implementation at the bottom of this page." to "⚠️ The \"Loxen\" implementation does not perform well with low latency. Select the \"Official\" implementation at the bottom of this page.",
        "🇸🇪 Moblin" to "🇸🇪 Loxen",
        "🍔 Buy Moblin icons to support the devs 🍔" to "🍔 Buy Loxen icons to support the devs 🍔",
        "👍 Buy Moblin icons if you like the app 👍" to "👍 Buy Loxen icons if you like the app 👍",
        "🙈 Buy Moblin icons to hide this message 🙈" to "🙈 Buy Loxen icons to hide this message 🙈",
        "🙏 Buy Moblin icons please =) 🙏" to "🙏 Buy Loxen icons please =) 🙏",
    )

    @Test
    fun everyCatalogStringThatNamesTheAppNamesLoxen() {
        for ((text, expected) in catalogStrings) {
            assertEquals(expected, localized(text), text)
        }
    }

    @Test
    fun onlyMoblinkAndErikServicesKeepTheName() {
        for ((text, _) in catalogStrings) {
            var rest = localized(text).replace("Moblink", "")
            for (phrase in Loxen.keptPhrases) {
                rest = rest.replace(phrase, "")
            }
            assertTrue("Moblin" !in rest, text)
        }
    }

    @Test
    fun formattedStringsAreRenamedAfterFormatting() {
        val text = String.format(
            localized("Select %s if you want the DJI camera to stream to Moblin's RTMP server on this device."),
            localized("Server"),
        )
        assertEquals("Select Server if you want the DJI camera to stream to Loxen's RTMP server on this device.", text)
        assertEquals("⚠️ The \"Loxen\" implementation", localized("⚠️ The \"Moblin\" implementation"))
    }

    @Test
    fun urlsSchemesFilesAndIdentifiersAreKept() {
        for (text in listOf(
            "moblin://?{}",
            "Open moblin://?%7B%7D in the browser.",
            "Pixel_2026-09-26.moblinSettings",
            "Export settings with file extension `.moblinSettings`.",
            "https://moblin.app/chat-bot/",
            "https://github.com/eerimoq/moblin",
            "https://discord.moblin.app",
            "group.com.eerimoq.Moblin",
            "com.eerimoq.Moblin.rist",
            "!moblin scene <name>",
            "MoblinInMouth",
            "Moblin-Assertion",
            "enhancedMoblinSrt",
            "Moblink relay",
            "Moblin Watch",
            "Moblin Remote Control Assistant",
        )) {
            assertEquals(text, localized(text))
        }
        assertEquals("Loxen chat bot help: https://moblin.app/chat-bot/", localized("Moblin chat bot help: https://moblin.app/chat-bot/"))
        assertEquals("Moblin website and Loxen", localized("Moblin website and Moblin"))
    }

    @Test
    fun textWithoutTheNameIsReturnedAsIs() {
        val text = "Streams"
        assertTrue(localized(text) === text)
        assertEquals("", localized(""))
    }

    @Test
    fun moblinIconsAndMascotAreReplacedByTheLoxenIcon() {
        assertEquals("Loxen/AppIcon", Loxen.imagePath("AppIcon"))
        assertEquals("Loxen/AppIcon", Loxen.imagePath("AppIconSanDiego"))
        assertEquals("Loxen/AppIconNoBackground", Loxen.imagePath("AppIconNoBackground"))
        assertEquals("Loxen/AppIconNoBackground", Loxen.imagePath("AppIconKingNoBackground"))
        assertEquals("Loxen/AppIconNoBackground", Loxen.imagePath("MoblinInMouth"))
        assertEquals("Assets/AlertFace", Loxen.imagePath("AlertFace"))
        assertEquals("Assets/ObsLogo", Loxen.imagePath("ObsLogo"))
        for (name in listOf("Loxen/AppIcon.png", "Loxen/AppIconNoBackground.png")) {
            assertTrue(javaClass.classLoader!!.getResource(name) != null, name)
        }
        for (name in listOf("Assets/AppIcon.png", "Assets/AppIconNoBackground.png", "Assets/MoblinInMouth.png")) {
            assertTrue(javaClass.classLoader!!.getResource(name) == null, name)
        }
    }

    @Test
    fun theLicenseIsTheRepositoryLicense() {
        val file = listOf(File("../LICENSE"), File("LICENSE")).first { it.isFile }
        assertEquals(file.readText().replace("\r\n", "\n"), Loxen.license)
        assertTrue(Loxen.license.contains("Copyright (c) 2023 Erik Moqvist"))
    }

    @Test
    fun iosSettingsTextsSayAndroidAndNameLoxen() {
        val expected = mapOf(
            "⚠️ Allow Moblin to access your location in iOS Settings to use location." to
                "⚠️ Allow Loxen to access your location and turn on location in Android settings to use location.",
            "Copy your device name from iOS settings." to "Copy your device name from Android settings.",
            "Download languages in iOS Settings → Apps → Translate → Languages." to
                "Download on-device translation languages in Android settings.",
            "Download enhanced and premium voices in iOS Settings → Accessibility → Live Speech → Preferred Voices." to
                "Download more voices in Android settings → Text-to-speech output.",
            "⚠️ Hijacks volume buttons. You can only change volume in Control Center when enabled." to
                "⚠️ Hijacks volume buttons. You can only change volume in Android settings when enabled.",
        )
        for ((text, android) in expected) {
            assertEquals(android, localized(text), text)
        }
        for (text in Loxen.androidTexts.keys) {
            val android = localized(text)
            assertFalse("iOS" in android || "Control Center" in android || "Moblin" in android, android)
            assertEquals(android, localized(android), text)
        }
    }

    @Test
    fun noAndroidTextIsAlsoAnIosText() {
        for (android in Loxen.androidTexts.values) {
            assertFalse(android in Loxen.androidTexts.keys, android)
        }
    }

    @Test
    fun iosTextsOutsideTheMapAreUntouched() {
        for (text in listOf(
            "  • Select Esperanto as app language in iOS settings.",
            "Enable Override video stabilization to override Settings → Camera → Video stabilization in this scene.",
            "Download languages in iOS Settings.",
        )) {
            assertTrue(localized(text) === text, text)
        }
    }

    @Test
    fun everyIosTextIsStillInTheGeneratedViews() {
        val root = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
        val concatenation = Regex("\"\\s*\\+\\s*\"")
        val sources = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "platform" !in it.invariantSeparatorsPath.split("/") }
            .joinToString("\n") { it.readText().replace(concatenation, "") }
        for (text in Loxen.androidTexts.keys) {
            assertTrue(text in sources, text)
        }
    }
}
