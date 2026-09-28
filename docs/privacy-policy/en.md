# Privacy Policy

Loxen — last updated 28 September 2026.

Loxen is a free and open source IRL live streaming app for Android, developed by Gabbeloxen, an
individual developer in Sweden ("we", "us"). Loxen is based on
[Moblin](https://github.com/eerimoq/moblin) by Erik Moqvist, which is released under the MIT
license. Loxen is an independent project. It is **not affiliated with or endorsed by Moblin or
Erik Moqvist**, and Moblin's own privacy policy does not apply to Loxen. The source code of Loxen
is available at [https://github.com/gabbelitoV2/loxen](https://github.com/gabbelitoV2/loxen).

This Privacy Policy explains what information Loxen (the "app") accesses, collects, stores, uses
and shares, and what choices you have over that information. It is published at
[https://gabbelitov2.github.io/loxen/privacy-policy/en.html](https://gabbelitov2.github.io/loxen/privacy-policy/en.html).

Loxen has no servers of its own, and the data that Loxen handles does not reach us (section 2.4
describes what Google Play shows us as the developer). Your settings and the data described below
are stored on your device. Almost everything Loxen sends over the network is sent only after you
have set up a feature that needs it, and it goes to the streaming platforms and services that you
yourself configure.

In short:

- You do not need an account with us. Loxen itself shows no advertising and has no analytics, no
  advertising ID, no crash-reporting service and no in-app purchases. Web pages and chat images
  that you choose to show can come from third parties (section 4.2.3).
- We (Gabbeloxen) do not receive your streams, your chat, your API Data or your usage of the app;
  see section 2.4 for what Google Play shows us.
- Erik Moqvist does not receive data from Loxen, except in these cases. While Loxen is running, it
  keeps a connection open to a remote control relay server of the Moblin project. This happens by
  default, without any setup, and is described exactly in section 4.2.1, which also says when your
  remote control traffic passes through that server and how to stop the connection. YouTube and
  Twitch sign-in use the Moblin project's app registrations (sections 1 and 2.2), so Google can
  show the owner of the YouTube registration aggregate figures such as the number of API
  requests. Links to Moblin's websites open them in your browser when you tap them.
- When you use face or text effects, Google components built into Loxen send diagnostic data to
  Google (section 4.2.4).
- Everything else leaves your device only when you use a feature that needs it (sections 4.2.2
  and 4.2.3). The exception is the Android and Google system components in section 4.2.4, such as
  the emoji font that Google Play services may download for Loxen when it starts.

## 1. YouTube API Services and the YouTube Terms of Service

Loxen uses **YouTube API Services**. Loxen is an API Client of the YouTube API Services and uses
them to let you manage and go live with your own YouTube live streams from within the app. Loxen
uses the YouTube API Services only after you sign in to YouTube in Loxen.

**By using Loxen, you are agreeing to be bound by the YouTube Terms of Service, available at
[https://www.youtube.com/t/terms](https://www.youtube.com/t/terms).**

Google's use of information received from the YouTube API Services, and any information Google
collects when you sign in with your Google Account, is governed by the **Google Privacy
Policy**, available at
[http://www.google.com/policies/privacy](http://www.google.com/policies/privacy).

Loxen currently signs in to YouTube with the Google OAuth client registration of the upstream
Moblin project. Google's consent screen, and the list of third-party connections in your Google
Account, may therefore show the name "Moblin". Your tokens and your YouTube data go directly
between your device and Google. They are not sent to us, to Erik Moqvist or to the Moblin project,
although Google may show the owner of that registration aggregate figures such as the number of
API requests.

## 2. Information Loxen accesses, collects and stores

### 2.1 YouTube (API Data)

When you choose to connect a YouTube account (Settings → Streams → (your stream) → YouTube →
**Login**, or the same button in the stream wizard), Loxen opens Google's own sign-in page in
your browser or in a browser tab shown over the app (OAuth 2.0 authorization code flow). Loxen
never sees or stores your Google password. Loxen requests the
`https://www.googleapis.com/auth/youtube` scope, and uses it to call the YouTube Data API v3 at
`youtube.googleapis.com`. Through those calls Loxen accesses:

| Data | Why it is accessed |
| --- | --- |
| OAuth 2.0 access token, refresh token and ID token for your Google Account, together with the sign-in state that holds them | To authorize the API calls listed below on your behalf, and to refresh access when it expires |
| Your channel (`channels.list`, `mine=true`, `snippet`): channel handle | To fill in the handle of the channel you are signed in as |
| Your live broadcasts (`liveBroadcasts.list/insert/delete/bind/transition`): titles, thumbnails, scheduled start times, privacy status, broadcast ids, the id of the bound stream and the auto start and auto stop settings | To list, create, schedule, start, stop and delete your live broadcasts from the app |
| Your live streams (`liveStreams.list`, `mine=true`): stream ids, stream keys and ingestion (RTMP) addresses | To set up the stream URL that sends your video to the correct YouTube ingestion endpoint |
| Video details (`videos.list`, `liveStreamingDetails`): concurrent viewer count, actual start and end times and the live chat id | To show the viewer count and live status in the app and, if you set up remote control, on your assistant device (section 4.2.5), and to find the live chat of your stream |
| Live chat bans (`liveChatBans.insert`): the channel id of the chat participant you choose | To ban or time out a viewer when you ask the app to |

Loxen also reads the public live chat of the broadcast. It does this through YouTube's public live
chat web pages on `www.youtube.com`, without your Google Account and without the YouTube Data API,
so it also works when you are not signed in. If you enter a channel handle but are not signed in,
Loxen finds the current live video by loading `https://www.youtube.com/@handle/live`, sending a
YouTube consent cookie value and a desktop browser identification with the request. The chat data
it reads is message text, author display name, author channel id, author badges (for example
moderator, member and owner), Super Chat and Super Sticker amounts, and membership and
gifted-membership events. Chat is used to display chat in the app and in stream overlays, and in
optional chat features such as alerts, text-to-speech, the chat bot, cat printers and remote
control.

What is stored: the sign-in state with the tokens is stored encrypted on your device (section
3.2). The app's settings file stores your channel handle and the ids of the live broadcasts that
you select or schedule in Loxen, or that Loxen finds as your channel's current live video. If you
set up a stream with the stream wizard while signed in, or choose to use a broadcast's ingestion
settings on the stream's YouTube page, the stream URL that contains the ingestion address and
stream key is stored there as well. Logging out, or revoking Loxen's access at Google, does not
remove these values; they are kept until you change them, delete the stream, reset the settings or
clear the app's data. Copies of them are also kept in the streaming history, and they are included
in Android backup (section 3.4) and in exported settings. All other API responses (broadcast
titles, thumbnails and other broadcast details, stream lists, viewer counts and live status) are
kept in memory only while the app is running.

Loxen does **not** access your Google contacts, email, Drive, watch history, subscriptions or any
Google data outside of the YouTube live streaming data listed above.

### 2.2 Other streaming platforms and services

If you configure them, Loxen connects in a comparable way to other platforms and services. For
those services Loxen stores the credentials, tokens, stream keys, channel names and URLs that you
provide, and reads chat, viewer counts and stream status from them. Your use of those services is
governed by their own terms and privacy policies.

- **Twitch.** Signing in opens Twitch's own login page (`id.twitch.tv`) in a web view inside
  Loxen. You type your password on Twitch's page, and Loxen does not read or store it. Loxen asks
  Twitch for permission to read and send chat, read your followers and the live channels you
  follow (to suggest channels to raid), subscriptions, channel point redemptions, hype trains, ad schedules and bits, read your stream key, moderate your chat, and
  manage polls, predictions, raids, commercials, broadcast settings, moderators and VIPs. After
  you sign in, Loxen receives an access token, stores it encrypted (section 3.2) and uses it with
  the Twitch API (`api.twitch.tv`) and Twitch EventSub (`eventsub.wss.twitch.tv`). Chat is read
  anonymously from Twitch's chat server (`irc-ws.chat.twitch.tv`) when you enter a channel name,
  without the token. Loxen also stores the names and ids of the last 10 channels that raided you
  and of the last 10 channels you raided in the settings file, so that you can raid them again.
  Twitch sign-in currently uses the Twitch application registration of the upstream Moblin
  project, so Twitch's authorization page may show the name "Moblin". The token goes directly
  between your device and Twitch.
- **Kick.** Kick sign-in does not use OAuth. Loxen opens Kick's login page (`kick.com/login`) in
  a web view inside Loxen, and after you log in it reads the `session_token` cookie that Kick sets
  and uses it as your login for Kick's website APIs (`kick.com`, `web.kick.com` and
  `search.kick.com`). This session token gives the same access as being logged in on kick.com.
  Loxen stores it encrypted (section 3.2). Chat is read anonymously through Kick's public Pusher
  service (`ws-us2.pusher.com`) when you enter a channel name.
- **SOOP (AfreecaTV).** Loxen uses only the channel name you enter, without a login, to read
  chat and viewer counts from SOOP's servers.
- **Open Streaming Platform.** Chat is read over XMPP from the server address you enter.
- **OBS Studio.** Loxen connects to the OBS WebSocket address you enter. Your OBS WebSocket
  password is used for OBS's challenge-response authentication and is never sent in clear.
- **Your own streaming servers.** Loxen sends your video and audio to the RTMP(S), SRT, SRTLA,
  RIST or WHIP address you enter, including the stream key in that address. Facebook and similar
  services are supported only as RTMP(S) addresses you enter; Loxen has no Facebook sign-in or
  Facebook API. If you enter a preview stream address (with **Show all settings** on, Settings →
  Streams → (your stream) → Preview stream), Loxen also sends a copy of your video and audio at a
  lower resolution (640x360 by default) over WHIP to that address while the preview stream is on,
  also when you are not live. You can start it with the Preview stream quick button, and a
  connected remote control assistant or anyone using web remote control can also start it
  (section 4.2.5).

### 2.3 Information created on or read from your device

Loxen records or processes the following on your device. Unless a feature is marked as optional,
it is part of the app's normal use.

- **Camera and microphone.** Once you have granted the permissions, the camera shows a live
  preview and the microphone feeds the audio level meter as soon as Loxen is open, not only when
  you are live. Video and audio leave your device only when you go live (to the destination you
  configured), turn on the preview stream (section 2.2) or use one of the features listed in
  section 4.2 that sends them, and they are
  saved only when you record, take a snapshot or save a replay. Loxen does not capture your
  screen.
- **Snapshots and photos.** Snapshots (from the quick button, the Snapshot app shortcut or,
  if you enable it, the chat bot) and photos are saved as JPEG images to the shared
  `Pictures/Loxen` folder (on Android 8 and 9, directly in the `Pictures` folder), where your
  gallery app can see them. Loxen does not write your location into photos.
- **Files you choose.** Images, videos, media, settings files and a folder for recordings are
  chosen through the Android photo picker or the Android file picker. Chosen files are copied into
  Loxen's own storage, and a chosen recordings folder is remembered so that Loxen can write
  recordings there. Loxen has no permission to browse your other photos or files.
- **Recordings, replays and logs** (section 3.2).
- **Streaming history.** Each time you go live, Loxen saves an entry with a copy of that stream's
  settings (including its URL and stream key), start and stop times, the amount of data sent,
  bitrate, the highest thermal state and the lowest battery level. At most 101 entries are kept.
- **Location (optional).** Loxen uses your location only when you turn on Settings → Location,
  when the navigation map shows your position, or when you let Loxen fill in the name of the
  Wi-Fi network you are connected to. It uses Android's own location service (the fused, GPS or
  network provider) through Android's location manager, not the Google Play services location
  API. On most devices with Google Play services, the fused and network providers are run by
  Google. If Google Location Accuracy is turned on in your device settings, they may send nearby
  Wi-Fi and cell tower information to Google to work out your position. With location on, Loxen
  uses your position, speed and altitude for the features you set up: text, map and weather
  widgets in your stream, navigation, city and country names, RealtimeIRL, chat bot commands,
  macros and remote control statistics. It stores the accumulated distance, altitude ascent and
  descent, and the privacy regions you define in the settings file. While you are inside a
  privacy region, RealtimeIRL updates stop and map widgets hide your position. Privacy regions do
  not apply to anything else. Text widgets, chat bot commands, remote control statistics,
  Open-Meteo weather requests and city or country lookups keep using your position inside a
  privacy region.
- **Wi-Fi network name.** If Loxen has been granted the location permission, for any reason
  (including the Bluetooth prompt on Android 8 to 11, see section 3.1), it reads the name (SSID)
  and identifier (BSSID) of the Wi-Fi network you are connected to every 10 seconds while it is
  running. The name is kept in memory and is sent only as part of the status that Loxen sends to
  a remote control assistant you connect, or to anyone who opens web remote control while it is
  on (section 4.2.5).
- **Bluetooth accessories (optional).** When you add an accessory, Loxen scans for nearby
  Bluetooth devices and shows them so that you can choose one. It stores an identifier of the
  device you choose in the settings file. Depending on the accessory:
  - **DJI cameras and GoPro:** Loxen sends the camera the Wi-Fi network name and password, the
    RTMP address including your stream key, and video settings, and reads the camera's battery
    level and status.
  - **Tesla:** Loxen stores your car's VIN and a private key that it generates in the settings
    file, sends only the matching public key to the car, and controls and reads charge, drive and
    media state over Bluetooth. Loxen does not use any Tesla cloud service.
  - **Cat printers:** Loxen sends images of chat messages, platform events (with usernames) and
    snapshots to print.
  - **Black Shark coolers:** Loxen reads the cooling state and controls the fan and power.
  - **Heart rate monitors, and cycling and running sensors:** Loxen reads heart rate, power,
    cadence, speed and similar values. They are kept in memory, can be shown in text widgets in
    your stream, and are included in remote control statistics.
- **Health data.** Loxen does not use Health Connect or body sensors. The only health-related data
  is the heart rate and fitness values from Bluetooth sensors you add yourself.
- **Game controllers (optional).** Buttons on a connected controller trigger actions in the app.
- **Speech-to-text (optional).** Only while a widget that needs subtitles or speech-to-text is in
  the current scene, Loxen uses Android's speech recognizer on the microphone audio. On Android 13
  and later it uses on-device recognition when it is available for your language. Otherwise, and
  always on Android 12 and earlier, it uses your device's default speech recognition service
  (usually Google's), which may process the audio on the provider's servers. If on-device
  recognition is not yet available for your language, Loxen also asks the speech service to
  download the language model, which the service's provider (usually Google) delivers.
- **Translation (optional).** Subtitle translation uses Android's on-device translation (Android
  12 and later) and only works for languages that are already downloaded on the device. Loxen
  makes no network requests for it.
- **Text-to-speech (optional).** Reading chat or alerts aloud uses your device's text-to-speech
  engine. If you choose a voice that needs a network connection, the engine's provider processes
  the text. TTS Monster is used only if you enter a TTS Monster token (section 4.2.3).
- **Face and text detection (optional).** Face blur, background blur, Loxen in mouth, beauty,
  VTuber and PNGTuber widgets, face tracking and face-positioned alerts detect faces with Google
  MediaPipe, and text blur detects text with Google ML Kit. Both run on your device with models
  that are included in the app, and camera images are not uploaded. See section 4.2.4 for the
  diagnostic data these components send.
- **Other device information.** Loxen uses your device's model name as its name on the local
  network for Moblink, as its name towards the computer that receives a Mobcam stream, and in the
  file name of exported settings. It reads the battery level and thermal state for status
  displays, the streaming history, Moblink and remote control. It reads the clipboard only when
  you choose to import settings from it. It writes to it when you copy a chat message, a macro
  variable or a link from the deep link creator, or when Android cannot open a recordings folder
  (Loxen then copies the folder's path). A deep link contains the stream URLs and stream keys you
  entered in the deep link creator, and other apps, such as your keyboard, may be able to read the
  clipboard. It uses
  the motion sensors only for a g-force widget or gimbal tracking.
- **Diagnostic logs.** Loxen writes diagnostic messages to Android's system log (logcat) and
  keeps log files on your device. Other apps cannot read the system log, but it can be read over
  USB debugging and is included in bug reports you create. The logs stay on your device unless
  you share a log file or a bug report yourself, or a remote control assistant or web remote
  control that you set up shows them (section 4.2.5).

### 2.4 Information we do not collect

Loxen itself contains no analytics, no advertising, no advertising ID, no tracking SDKs, no
crash-reporting service and no in-app purchases. We do not operate any server, and we do not
receive your API Data, your streams, your chat or your usage of the app.

As the developer, Google Play gives us aggregated install statistics, and crash and ANR (app not
responding) reports (device model, Android version, app version and the error, without your name
or account) from devices that are set to share usage and diagnostics data. It also shows us the
ratings and reviews you post, with the public name on them. If you take part in a test of Loxen
that requires an invitation, we have the email address of the Google Account you were invited
with. This information is provided to us through Google Play Console and is governed by the
[Google Privacy Policy](http://www.google.com/policies/privacy). Apart from that, only what you
send us yourself reaches us (section 9).

Loxen does not access your contacts, call log, SMS, phone number or phone state, or device
identifiers such as the Android ID, IMEI or serial number. It does not request background
location, does not capture your screen and has no Wear OS app. In Loxen, the "Moblin website"
go-live notification option sends nothing to Moblin's website.

Some Google components built into Loxen, and Android System WebView, send diagnostic data to
Google (section 4.2.4).

## 3. Information stored on, accessed from or collected from your device

### 3.1 Android permissions

Android asks you before Loxen can use the permissions below. You can change them at any time in
Settings → Apps → Loxen → Permissions on your device.

| Permission (as Android shows it) | When Loxen asks | What it is used for |
| --- | --- | --- |
| Camera | At every start until granted | Camera preview, streaming, recording, snapshots and face or text effects |
| Microphone | At every start until granted | Audio level meter, streaming, recording and speech-to-text |
| Notifications (Android 13 and later) | At every start until granted | The notification shown while Loxen is live, recording or running in the background |
| Nearby devices (Android 12 and later) | At the first start | Finding and connecting to the Bluetooth accessories you add. Loxen declares to Android that it does not use Bluetooth scans to find your location |
| Location (Android 8 to 11) | At the first start | Older Android versions require the location permission for Bluetooth scanning. Granting it also lets Loxen read the Wi-Fi network name (section 2.3) |
| Location, precise or approximate (you can choose approximate) | When you turn on Settings → Location, when the navigation map shows your position, or when you let Loxen fill in a Wi-Fi network name | The location features in section 2.3 |
| Storage (Android 8 and 9 only) | When you open some settings pages for snapshots and images | Saving snapshots to the `Pictures` folder |

Loxen also declares permissions that Android grants without asking: internet and network access,
viewing Wi-Fi connections (Android 11 and earlier, to read the Wi-Fi network name in section 2.3),
changing network state (to keep mobile data, Wi-Fi and Ethernet connections up at the same time
for bonding), Bluetooth (Android 11 and earlier, to connect to the accessories you add), audio
settings (to use a Bluetooth microphone), foreground services, keeping the device awake,
vibration, and, on Android 16, showing the status notification as a promoted notification.

### 3.2 Where Loxen stores information

- **Encrypted storage (Android Keystore).** Your Twitch access token, your Kick session token and
  your YouTube sign-in state (access, refresh and ID tokens) are stored encrypted in the app's
  private storage with AndroidX EncryptedSharedPreferences (AES-256). The encryption key is kept
  in the Android Keystore and never leaves your device.
- **Settings file.** All other settings are stored in one JSON file in the app's private storage.
  It is protected by Android's app sandbox but is not additionally encrypted by Loxen. Depending
  on what you have set up, it contains stream URLs with stream keys, the OBS WebSocket password,
  Discord webhook addresses, the RealtimeIRL push key, your AI service API key, your TTS Monster
  token, ingest stream keys, your Tesla VIN and private key, saved Wi-Fi network names and
  passwords, GoPro and DJI Wi-Fi passwords, channel names, your privacy regions and accumulated
  distance. It always contains the remote control password, which Loxen generates randomly, and
  the Moblink password, which is 1234 until you change it in Settings → Moblink. Change it before
  you use Moblink.
- **Streaming history and other small files.** The streaming history (section 2.3), the list of
  saved replays, and random identifiers used by Moblink, remote control and SRTLA relays are
  stored next to the settings file. Because each streaming history entry holds a copy of the
  stream's settings, it can also contain a copy of your Twitch access token and, in some cases,
  your Kick session token, which are not additionally encrypted there. Loxen also keeps an
  encrypted copy of your YouTube sign-in state for each such entry.
- **Documents folder.** Recordings, replays, log files, images, media, alert media and VTuber and
  PNGTuber files are stored in the app's private storage. You can see and delete them in
  Android's file manager, where they appear under the name "Loxen" (see section 5.2 for how to
  open it). The same Loxen folder is offered in Android's file picker, so another app can open,
  change or delete these files if you pick them, or the Loxen folder, for that app. By default
  recordings are stored here; you can instead choose a folder in shared storage for each stream.
  Log files are limited to 10 files of 10 MB each.
- **Shared storage.** Snapshots and photos are saved in `Pictures/Loxen` (on Android 8 and 9, in
  `Pictures`), and recordings are saved in the folder you choose, if you choose one. These files
  are not part of the app's private storage.
- **Cache.** Settings exports, log files you share, files you share or import, and temporary
  files are written to the app's cache. Loxen removes some of these at the next start, and
  Android can clear the cache at any time.
- **Exported settings.** When you export settings, the file contains your settings, including
  stream keys and passwords (but not your Twitch, Kick or YouTube tokens), and copies of the
  images, media, alert media, VTuber and PNGTuber files you have added to Loxen. Keep exported
  files private.

### 3.3 Cookies and similar technology

- **YouTube sign-in** happens in your browser or a browser tab shown over the app, so Google and
  YouTube may place, access or recognize cookies and similar identifiers in your browser as part
  of signing in.
- **Twitch and Kick sign-in** happen in web views inside Loxen, so Twitch and Kick place their
  cookies in Loxen's web view storage.
- **Browser widgets and the in-app web browser** load the pages you choose, and those pages can
  store cookies and local storage. All web views in Loxen share one persistent cookie and
  storage area, so a browser widget that loads a Twitch or Kick page may be signed in to that
  site. Opening the Kick login removes all cookies stored by Loxen's web views. Web views never
  get access to your camera, microphone or location.
- **YouTube consent cookie.** Loxen sends a fixed YouTube consent cookie value when it looks up the
  live video of a channel handle. It does not store any cookies from those requests.

Loxen itself does not use cookies for advertising, analytics or cross-site tracking.

### 3.4 Android backup and device transfer

Loxen allows Android backup. Only the folder that holds the settings file, the streaming history,
the replay list and the random identifiers is included in Google's cloud backup and in
device-to-device transfer. That means the backup can contain the secrets listed for the settings
file in section 3.2, and the Twitch or Kick token copies that the streaming history may hold. It
does not contain the encrypted token storage, recordings, replays, log files, media or cookies.
Because the encryption key never leaves your device, after a restore you need to sign in to
Twitch, Kick and YouTube again. Backups are made only if backup is turned on in your device's
settings, and Google stores them under the
[Google Privacy Policy](http://www.google.com/policies/privacy).

### 3.5 Notifications and running in the background

While Loxen is live or recording, it runs a foreground service with an ongoing notification. When
you leave the app, the stream stops unless you have turned on background streaming. Background
chat text-to-speech, background printing and the Moblink relay can also keep Loxen running in the
background with a notification. All of these are off by default. If location is turned on, Loxen
can keep receiving your location while one of these services keeps it running. While Loxen is in
the background, it may show a status notification on the lock screen. That notification shows
only which of live, recording, background chat, Moblink relay and background printing are
active, never chat or personal data. Loxen does not use
push notifications.

## 4. How the information is used, processed and shared

Loxen uses the information described above only to provide the features you activate:
authenticating you with the platforms, listing and managing your live streams, sending your video
and audio to the destination you selected, displaying chat, viewer counts and stream status, and
running the optional features you set up.

All processing that Loxen itself does happens on your device. We do not sell your information,
we do not use it for advertising or profiling, and we do not use it to build user profiles.

### 4.1 Internal parties

Loxen is developed by an individual developer with no employees, servers or internal systems that
receive your data. No information is shared internally.

### 4.2 External parties

Information leaves your device in the cases below.

#### 4.2.1 Without any setup: the Moblin remote control relay

On a new install, without any setup, Loxen opens a connection when it starts to the remote
control relay server `wss://moblin.mys-lang.org/moblin-remote-control-relay`. This server belongs
to the upstream Moblin project, and we understand that it is run by Erik Moqvist. Loxen does not
operate it. This is a default inherited from Moblin's remote control assistant feature.

- **When:** while Loxen is running. The connection is reopened when you return to the app or
  change stream, and is kept alive with a message every 10 seconds.
- **What is sent:** a random identifier that Loxen creates once and then sends on every connection
  (the bridge id). Because it stays the same, the server can recognize your installation across
  connections and IP addresses. It is stored in the settings file, so it is also kept in Android
  backup (section 3.4) and in exported settings, and a new one is created only when you reset the
  settings or clear the app's data. The server also sees the technical connection information any
  server sees, such as your IP address and the time of the connection. No settings, video, audio,
  chat or location is sent over this connection.
- **When more is sent:** your remote control traffic passes through this server in two cases. The
  first is when you set up another device as a remote control streamer and give it the relay
  address shown in Loxen's remote control settings. The second is when you set up Loxen as a
  remote control streamer and enter a relay address on `moblin.mys-lang.org`, for example one
  shown by another Moblin or Loxen device, or one used by the Moblin Remote Control Assistant
  website that Loxen links to. The server then carries everything listed for remote control in
  section 4.2.5, in both directions, including viewer counts, the Wi-Fi network name, your exact
  coordinates, chat, log messages and preview images. This traffic is encrypted between each
  device and the server, but not end to end, so the server can read it. Only the Twitch access token sent with "Reliable
  chat and events" is encrypted end to end.
- **How to stop it:** Loxen currently has no single switch for this connection. To stop it, turn
  on **Show all settings** at the bottom of Settings, open Settings → Remote control → Assistant,
  tap **Create**, choose the new entry as **Current streamer**, and then set **Current streamer**
  back to "-- None --". You can then delete the entry. The connection stays off until you choose a
  current streamer whose relay is turned on (the default for a new entry), or until you reset the
  settings or clear the app's data, which turn it back on.

#### 4.2.2 Streaming platforms and servers you configure

- **Google / YouTube** — API requests, sign-in and the live chat requests described in section
  2.1, and your video and audio stream when you go live on YouTube. Governed by the
  [Google Privacy Policy](http://www.google.com/policies/privacy).
- **Twitch, Kick, SOOP, Open Streaming Platform and OBS Studio** — as described in section 2.2.
- **Your own RTMP(S), SRT, SRTLA, RIST and WHIP servers** — your video, audio and stream key, when
  you go live, and to a preview stream address while the preview stream is on (section 2.2).
  With SRTLA or RIST bonding, the stream is sent over several connections at once, including
  mobile data.

#### 4.2.3 Optional features and third-party services

Each of these is used only if you set up the feature, and is governed by that provider's own
privacy policy.

| Service | What is sent | When |
| --- | --- | --- |
| Emote, badge, avatar and GIF image hosts (for example Twitch, Kick, YouTube, BTTV and 7TV image servers) | Image requests, which reveal your IP address | When chat or emote effects show images from those hosts |
| BTTV (`api.betterttv.net`, also used for FFZ emotes) and 7TV (`7tv.io`) | The platform and channel id | When BTTV, FFZ or 7TV emotes are turned on for a stream (off by default) |
| Kick badge images (`raw.githubusercontent.com`) | Image requests | When Kick chat shows badges |
| decapi.me | Twitch usernames | When a cat printer prints Twitch chat with avatars |
| Any address a chat user sends with the chat bot "fax" command | A request from your device, which reveals your IP address | When the chat bot is on and a permitted user (by default moderators) uses the command |
| AI service: by default Google Gemini (`generativelanguage.googleapis.com`), or another OpenAI-compatible address you enter | Your API key, the personality text, and the viewer's question or the alert text | When a permitted chat user uses the chat bot AI command (the request is sent even if you have not entered an API key), or when an alerts widget uses AI |
| TTS Monster (`api.console.tts.monster`) | Your token, the chosen voice and the chat or alert text to speak | When you have entered a token and chosen a TTS Monster voice |
| Discord (webhooks you enter) | A JPEG snapshot of the camera or stream and a message | When you take a snapshot (by default only while live), when the chat bot takes one, or when you go live with a go-live notification set up |
| RealtimeIRL (`rtirl.com`) | Your push key, exact latitude and longitude, speed, altitude and time | While live, with RealtimeIRL turned on, outside your privacy regions |
| Open-Meteo (`api.open-meteo.com`) | Your latitude and longitude, rounded to about 11 m, every 10 minutes (every 5 seconds until the first successful answer) | With location on, when a widget, chat bot command, macro or remote control statistics need the weather |
| Android's geocoding service (provided by Google on most devices) | Your exact coordinates, first when a name is needed and then each time you have moved more than 1 km | With location on, when a city or country name is needed |
| Photon by komoot (`photon.komoot.io`) | Your search text and the center of the map, or the coordinates you long-press, and the app's name and version | When you search or long-press in the navigation map |
| OSRM routing (`routing.openstreetmap.de`) | Your current position and the destination, and the app's name and version | When you ask the navigation for a route |
| OpenFreeMap (`tiles.openfreemap.org`) | Map tile requests, which reveal the area shown on the map | When a map widget or the navigation map is shown |
| Google STUN server (`stun.l.google.com`) | Your public IP address | When you use WHIP, WHEP or other WebRTC connections |
| Time server (by default `time.apple.com`, or the one you enter) | A time request, which reveals your IP address | When timecodes are turned on (off by default) |
| Google Search (in the in-app web browser) | The pages you open; text that is not an address is searched on Google | When you use the in-app web browser, whose start page is google.com |
| Web pages in browser widgets | Whatever those pages send. If you turn on "Loxen access" for the widget, Loxen also gives the page your chat messages (username, text and emotes). If you also turn on "Speech to text" for it, the page gets a live transcript of what you say. The page can send this data anywhere | When you add a browser widget |

Loxen allows third parties to serve content in the app: the web pages you open in browser widgets
and in the in-app web browser (whose start page is Google Search), and the emote, badge, avatar
and GIF images shown in chat. Those third parties decide that content, which can include
advertisements, and those web pages can place cookies (section 3.3). Loxen does not choose that
content and receives nothing for it.

Links in the app (for example to Moblin's website, the Moblin Remote Control Assistant and Moblin
Remote Control websites, Discord and GitHub) open in your browser only when you tap them.

#### 4.2.4 Google components and Android system services

- **Google ML Kit (text recognition).** When text blur is used, ML Kit sends diagnostic and usage
  data to Google: device information (manufacturer, model, Android version and build), the app's
  package name and version, a random per-installation identifier that ML Kit stores on your
  device, performance measurements, API settings such as image format and size, and error codes.
  Google describes this in its
  [ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure).
- **Google MediaPipe (face detection).** When one of the face features in section 2.3 is used,
  MediaPipe sends usage statistics to Google: the app's package name and version, the task used,
  the number of processed and dropped frames, processing times, and error details. Loxen cannot
  turn this off.
- ML Kit and MediaPipe send their data through Google's data transport service, which adds device
  model, manufacturer, hardware and build information, language and country settings, mobile
  network operator code, network type and time zone. The data may be sent later than the feature
  was used. Camera images are never part of it. This data goes to Google, not to us, and is
  governed by the [Google Privacy Policy](http://www.google.com/policies/privacy). Nothing is sent
  by these components until one of these features has been used.
- **Android System WebView** may send usage and crash statistics to Google if you have allowed
  usage and diagnostics sharing on your device. Loxen does not opt out of this. Android System
  WebView also checks the pages loaded in Loxen's web views (Twitch and Kick sign-in, browser
  widgets and the in-app web browser) against Google Safe Browsing, which can send information
  about those page addresses to Google. Loxen does not turn this off.
- **Android location service** — see section 2.3. It is controlled by your device's location
  settings.
- **Speech recognition, text-to-speech and geocoding** use the services installed on your device,
  as described in sections 2.3 and 4.2.3.
- **Google Play services** may download an up-to-date emoji font for Loxen.
- **Android backup** is described in section 3.4.

#### 4.2.5 Your viewers, your other devices and accessories

- **Your viewers.** Anything you put in your stream is sent to your streaming platform and can be
  public: your camera and microphone, and any widget that shows chat, your location, speed,
  altitude, city, country, weather or heart rate. If you turn on the chat bot, or use a macro that
  sends a chat message, the message is posted in your Twitch or Kick chat and is public. Chat bot
  custom commands and macro messages can include your city, neighborhood, country, speed,
  altitude, distance, weather, heart rate, workout values and Tesla state, and AI command replies
  include the AI service's answer. Macros can run automatically on events you choose.
- **Remote control (off by default, except the relay connection in section 4.2.1).** If you set
  up remote control, the streamer device sends the assistant device its status (including live,
  recording and muted state, viewer counts, battery level and the Wi-Fi network name), statistics
  (which can include your exact latitude and longitude, time zone, speed, altitude, distance,
  city, country, weather, heart rate and other workout values), chat, log messages and preview
  images (by default one per second), and it lets the assistant control it, including starting
  the preview stream (section 2.2). If you turn on "Reliable chat and events", it also sends
  your Twitch access token, encrypted with a key derived from the remote control password. When
  Loxen is the assistant, it sends each streamer that connects its scenes and its browser, text
  and map widget settings (including browser widget addresses). If you choose a remote scene, it
  also keeps sending the values its text widgets use (such as speed, altitude, distance, city,
  country, weather, heart rate, workout values and Tesla state) and, for map widgets, its
  location, with privacy regions applied only to the map location. A remote control assistant
  and streamer authenticate with the remote control password without sending it. A streamer
  connects to the address you enter: directly to an assistant, through the Moblin relay (section
  4.2.1), or through another relay service whose address you enter, which then receives the same
  traffic under its own privacy policy.
- **Web remote control (off by default).** Web remote control has no password. While it is on,
  Loxen serves a web page over unencrypted HTTP on your local network and announces it there with
  mDNS under the name "moblin". Anyone who can reach your device on that network can open it.
  They can see the status (including the Wi-Fi network name and battery level), settings, log
  messages and preview images, and control the app, including starting the preview stream
  (section 2.2). They can also list, download and delete the recordings in Loxen's default
  recordings folder. Turn it on only on networks you trust.
- **Moblink (off by default).** Loxen finds other devices on your local network with mDNS and
  shows your device's model name to them. A relay device sends your stream over its own mobile
  data connection. The devices exchange a relay id and name, a password check, battery level and
  thermal state. The Moblink password is 1234 until you change it (section 3.2), so change it
  before you use Moblink.
- **Mobcam and local servers (off by default).** Mobcam sends your stream over a USB cable to a
  computer that runs the Moblin Mobcam host tool. It listens only on the device itself, and the
  computer connects to it through the cable, not over your local network. The RTMP, SRT(LA), RIST
  and WHIP ingest servers listen on your local network when you turn them on, and the HTTP proxy
  does so when you let it accept connections from your local network. The RTSP, SRT and WHEP
  ingest clients connect to the camera or server addresses you enter, using any user name,
  password or stream id in the address to log in, to receive video for your scenes.
- **Bluetooth accessories** receive the data described in section 2.3.

We may disclose information if required to do so by law, but in practice the only user
information we hold is what is described in sections 2.4 and 9.

## 5. Data retention, deletion and revoking access

### 5.1 Retention

Data obtained from the YouTube API Services is kept as follows. The tokens are kept until you log
out, delete the stream or clear the app's data, and Loxen also deletes them when Google rejects
them because access was revoked. Copies of the tokens kept for streaming history entries are
kept until you delete those entries, and a stream duplicated from a signed-in stream keeps its
own copy until you log out of it or delete it. The channel handle, broadcast ids and stream URL
described in section 2.1 are kept in the settings file until you change them, delete the
stream, reset the settings or clear the app's data; logging out or revoking access does not
remove them. Other API responses (broadcast details, streams, chat and viewer counts) are kept in
memory only while the app is running.

Twitch and Kick tokens are kept until you log out, delete the stream or clear the app's data, and
copies in streaming history entries are kept until you delete those entries. The settings file,
the streaming history (at most 101 entries), replays (at most 500) and recordings are kept until
you delete them. Log files are rotated, with at most 10 files of 10 MB each. Snapshots and photos
in `Pictures/Loxen` (or `Pictures` on Android 8 and 9) are kept until you delete them. Data that
you send to a third-party service is kept according to that service's own policy.

Support emails are kept for up to 12 months after the conversation ends (section 9). Tester email
addresses are kept in Google Play Console while you take part in a test, and we remove yours if
you ask us to.

### 5.2 Deleting stored data

- **To delete your YouTube tokens:** open Settings → Streams → (your stream) → YouTube in Loxen
  and tap **Logout**. This deletes the access, refresh and ID tokens stored for that stream. If
  you went live with the stream while signed in, encrypted copies kept for its streaming history
  entries remain until you delete those entries (see below) and restart Loxen. Logging out does
  not remove the channel handle, broadcast ids or stream URL in the settings (section 2.1), and it
  does not revoke access at Google; see section 5.3 for that. Logout only affects the stream you
  are in. A stream that you duplicated from it, or that you signed in to separately, keeps its own
  tokens, so log out of each of those streams as well (or delete them).
- **To delete your Twitch or Kick token:** open Settings → Streams → (your stream) → Twitch or
  Kick and tap **Logout**. This deletes the token stored for that stream, but does not remove the
  copies kept in streaming history entries (delete those entries as described below), does not
  revoke the token at Twitch or Kick, and does not remove the Twitch or Kick cookies in Loxen's
  web views (see below). Logout only affects the stream you are in. A stream that you duplicated
  from it, or that you signed in to separately, keeps its own tokens, so log out of each of those
  streams as well (or delete them).
- **To delete web view cookies:** open Settings → Streams → (a stream in which you are not logged
  in to Kick) → Kick and tap **Login**. Opening Kick's login page removes all cookies stored by
  Loxen's web views, including Twitch and Kick sign-in cookies; close the page without logging
  in. This does not remove local storage saved by web pages; to remove that as well, clear the
  app's storage as described below.
- **To delete a stream:** in Settings → Streams, swipe left on a stream that is not the active
  one and tap **Delete**. Its tokens are removed from your device the next time Loxen starts.
  Copies of the stream in the streaming history are not removed.
- **To delete the streaming history:** turn on **Show all settings** at the bottom of Settings,
  open Settings → Streaming history, and swipe left on each entry and tap **Delete**. The totals
  at the top of the page (number of streams, total time and data sent) are not reset; clear the
  app's storage to remove them.
- **To delete recordings, replays and log files:** open Settings → Recordings and tap **Default
  recordings directory** or **Replays directory**, or turn on **Show all settings** and open
  Settings → Debug → **Logs directory**. This opens Android's file manager at the Loxen folder,
  where you can delete the files. On some devices, the Loxen folder is also listed in the Files
  app. A replay can also be deleted with the trash button on the replay preview; its file is
  removed at the next start.
- **To delete snapshots and photos:** delete them from `Pictures/Loxen`, or `Pictures` on Android
  8 and 9, with your gallery or Files app.
- **To stop the connection to the Moblin relay:** follow the steps in section 4.2.1.
- **To reset the settings:** tap **Reset settings** at the bottom of Settings. This replaces your
  settings with the defaults, and the tokens of the old streams are deleted at the next start. It
  does not delete the streaming history, replays, recordings, logs, snapshots or cookies, and it
  turns the connection to the Moblin relay back on with a new bridge id (section 4.2.1).
- **To delete all app data at once:** open Settings → Apps → Loxen → Storage (called Storage &
  cache on some devices) and tap **Clear storage** (called **Clear data** on some devices), or
  uninstall Loxen. Android then removes the app's private storage: the settings file, streaming
  history, encrypted tokens and their Keystore key, recordings, replays and logs in the app's
  storage, the cache, and web view cookies and storage. Snapshots and photos in `Pictures/Loxen`
  or `Pictures`, recordings in a folder you chose, and exported settings files you saved elsewhere
  are not removed. A Google cloud backup made earlier is kept by Google until it expires or you
  delete it in your Google Account's backup settings.

When the retention period for a given type of data expires, or when you perform any of the
actions above, the data is deleted or destroyed.

### 5.3 Revoking access to your accounts

You can revoke Loxen's access to your Google Account data at any time from the Google security
settings page at
[https://security.google.com/settings/security/permissions](https://security.google.com/settings/security/permissions),
which opens the list of third-party apps and services in your Google Account. You can also go
there directly:

[https://myaccount.google.com/connections?filters=3,4&hl=en](https://myaccount.google.com/connections?filters=3,4&hl=en)

Select the entry for the app in the list of third-party apps and services and remove its access.
Because Loxen uses the Moblin project's registration (section 1), the entry may be shown as
"Moblin", and removing it also removes access for the Moblin app if you use it with the same
Google Account. After revoking access, any token still stored on your device stops working; use
the **Logout** button described above to delete it from your device as well.

For Twitch, you can remove the app's access under Connections in your Twitch account settings,
where it may also be shown as "Moblin". For Kick, logging out in Loxen does not end the session on
Kick's side; use the account security options Kick provides to end it.

## 6. Security

- Twitch, Kick and YouTube tokens are stored encrypted with a key that is kept in the Android
  Keystore and never leaves your device, except the Twitch and Kick token copies in the streaming
  history (section 3.2), which are not additionally encrypted.
- Everything else Loxen stores is kept in the app's private storage, which Android's app sandbox
  protects from other apps, except the files in the Loxen folder, which another app can reach only
  if you pick them for it in Android's file picker (section 3.2). The settings file, streaming
  history and exported settings are not additionally encrypted by Loxen, and they can contain
  secrets such as stream keys and passwords. The same applies to the parts of them included in
  Android backup (section 3.4).
- Anyone who can use your unlocked device can see recordings, replays and logs through Android's
  file manager.
- Loxen's connections to the APIs and chat services of Google, YouTube, Twitch, Kick and SOOP, to
  the Moblin relay and to the web services in section 4.2.3 at their default addresses use HTTPS
  or encrypted WebSockets.
  Time server and STUN requests are not encrypted. The stream wizard, and applying a YouTube
  broadcast's ingestion settings, fill in unencrypted RTMP addresses for YouTube and Twitch (for
  example `rtmp://a.rtmp.youtube.com/live2`). With those addresses, your video, audio and stream
  key are sent to the platform unencrypted unless you change the address to RTMPS. Addresses that
  you or your chat provide (your own servers, preview stream and ingest addresses, OBS, browser
  widgets, chat images, the fax command, and an AI service or RealtimeIRL base URL you change) use
  whatever transport the address specifies, and Loxen allows unencrypted ones such as
  RTMP, SRT without a passphrase, HTTP and plain WebSocket. Choose encrypted transports (for
  example RTMPS, SRT with a passphrase or HTTPS) when you can.
- Features that work directly on your local network are not encrypted: Moblink (plain
  WebSocket; devices pair using the Moblink password, which you should change from the default
  1234),
  the remote control assistant (plain WebSocket), web remote control (HTTP, without a
  password), the local RTMP, SRT(LA) and RIST ingest servers, the requests to the WHIP ingest
  server, and the HTTP proxy when it accepts connections from your local network. A remote control
  streamer uses the address you enter. The Moblin relay uses an encrypted WebSocket, but its
  traffic is not end-to-end encrypted (section 4.2.1). Mobcam sends your stream unencrypted over
  the USB cable. Use these features only on networks you trust.
- The lock screen notification shows only which features are active (section 3.5), never chat
  or personal data.

No security measure is perfect. Because we have no servers, there is no Loxen server with your
data that could be breached, but the services you configure have their own security.

## 7. Children

Loxen is not directed at children under the age of 13, and we do not knowingly collect
information from children. The streaming platforms that Loxen connects to have their own age
requirements.

## 8. Changes to this Privacy Policy

We may update this Privacy Policy from time to time. The date at the top of this page shows when
it was last changed, and the history of changes is public in the Loxen repository on GitHub.
Continued use of Loxen after a change means you accept the updated policy.

## 9. Contact

If you have questions about this Privacy Policy, about the data Loxen accesses, or if you want
help deleting your data, contact us:

- Email: [boxcloudproject@gmail.com](mailto:boxcloudproject@gmail.com)
- GitHub: [https://github.com/gabbelitoV2/loxen/issues](https://github.com/gabbelitoV2/loxen/issues)

GitHub issues are public, so do not post personal information, stream keys or other secrets in
them; use email for private questions. If you contact us, we use your email address and message
only to answer you.

We process the personal data described in section 2.4 and in this section on the basis of our
legitimate interest in answering you and in developing and testing Loxen (Article 6(1)(f) GDPR).
Emails are stored with our email provider (Google Gmail), and GitHub issues are stored and
published by GitHub. Google (Gmail and Google Play Console) and GitHub may process these data in
the United States. These transfers rely on the EU–US Data Privacy Framework adequacy decision
(Article 45 GDPR), under which Google LLC and GitHub, Inc. are certified. We keep support emails
for up to 12 months after the conversation ends, or delete them earlier if you ask. You have the
right to access, correct or delete the personal data we hold about you, to restrict or object to
its processing and to receive a copy of it; contact us to use these rights. You can also lodge a
complaint with the Swedish Authority for Privacy Protection (IMY),
[https://www.imy.se](https://www.imy.se).

When you use face or text effects, the Google components built into Loxen send the diagnostic
data described in section 4.2.4 to Google. We include these components on the basis of our
legitimate interest in offering these effects on your device (Article 6(1)(f) GDPR). Google uses
that data as described in its
[ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure) and the
[Google Privacy Policy](http://www.google.com/policies/privacy), and may process it in the United
States. You can object by not using these effects.

By default, Loxen connects to the Moblin remote control relay described in section 4.2.1, which
receives the bridge id and sees your IP address. Loxen keeps this default from Moblin so that
remote control through the relay works without setup, on the basis of our legitimate interest in
offering that feature (Article 6(1)(f) GDPR). We do not operate the relay server and cannot say
where it is hosted; its operator processes these data under its own responsibility. You can object
at any time by stopping the connection as described in section 4.2.1.

Data controller: Gabbeloxen, Sweden.
