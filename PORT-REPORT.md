# Port report

Generated 2026-10-09T00:50:26+00:00

## Summary

| tier | done | error | stale | pending | skipped |
|---|---|---|---|---|---|
| logic | 152 | 0 | 0 | 0 | 0 |
| platform | 55 | 0 | 0 | 0 | 0 |
| test | 68 | 0 | 0 | 0 | 0 |
| media | 161 | 0 | 0 | 0 | 0 |
| ui | 361 | 0 | 0 | 0 | 0 |
| apple_only | 1 | 0 | 0 | 0 | 0 |
| skip | 0 | 0 | 0 | 0 | 22 |

## Needs manual work

- Moblin/Various/Model/ModelAppleWatch.swift
  - WatchConnectivity (WCSession) has no Android counterpart; the bodies remain stubs and makePng keeps its TODO()
- Moblin/Various/Model/ModelMoblinWebsite.swift
  - DCAppAttestService (DeviceCheck)
  - DCError
- Moblin/View/Settings/Display/QuickButtons/QuickButtonsButtonSettingsView.swift
  - model.database.objectWillChange.send() (Combine ObservableObjectPublisher on the settings Database)

## Apple only / needs Gabriel

Not port work: the Swift uses an Apple-only service, or the Android counterpart needs Gabriel.

### Apple only

- Common/Various/CommonUtils.swift
  - HealthKit HKQuantityType (heartRateType, distanceCyclingType, distanceWalkingRunningType, stepCountType, activeEnergyBurnedType, runningPowerType, cyclingPowerType, cyclingCadenceType)
- Moblin Watch/Shared/WatchProtocol.swift
  - HealthKit (HKStatistics, HKUnit, heart rate / energy / distance / step / power / cadence quantity types)
- Moblin/Media/HaishinKit/Media/MacScreenCapture.swift
  - no Android counterpart for ScreenCaptureKit SCStream
  - no Android counterpart for ScreenCaptureKit SCStream.stopCapture
  - no Android counterpart for ScreenCaptureKit SCShareableContent
- Moblin/Media/WiFiAware/WiFiAwareReceiver.swift
  - WiFiAware framework (Wi-Fi Aware listening, publishable/subscribable service pairing)
  - Network.framework declarative NetworkListener / NetworkConnection API
- Moblin/Media/WiFiAware/WiFiAwareSender.swift
  - WiFiAware NetworkBrowser / NetworkConnection over UDP (Network.framework on Wi-Fi Aware)
- Moblin/Various/ChatPost.swift
  - WatchConnectivity: WatchProtocolChatHighlight / WatchProtocolChatHighlightKind do not exist on Android, so ChatHighlight.toWatchProtocol() is TODO().
- Moblin/Various/Gimbal.swift
  - DockKit (DockAccessoryManager, DockAccessory, DockAccessory.StateChange, DockAccessory.AccessoryEvent, DockAccessory.Animation): no Android counterpart, replaced with TODO
- Moblin/Various/Model/ModelAppleWatch.swift
  - WatchConnectivity (WCSession, WCSessionDelegate, WCSessionActivationState)
  - WatchMessageToWatch and WatchMessageFromWatch
  - WatchProtocol* payload types (WatchProtocolScene, WatchProtocolChatMessage, WatchProtocolPadelScoreboard, ...)
  - UIImage (UIKit)
- Moblin/Various/Model/ModelMacStatusItem.swift
  - NSBundle.main.builtInPlugInsURL / Bundle(url:) / principalClass plugin bundle loading (MoblinMac.bundle)
  - macCatalyst NSStatusItem bridge (MacStatusItem helper object)
  - UIImage(named:)?.pngData() icon encoding
  - MacStatusItemDelegate conformance declared from an extension
- Moblin/Various/Model/ModelMoblinWebsite.swift
  - DeviceCheck DCAppAttestService.generateKey/attestKey/isSupported (App Attest) - attestedKey() and sendLive() bodies are TODO()
  - DeviceCheck DCAppAttestService.generateAssertion - generateAssertion() body is TODO()
- Moblin/Various/Model/ModelMusic.swift
  - MusicKit MusicAuthorization.request()
  - MusicKit MusicCatalogResourceRequest / MusicCatalogSearchRequest (Song lookup by URL id and search by term)
  - MusicKit ApplicationMusicPlayer (queue assignment/insertion, play, pause, prepareToPlay, skipToNextEntry, skipToPreviousEntry, state.playbackStatus, isPreparedToPlay)
  - MusicKit Song and MusicItemID types (findSong result type kept as placeholder Any?)
- Moblin/Various/Model/ModelStreamDeck.swift
  - StreamDeckKit (StreamDeckSession.setUp, StreamDeckLayout, StreamDeckKeyAreaLayout)
  - UIApplication.canOpenURL
- Moblin/Various/Model/ModelWorkout.swift
  - HealthKit (HKHealthStore, HKWorkoutConfiguration, HKWorkoutSession, HKWorkoutSessionDelegate, HKLiveWorkoutBuilder, HKLiveWorkoutBuilderDelegate, HKLiveWorkoutDataSource, HKQuantitySample, HKUnit, HKSampleType, HKWorkoutSessionState)
  - WatchConnectivity (source of the WatchProtocolWorkoutType workout kinds)
  - ContinuousClock.Instant and HKWorkoutSession state transitions used for sample rate limiting
- Moblin/View/Settings/AppleMusic/AppleMusicSettingsView.swift
  - MusicKit MusicSubscription.subscriptionUpdates
  - MusicKit MusicSubscriptionOffer (musicSubscriptionOffer modifier)
  - MusicKit ApplicationMusicPlayer state and queue (musicPlayer)
- Moblin/View/Settings/StreamDeck/StreamDeckLayoutSettingsView.swift
  - ColorPicker -> TODO("ColorPicker has no Compose counterpart")
- Moblin/View/Settings/WiFiAware/WiFiAwareSettingsView.swift
  - WiFiAware (WAPublishableService.allServices, WASubscribableService.allServices, WAPairedDevice.allDevices, WACapabilities.supportedFeatures, WAFeature.wifiAware)
  - DeviceDiscoveryUI.DevicePairingView
  - DeviceDiscoveryUI.DevicePicker
- MoblinTests/Moblin/Various/Utils/CmTimeSuite.swift
  - no Android counterpart for CMTime preferredTimescale quantization

### Needs Gabriel

- Moblin/Various/Model/ModelStore.swift
  - StoreKit: Product.products(for:), Transaction.updates, VerificationResult, AppStore.sync(), Product.purchase(), Transaction.currentEntitlements, Transaction.finish() (needs Google Play Billing and the icon products in the Play Console)

## Ported files

| Swift | Kotlin | model | seconds |
|---|---|---|---|
| Common/Various/AVAudioPCMBuffer+Extension.swift | app/src/main/java/com/moblin/android/common/various/AVAudioPCMBuffer+Extension.kt | deepseek-flash | 14.5 |
| Common/Various/AudioLevel.swift | app/src/main/java/com/moblin/android/common/various/AudioLevel.kt | deepseek-flash | 25.0 |
| Common/Various/CMBlockBuffer+Extension.swift | app/src/main/java/com/moblin/android/common/various/CMBlockBuffer+Extension.kt | deepseek-flash | 13.3 |
| Common/Various/CMFormatDescription+Extension.swift | app/src/main/java/com/moblin/android/common/various/CMFormatDescription+Extension.kt | deepseek-flash | 10.2 |
| Common/Various/CMSampleBuffer+Extension.swift | app/src/main/java/com/moblin/android/common/various/CMSampleBuffer+Extension.kt | deepseek-flash | 47.1 |
| Common/Various/CommonUtils.swift | app/src/main/java/com/moblin/android/common/various/CommonUtils.kt | deepseek-flash | 44.2 |
| Common/Various/Validate.swift | app/src/main/java/com/moblin/android/common/various/Validate.kt | deepseek-flash | 9.2 |
| Common/View/StreamOverlayIconAndTextView.swift | app/src/main/java/com/moblin/android/common/view/StreamOverlayIconAndTextView.kt | deepseek-flash | 23.0 |
| Common/View/StreamOverlayTextView.swift | app/src/main/java/com/moblin/android/common/view/StreamOverlayTextView.kt | deepseek-flash | 2.3 |
| Common/View/ThermalStateView.swift | app/src/main/java/com/moblin/android/common/view/ThermalStateView.kt | deepseek-flash | 13.9 |
| Moblin Live Activity/Shared/MoblinLiveActivity.swift | app/src/main/java/com/moblin/android/moblinliveactivity/shared/MoblinLiveActivity.kt | deepseek-flash | 4.1 |
| Moblin Watch/Shared/WatchProtocol.swift | app/src/main/java/com/moblin/android/moblinwatch/shared/WatchProtocol.kt | deepseek-flash | 36.8 |
| Moblin Watch/Shared/WatchSettings.swift | app/src/main/java/com/moblin/android/moblinwatch/shared/WatchSettings.kt | deepseek-flash | 122.2 |
| Moblin/Integrations/BlackSharkCooler/BlackSharkCoolerDevice.swift | app/src/main/java/com/moblin/android/integrations/blacksharkcooler/BlackSharkCoolerDevice.kt | deepseek-flash | 82.6 |
| Moblin/Integrations/CatPrinter/AtkinsonDithering.swift | app/src/main/java/com/moblin/android/integrations/catprinter/AtkinsonDithering.kt | deepseek-flash | 9.3 |
| Moblin/Integrations/CatPrinter/CatPrinter.swift | app/src/main/java/com/moblin/android/integrations/catprinter/CatPrinter.kt | deepseek-flash | 114.2 |
| Moblin/Integrations/CatPrinter/CatPrinterCommands.swift | app/src/main/java/com/moblin/android/integrations/catprinter/CatPrinterCommands.kt | deepseek-flash | 61.4 |
| Moblin/Integrations/CatPrinter/CatPrinterCommandsMxw01.swift | app/src/main/java/com/moblin/android/integrations/catprinter/CatPrinterCommandsMxw01.kt | deepseek-flash | 50.7 |
| Moblin/Integrations/CatPrinter/FloydSteinbergDithering.swift | app/src/main/java/com/moblin/android/integrations/catprinter/FloydSteinbergDithering.kt | deepseek-flash | 12.1 |
| Moblin/Integrations/Dji/DjiDevice/DjiDevice.swift | app/src/main/java/com/moblin/android/integrations/dji/djidevice/DjiDevice.kt | deepseek-flash | 17.3 |
| Moblin/Integrations/Dji/DjiDevice/DjiDeviceMessage.swift | app/src/main/java/com/moblin/android/integrations/dji/djidevice/DjiDeviceMessage.kt | deepseek-flash | 9.3 |
| Moblin/Integrations/Dji/DjiDevice/DjiDeviceModel.swift | app/src/main/java/com/moblin/android/integrations/dji/djidevice/DjiDeviceModel.kt | deepseek-flash | 5.7 |
| Moblin/Integrations/Dji/DjiDevice/DjiDeviceScanner.swift | app/src/main/java/com/moblin/android/integrations/dji/djidevice/DjiDeviceScanner.kt | deepseek-flash | 16.8 |
| Moblin/Integrations/Dji/DjiMessage.swift | app/src/main/java/com/moblin/android/integrations/dji/DjiMessage.kt | deepseek-flash | 42.8 |
| Moblin/Integrations/Emotes/Bttv.swift | app/src/main/java/com/moblin/android/integrations/emotes/Bttv.kt | deepseek-flash | 32.3 |
| Moblin/Integrations/Emotes/Emotes.swift | app/src/main/java/com/moblin/android/integrations/emotes/Emotes.kt | deepseek-flash | 42.9 |
| Moblin/Integrations/Emotes/Ffz.swift | app/src/main/java/com/moblin/android/integrations/emotes/Ffz.kt | deepseek-flash | 39.5 |
| Moblin/Integrations/Emotes/Seventv.swift | app/src/main/java/com/moblin/android/integrations/emotes/Seventv.kt | deepseek-flash | 42.8 |
| Moblin/Integrations/GoPro/GoPro.swift | app/src/main/java/com/moblin/android/integrations/gopro/GoPro.kt | deepseek-flash | 6.0 |
| Moblin/Integrations/GoPro/GoProBleProtocol.swift | app/src/main/java/com/moblin/android/integrations/gopro/GoProBleProtocol.kt | deepseek-flash | 48.6 |
| Moblin/Integrations/GoPro/GoProDevice.swift | app/src/main/java/com/moblin/android/integrations/gopro/GoProDevice.kt | deepseek-flash | 98.3 |
| Moblin/Integrations/GoPro/GoProDeviceScanner.swift | app/src/main/java/com/moblin/android/integrations/gopro/GoProDeviceScanner.kt | deepseek-flash | 11.6 |
| Moblin/Integrations/GoPro/Protobuf/live_streaming.pb.swift | app/src/main/java/com/moblin/android/integrations/gopro/protobuf/live_streaming.pb.kt | deepseek-flash | 68.2 |
| Moblin/Integrations/GoPro/Protobuf/network_management.pb.swift | app/src/main/java/com/moblin/android/integrations/gopro/protobuf/network_management.pb.kt | deepseek-flash | 143.8 |
| Moblin/Integrations/GoPro/Protobuf/response_generic.pb.swift | app/src/main/java/com/moblin/android/integrations/gopro/protobuf/response_generic.pb.kt | deepseek-flash | 36.2 |
| Moblin/Integrations/OpenAi/OpenAi.swift | app/src/main/java/com/moblin/android/integrations/openai/OpenAi.kt | deepseek-flash | 76.0 |
| Moblin/Integrations/RealtimeIrl/RealtimeIrl.swift | app/src/main/java/com/moblin/android/integrations/realtimeirl/RealtimeIrl.kt | deepseek-flash | 16.5 |
| Moblin/Integrations/Tesla/TeslaVehicle.swift | app/src/main/java/com/moblin/android/integrations/tesla/TeslaVehicle.kt | deepseek-flash | 167.2 |
| Moblin/Integrations/Tesla/TeslaVehicleScanner.swift | app/src/main/java/com/moblin/android/integrations/tesla/TeslaVehicleScanner.kt | deepseek-flash | 16.2 |
| Moblin/Integrations/TtsMonster/TtsMonster.swift | app/src/main/java/com/moblin/android/integrations/ttsmonster/TtsMonster.kt | deepseek-flash | 41.6 |
| Moblin/Integrations/WorkoutDevice/WorkoutDevice.swift | app/src/main/java/com/moblin/android/integrations/workoutdevice/WorkoutDevice.kt | deepseek-flash | 49.0 |
| Moblin/Integrations/WorkoutDevice/WorkoutDeviceCrankCadence.swift | app/src/main/java/com/moblin/android/integrations/workoutdevice/WorkoutDeviceCrankCadence.kt | deepseek-flash | 20.8 |
| Moblin/Integrations/WorkoutDevice/WorkoutDeviceCyclingPower.swift | app/src/main/java/com/moblin/android/integrations/workoutdevice/WorkoutDeviceCyclingPower.kt | deepseek-flash | 33.6 |
| Moblin/Integrations/WorkoutDevice/WorkoutDeviceCyclingSpeedCadence.swift | app/src/main/java/com/moblin/android/integrations/workoutdevice/WorkoutDeviceCyclingSpeedCadence.kt | deepseek-flash | 24.3 |
| Moblin/Integrations/WorkoutDevice/WorkoutDeviceHeartRate.swift | app/src/main/java/com/moblin/android/integrations/workoutdevice/WorkoutDeviceHeartRate.kt | deepseek-flash | 11.2 |
| Moblin/Integrations/WorkoutDevice/WorkoutDeviceRunning.swift | app/src/main/java/com/moblin/android/integrations/workoutdevice/WorkoutDeviceRunning.kt | deepseek-flash | 25.9 |
| Moblin/Intents/MoblinShortcuts.swift | app/src/main/java/com/moblin/android/intents/MoblinShortcuts.kt | deepseek-flash | 3.2 |
| Moblin/Intents/MuteIntent.swift | app/src/main/java/com/moblin/android/intents/MuteIntent.kt | deepseek-flash | 5.4 |
| Moblin/Intents/SnapshotIntent.swift | app/src/main/java/com/moblin/android/intents/SnapshotIntent.kt | deepseek-flash | 5.4 |
| Moblin/Intents/UnmuteIntent.swift | app/src/main/java/com/moblin/android/intents/UnmuteIntent.kt | deepseek-flash | 9.9 |
| Moblin/Media/AdaptiveBitrate/AdaptiveBitrate.swift | app/src/main/java/com/moblin/android/media/adaptivebitrate/AdaptiveBitrate.kt | deepseek-flash | 26.4 |
| Moblin/Media/AdaptiveBitrate/AdaptiveBitrateRistExperiment.swift | app/src/main/java/com/moblin/android/media/adaptivebitrate/AdaptiveBitrateRistExperiment.kt | deepseek-flash | 67.7 |
| Moblin/Media/AdaptiveBitrate/AdaptiveBitrateSrtBelabox.swift | app/src/main/java/com/moblin/android/media/adaptivebitrate/AdaptiveBitrateSrtBelabox.kt | deepseek-flash | 96.0 |
| Moblin/Media/AdaptiveBitrate/AdaptiveBitrateSrtFight.swift | app/src/main/java/com/moblin/android/media/adaptivebitrate/AdaptiveBitrateSrtFight.kt | deepseek-flash | 54.7 |
| Moblin/Media/HaishinKit/Codec/Audio/AudioEncoder.swift | app/src/main/java/com/moblin/android/media/haishinkit/codec/audio/AudioEncoder.kt | deepseek-flash | 140.0 |
| Moblin/Media/HaishinKit/Codec/Audio/AudioEncoderRingBuffer.swift | app/src/main/java/com/moblin/android/media/haishinkit/codec/audio/AudioEncoderRingBuffer.kt | deepseek-flash | 60.5 |
| Moblin/Media/HaishinKit/Codec/Audio/AudioEncoderSettings.swift | app/src/main/java/com/moblin/android/media/haishinkit/codec/audio/AudioEncoderSettings.kt | deepseek-flash | 66.6 |
| Moblin/Media/HaishinKit/Codec/Video/VTSessionProperty.swift | app/src/main/java/com/moblin/android/media/haishinkit/codec/video/VTSessionProperty.kt | deepseek-flash | 17.8 |
| Moblin/Media/HaishinKit/Codec/Video/VideoDecoder.swift | app/src/main/java/com/moblin/android/media/haishinkit/codec/video/VideoDecoder.kt | deepseek-flash | 59.7 |
| Moblin/Media/HaishinKit/Codec/Video/VideoEncoder.swift | app/src/main/java/com/moblin/android/media/haishinkit/codec/video/VideoEncoder.kt | deepseek-flash | 8.7 |
| Moblin/Media/HaishinKit/Codec/Video/VideoEncoderSettings.swift | app/src/main/java/com/moblin/android/media/haishinkit/codec/video/VideoEncoderSettings.kt | deepseek-flash | 11.4 |
| Moblin/Media/HaishinKit/Extension/AVCaptureColorSpace+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/AVCaptureColorSpace+Extension.kt | deepseek-flash | 13.8 |
| Moblin/Media/HaishinKit/Extension/AVCaptureDevice.Format+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/AVCaptureDevice.Format+Extension.kt | deepseek-flash | 7.2 |
| Moblin/Media/HaishinKit/Extension/AVFrameRateRange+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/AVFrameRateRange+Extension.kt | deepseek-flash | 4.5 |
| Moblin/Media/HaishinKit/Extension/AudioStreamBasicDescription+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/AudioStreamBasicDescription+Extension.kt | deepseek-flash | 18.1 |
| Moblin/Media/HaishinKit/Extension/Bool+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/Bool+Extension.kt | deepseek-flash | 2.3 |
| Moblin/Media/HaishinKit/Extension/CMVideoDimensions+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/CMVideoDimensions+Extension.kt | deepseek-flash | 68.6 |
| Moblin/Media/HaishinKit/Extension/CMVideoFormatDescription+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/CMVideoFormatDescription+Extension.kt | deepseek-flash | 12.4 |
| Moblin/Media/HaishinKit/Extension/CVPixelBuffer+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/CVPixelBuffer+Extension.kt | deepseek-flash | 44.0 |
| Moblin/Media/HaishinKit/Extension/Data+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/Data+Extension.kt | deepseek-flash | 2.9 |
| Moblin/Media/HaishinKit/Extension/ExpressibleByIntegerLiteral+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/ExpressibleByIntegerLiteral+Extension.kt | deepseek-flash | 5.0 |
| Moblin/Media/HaishinKit/Extension/URL+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/URL+Extension.kt | deepseek-flash | 29.5 |
| Moblin/Media/HaishinKit/Extension/VTCompressionSession+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/VTCompressionSession+Extension.kt | deepseek-flash | 23.6 |
| Moblin/Media/HaishinKit/Extension/VTDecompressionSession+Extension.swift | app/src/main/java/com/moblin/android/media/haishinkit/extension/VTDecompressionSession+Extension.kt | deepseek-flash | 34.3 |
| Moblin/Media/HaishinKit/Flv/Flv.swift | app/src/main/java/com/moblin/android/media/haishinkit/flv/Flv.kt | deepseek-flash | 7.2 |
| Moblin/Media/HaishinKit/Media/Audio/AudioUnit.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/audio/AudioUnit.kt | deepseek-flash | 20.4 |
| Moblin/Media/HaishinKit/Media/Audio/BufferedAudio.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/audio/BufferedAudio.kt | deepseek-flash | 74.3 |
| Moblin/Media/HaishinKit/Media/BufferedStats.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/BufferedStats.kt | deepseek-flash | 3.8 |
| Moblin/Media/HaishinKit/Media/DriftTracker.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/DriftTracker.kt | deepseek-flash | 26.9 |
| Moblin/Media/HaishinKit/Media/MacScreenCapture.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/MacScreenCapture.kt | deepseek-flash | 46.4 |
| Moblin/Media/HaishinKit/Media/Processor.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/Processor.kt | deepseek-flash | 15.7 |
| Moblin/Media/HaishinKit/Media/Recorder.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/Recorder.kt | deepseek-flash | 14.9 |
| Moblin/Media/HaishinKit/Media/Video/BufferedVideo.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/BufferedVideo.kt | deepseek-flash | 40.4 |
| Moblin/Media/HaishinKit/Media/Video/Hlg.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/Hlg.kt | deepseek-flash | 22.6 |
| Moblin/Media/HaishinKit/Media/Video/PreviewView.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/PreviewView.kt | deepseek-flash | 44.4 |
| Moblin/Media/HaishinKit/Media/Video/VideoCaptureSession.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/VideoCaptureSession.kt | deepseek-flash | 69.4 |
| Moblin/Media/HaishinKit/Media/Video/VideoEffect.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/VideoEffect.kt | deepseek-flash | 10.6 |
| Moblin/Media/HaishinKit/Media/Video/VideoEffectsProcessor.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/VideoEffectsProcessor.kt | deepseek-flash | 32.4 |
| Moblin/Media/HaishinKit/Media/Video/VideoFpsEstimator.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/VideoFpsEstimator.kt | deepseek-flash | 5.9 |
| Moblin/Media/HaishinKit/Media/Video/VideoLowFpsImage.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/VideoLowFpsImage.kt | deepseek-flash | 29.5 |
| Moblin/Media/HaishinKit/Media/Video/VideoSnapshots.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/VideoSnapshots.kt | deepseek-flash | 63.7 |
| Moblin/Media/HaishinKit/Media/Video/VideoUnit.swift | app/src/main/java/com/moblin/android/media/haishinkit/media/video/VideoUnit.kt | deepseek-flash | 27.6 |
| Moblin/Media/HaishinKit/Mpeg/Adts.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/Adts.kt | deepseek-flash | 84.4 |
| Moblin/Media/HaishinKit/Mpeg/AudioSpecificConfig.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/AudioSpecificConfig.kt | deepseek-flash | 20.4 |
| Moblin/Media/HaishinKit/Mpeg/Avc/AvcNalUnit.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/avc/AvcNalUnit.kt | deepseek-flash | 50.4 |
| Moblin/Media/HaishinKit/Mpeg/Avc/AvcNalUnitPps.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/avc/AvcNalUnitPps.kt | deepseek-flash | 6.5 |
| Moblin/Media/HaishinKit/Mpeg/Avc/AvcNalUnitSei.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/avc/AvcNalUnitSei.kt | deepseek-flash | 64.9 |
| Moblin/Media/HaishinKit/Mpeg/Avc/AvcNalUnitSps.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/avc/AvcNalUnitSps.kt | deepseek-flash | 7.5 |
| Moblin/Media/HaishinKit/Mpeg/Avc/MpegTsVideoConfigAvc.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/avc/MpegTsVideoConfigAvc.kt | deepseek-flash | 27.8 |
| Moblin/Media/HaishinKit/Mpeg/Crc32.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/Crc32.kt | deepseek-flash | 28.5 |
| Moblin/Media/HaishinKit/Mpeg/ElementaryStreamSpecificData.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/ElementaryStreamSpecificData.kt | deepseek-flash | 44.7 |
| Moblin/Media/HaishinKit/Mpeg/Hevc/HevcNalUnit.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/hevc/HevcNalUnit.kt | deepseek-flash | 122.7 |
| Moblin/Media/HaishinKit/Mpeg/Hevc/HevcNalUnitPps.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/hevc/HevcNalUnitPps.kt | deepseek-flash | 3.6 |
| Moblin/Media/HaishinKit/Mpeg/Hevc/HevcNalUnitSei.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/hevc/HevcNalUnitSei.kt | deepseek-flash | 95.9 |
| Moblin/Media/HaishinKit/Mpeg/Hevc/HevcNalUnitSps.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/hevc/HevcNalUnitSps.kt | deepseek-flash | 8.0 |
| Moblin/Media/HaishinKit/Mpeg/Hevc/HevcNalUnitVps.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/hevc/HevcNalUnitVps.kt | deepseek-flash | 6.5 |
| Moblin/Media/HaishinKit/Mpeg/Hevc/MpegTsVideoConfigHevc.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/hevc/MpegTsVideoConfigHevc.kt | deepseek-flash | 59.9 |
| Moblin/Media/HaishinKit/Mpeg/MpegTsAdaptationField.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/MpegTsAdaptationField.kt | deepseek-flash | 25.8 |
| Moblin/Media/HaishinKit/Mpeg/MpegTsAudioConfig.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/MpegTsAudioConfig.kt | deepseek-flash | 51.6 |
| Moblin/Media/HaishinKit/Mpeg/MpegTsPacket.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/MpegTsPacket.kt | deepseek-flash | 58.2 |
| Moblin/Media/HaishinKit/Mpeg/MpegTsPacketizedElementaryStream.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/MpegTsPacketizedElementaryStream.kt | deepseek-flash | 69.5 |
| Moblin/Media/HaishinKit/Mpeg/MpegTsProgramSpecificInformation.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/MpegTsProgramSpecificInformation.kt | deepseek-flash | 42.9 |
| Moblin/Media/HaishinKit/Mpeg/MpegTsReader.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/MpegTsReader.kt | deepseek-flash | 41.1 |
| Moblin/Media/HaishinKit/Mpeg/MpegTsTimecodeGenerator.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/MpegTsTimecodeGenerator.kt | deepseek-flash | 14.5 |
| Moblin/Media/HaishinKit/Mpeg/MpegTsVideoConfig.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/MpegTsVideoConfig.kt | deepseek-flash | 3.0 |
| Moblin/Media/HaishinKit/Mpeg/MpegTsWriter.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/MpegTsWriter.kt | deepseek-flash | 127.3 |
| Moblin/Media/HaishinKit/Mpeg/NalUnitReader.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/NalUnitReader.kt | deepseek-flash | 21.8 |
| Moblin/Media/HaishinKit/Mpeg/NalUnitStream.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/NalUnitStream.kt | deepseek-flash | 42.2 |
| Moblin/Media/HaishinKit/Mpeg/NalUnitWriter.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/NalUnitWriter.kt | deepseek-flash | 16.9 |
| Moblin/Media/HaishinKit/Mpeg/Opus.swift | app/src/main/java/com/moblin/android/media/haishinkit/mpeg/Opus.kt | deepseek-flash | 40.7 |
| Moblin/Media/HaishinKit/Rist/RistStream.swift | app/src/main/java/com/moblin/android/media/haishinkit/rist/RistStream.kt | deepseek-flash | 139.2 |
| Moblin/Media/HaishinKit/Rtmp/Amf/Amf.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/amf/Amf.kt | deepseek-flash | 105.4 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpAbortMessge.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpAbortMessge.kt | deepseek-flash | 46.8 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpAcknowledgementMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpAcknowledgementMessage.kt | deepseek-flash | 29.5 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpAggregateMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpAggregateMessage.kt | deepseek-flash | 6.7 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpAudioMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpAudioMessage.kt | deepseek-flash | 5.2 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpCommandMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpCommandMessage.kt | deepseek-flash | 53.2 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpDataMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpDataMessage.kt | deepseek-flash | 47.3 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpMessage.kt | deepseek-flash | 16.8 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpSetChunkSizeMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpSetChunkSizeMessage.kt | deepseek-flash | 38.7 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpSetPeerBandwidthMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpSetPeerBandwidthMessage.kt | deepseek-flash | 41.8 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpUserControlMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpUserControlMessage.kt | deepseek-flash | 63.6 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpVideoMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpVideoMessage.kt | deepseek-flash | 4.3 |
| Moblin/Media/HaishinKit/Rtmp/Message/RtmpWindowAcknowledgementSizeMessage.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/message/RtmpWindowAcknowledgementSizeMessage.kt | deepseek-flash | 22.8 |
| Moblin/Media/HaishinKit/Rtmp/RtmpChunk.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/RtmpChunk.kt | deepseek-flash | 42.2 |
| Moblin/Media/HaishinKit/Rtmp/RtmpChunkReader.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/RtmpChunkReader.kt | deepseek-flash | 51.6 |
| Moblin/Media/HaishinKit/Rtmp/RtmpConnection.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/RtmpConnection.kt | deepseek-flash | 120.0 |
| Moblin/Media/HaishinKit/Rtmp/RtmpHandshake.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/RtmpHandshake.kt | deepseek-flash | 7.4 |
| Moblin/Media/HaishinKit/Rtmp/RtmpSocket.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/RtmpSocket.kt | deepseek-flash | 65.8 |
| Moblin/Media/HaishinKit/Rtmp/RtmpStream.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/RtmpStream.kt | deepseek-flash | 175.2 |
| Moblin/Media/HaishinKit/Rtmp/RtmpStreamInfo.swift | app/src/main/java/com/moblin/android/media/haishinkit/rtmp/RtmpStreamInfo.kt | deepseek-flash | 36.9 |
| Moblin/Media/HaishinKit/Srt/SrtPerformanceData.swift | app/src/main/java/com/moblin/android/media/haishinkit/srt/SrtPerformanceData.kt | deepseek-flash | 10.1 |
| Moblin/Media/HaishinKit/Srt/SrtSender.swift | app/src/main/java/com/moblin/android/media/haishinkit/srt/SrtSender.kt | deepseek-flash | 86.7 |
| Moblin/Media/HaishinKit/Srt/SrtSocketOption.swift | app/src/main/java/com/moblin/android/media/haishinkit/srt/SrtSocketOption.kt | deepseek-flash | 102.1 |
| Moblin/Media/HaishinKit/Srt/SrtStreamMoblin.swift | app/src/main/java/com/moblin/android/media/haishinkit/srt/SrtStreamMoblin.kt | deepseek-flash | 29.6 |
| Moblin/Media/HaishinKit/Srt/SrtStreamOfficial.swift | app/src/main/java/com/moblin/android/media/haishinkit/srt/SrtStreamOfficial.kt | deepseek-flash | 88.3 |
| Moblin/Media/HaishinKit/Util/AnyUtil.swift | app/src/main/java/com/moblin/android/media/haishinkit/util/AnyUtil.kt | deepseek-flash | 4.0 |
| Moblin/Media/HaishinKit/Util/Atomic.swift | app/src/main/java/com/moblin/android/media/haishinkit/util/Atomic.kt | deepseek-flash | 30.9 |
| Moblin/Media/HaishinKit/Util/BitrateStats.swift | app/src/main/java/com/moblin/android/media/haishinkit/util/BitrateStats.kt | deepseek-flash | 5.2 |
| Moblin/Media/HaishinKit/Util/ByteReader.swift | app/src/main/java/com/moblin/android/media/haishinkit/util/ByteReader.kt | deepseek-flash | 40.4 |
| Moblin/Media/HaishinKit/Util/ByteWriter.swift | app/src/main/java/com/moblin/android/media/haishinkit/util/ByteWriter.kt | deepseek-flash | 35.2 |
| Moblin/Media/HaishinKit/Util/MD5.swift | app/src/main/java/com/moblin/android/media/haishinkit/util/MD5.kt | deepseek-flash | 7.4 |
| Moblin/Media/HaishinKit/Whip/WhipStream.swift | app/src/main/java/com/moblin/android/media/haishinkit/whip/WhipStream.kt | deepseek-flash | 197.0 |
| Moblin/Media/MobcamStream/MobcamStream.swift | app/src/main/java/com/moblin/android/media/mobcamstream/MobcamStream.kt | deepseek-flash | 88.1 |
| Moblin/Media/MobcamStream/MobcamStreamProtocol.swift | app/src/main/java/com/moblin/android/media/mobcamstream/MobcamStreamProtocol.kt | deepseek-flash | 30.7 |
| Moblin/Media/RistServer/RistServer.swift | app/src/main/java/com/moblin/android/media/ristserver/RistServer.kt | deepseek-flash | 7.3 |
| Moblin/Media/RistServer/RistServerClient.swift | app/src/main/java/com/moblin/android/media/ristserver/RistServerClient.kt | deepseek-flash | 3.8 |
| Moblin/Media/RtmpServer/RtmpServer.swift | app/src/main/java/com/moblin/android/media/rtmpserver/RtmpServer.kt | deepseek-flash | 22.0 |
| Moblin/Media/RtmpServer/RtmpServerChunkStream.swift | app/src/main/java/com/moblin/android/media/rtmpserver/RtmpServerChunkStream.kt | deepseek-flash | 19.7 |
| Moblin/Media/RtmpServer/RtmpServerClient.swift | app/src/main/java/com/moblin/android/media/rtmpserver/RtmpServerClient.kt | deepseek-flash | 16.1 |
| Moblin/Media/RtspClient/RtspClient.swift | app/src/main/java/com/moblin/android/media/rtspclient/RtspClient.kt | deepseek-flash | 30.1 |
| Moblin/Media/RtspClient/RtspTransport.swift | app/src/main/java/com/moblin/android/media/rtspclient/RtspTransport.kt | deepseek-flash | 106.0 |
| Moblin/Media/SrtClient/SrtClient.swift | app/src/main/java/com/moblin/android/media/srtclient/SrtClient.kt | deepseek-flash | 8.2 |
| Moblin/Media/Srtla/Client/LocalListener.swift | app/src/main/java/com/moblin/android/media/srtla/client/LocalListener.kt | deepseek-flash | 46.0 |
| Moblin/Media/Srtla/Client/RemoteConnection.swift | app/src/main/java/com/moblin/android/media/srtla/client/RemoteConnection.kt | deepseek-flash | 186.7 |
| Moblin/Media/Srtla/Client/SrtlaClient.swift | app/src/main/java/com/moblin/android/media/srtla/client/SrtlaClient.kt | deepseek-flash | 196.8 |
| Moblin/Media/Srtla/Common/Srt.swift | app/src/main/java/com/moblin/android/media/srtla/common/Srt.kt | deepseek-flash | 39.9 |
| Moblin/Media/Srtla/Common/Srtla.swift | app/src/main/java/com/moblin/android/media/srtla/common/Srtla.kt | deepseek-flash | 9.8 |
| Moblin/Media/Srtla/Server/SrtServer.swift | app/src/main/java/com/moblin/android/media/srtla/server/SrtServer.kt | deepseek-flash | 5.8 |
| Moblin/Media/Srtla/Server/SrtServerClient.swift | app/src/main/java/com/moblin/android/media/srtla/server/SrtServerClient.kt | deepseek-flash | 5.2 |
| Moblin/Media/Srtla/Server/SrtlaServer.swift | app/src/main/java/com/moblin/android/media/srtla/server/SrtlaServer.kt | deepseek-flash | 33.5 |
| Moblin/Media/Srtla/Server/SrtlaServerClient.swift | app/src/main/java/com/moblin/android/media/srtla/server/SrtlaServerClient.kt | deepseek-flash | 105.5 |
| Moblin/Media/Srtla/Server/SrtlaServerClientConnection.swift | app/src/main/java/com/moblin/android/media/srtla/server/SrtlaServerClientConnection.kt | deepseek-flash | 26.2 |
| Moblin/Media/Wav.swift | app/src/main/java/com/moblin/android/media/Wav.kt | deepseek-flash | 15.3 |
| Moblin/Media/Webrtc/WebrtcCommon.swift | app/src/main/java/com/moblin/android/media/webrtc/WebrtcCommon.kt | deepseek-flash | 5.5 |
| Moblin/Media/Webrtc/WebrtcIngestClient.swift | app/src/main/java/com/moblin/android/media/webrtc/WebrtcIngestClient.kt | deepseek-flash | 17.4 |
| Moblin/Media/Webrtc/WhepClient/WhepClient.swift | app/src/main/java/com/moblin/android/media/webrtc/whepclient/WhepClient.kt | deepseek-flash | 7.9 |
| Moblin/Media/Webrtc/WhipServer/WhipServer.swift | app/src/main/java/com/moblin/android/media/webrtc/whipserver/WhipServer.kt | deepseek-flash | 10.1 |
| Moblin/Media/Webrtc/WhipServer/WhipServerClient.swift | app/src/main/java/com/moblin/android/media/webrtc/whipserver/WhipServerClient.kt | deepseek-flash | 6.3 |
| Moblin/Media/WiFiAware/WiFiAwareReceiver.swift | app/src/main/java/com/moblin/android/media/wifiaware/WiFiAwareReceiver.kt | deepseek-flash | 15.7 |
| Moblin/Media/WiFiAware/WiFiAwareSender.swift | app/src/main/java/com/moblin/android/media/wifiaware/WiFiAwareSender.kt | deepseek-flash | 73.0 |
| Moblin/Media/WrappingTimestamp.swift | app/src/main/java/com/moblin/android/media/WrappingTimestamp.kt | deepseek-flash | 9.6 |
| Moblin/MoblinApp.swift | app/src/main/java/com/moblin/android/MoblinApp.kt | deepseek-flash | 88.2 |
| Moblin/Moblink/MoblinkProtocol.swift | app/src/main/java/com/moblin/android/moblink/MoblinkProtocol.kt | claude-opus-5 | 151.2 |
| Moblin/Moblink/MoblinkRelay.swift | app/src/main/java/com/moblin/android/moblink/MoblinkRelay.kt | deepseek-flash | 127.2 |
| Moblin/Moblink/MoblinkScanner.swift | app/src/main/java/com/moblin/android/moblink/MoblinkScanner.kt | deepseek-flash | 56.0 |
| Moblin/Moblink/MoblinkStreamer.swift | app/src/main/java/com/moblin/android/moblink/MoblinkStreamer.kt | deepseek-flash | 122.1 |
| Moblin/Obs/ObsWebSocket.swift | app/src/main/java/com/moblin/android/obs/ObsWebSocket.kt | deepseek-flash | 132.1 |
| Moblin/RemoteControl/RemoteControl.swift | app/src/main/java/com/moblin/android/remotecontrol/RemoteControl.kt | deepseek-flash | 54.8 |
| Moblin/RemoteControl/RemoteControlAssistant.swift | app/src/main/java/com/moblin/android/remotecontrol/RemoteControlAssistant.kt | deepseek-flash | 23.4 |
| Moblin/RemoteControl/RemoteControlRelay.swift | app/src/main/java/com/moblin/android/remotecontrol/RemoteControlRelay.kt | deepseek-flash | 40.2 |
| Moblin/RemoteControl/RemoteControlStreamer.swift | app/src/main/java/com/moblin/android/remotecontrol/RemoteControlStreamer.kt | deepseek-flash | 76.7 |
| Moblin/RemoteControl/RemoteControlWeb.swift | app/src/main/java/com/moblin/android/remotecontrol/RemoteControlWeb.kt | deepseek-flash | 89.1 |
| Moblin/StreamingPlatforms/Kick/KickApi.swift | app/src/main/java/com/moblin/android/streamingplatforms/kick/KickApi.kt | deepseek-flash | 100.2 |
| Moblin/StreamingPlatforms/Kick/KickAuth.swift | app/src/main/java/com/moblin/android/streamingplatforms/kick/KickAuth.kt | deepseek-flash | 47.2 |
| Moblin/StreamingPlatforms/Kick/KickPlatformStatus.swift | app/src/main/java/com/moblin/android/streamingplatforms/kick/KickPlatformStatus.kt | deepseek-flash | 21.6 |
| Moblin/StreamingPlatforms/Kick/KickPusher.swift | app/src/main/java/com/moblin/android/streamingplatforms/kick/KickPusher.kt | deepseek-flash | 32.6 |
| Moblin/StreamingPlatforms/OpenStreamingPlatform/OpenStreamingPlatformChat.swift | app/src/main/java/com/moblin/android/streamingplatforms/openstreamingplatform/OpenStreamingPlatformChat.kt | deepseek-flash | 110.4 |
| Moblin/StreamingPlatforms/Platform.swift | app/src/main/java/com/moblin/android/streamingplatforms/Platform.kt | deepseek-flash | 7.3 |
| Moblin/StreamingPlatforms/Soop/SoopChat.swift | app/src/main/java/com/moblin/android/streamingplatforms/soop/SoopChat.kt | deepseek-flash | 11.3 |
| Moblin/StreamingPlatforms/Soop/SoopPlatformStatus.swift | app/src/main/java/com/moblin/android/streamingplatforms/soop/SoopPlatformStatus.kt | deepseek-flash | 22.6 |
| Moblin/StreamingPlatforms/Twitch/TwitchApi.swift | app/src/main/java/com/moblin/android/streamingplatforms/twitch/TwitchApi.kt | deepseek-flash | 50.3 |
| Moblin/StreamingPlatforms/Twitch/TwitchAuth.swift | app/src/main/java/com/moblin/android/streamingplatforms/twitch/TwitchAuth.kt | deepseek-flash | 5.5 |
| Moblin/StreamingPlatforms/Twitch/TwitchChat.swift | app/src/main/java/com/moblin/android/streamingplatforms/twitch/TwitchChat.kt | deepseek-flash | 30.8 |
| Moblin/StreamingPlatforms/Twitch/TwitchCommon.swift | app/src/main/java/com/moblin/android/streamingplatforms/twitch/TwitchCommon.kt | deepseek-flash | 4.7 |
| Moblin/StreamingPlatforms/Twitch/TwitchEventSub.swift | app/src/main/java/com/moblin/android/streamingplatforms/twitch/TwitchEventSub.kt | deepseek-flash | 41.7 |
| Moblin/StreamingPlatforms/YouTube/YouTubeApi.swift | app/src/main/java/com/moblin/android/streamingplatforms/youtube/YouTubeApi.kt | deepseek-flash | 14.8 |
| Moblin/StreamingPlatforms/YouTube/YouTubeAuth.swift | app/src/main/java/com/moblin/android/streamingplatforms/youtube/YouTubeAuth.kt | deepseek-flash | 10.7 |
| Moblin/StreamingPlatforms/YouTube/YouTubeLiveChat.swift | app/src/main/java/com/moblin/android/streamingplatforms/youtube/YouTubeLiveChat.kt | deepseek-flash | 26.7 |
| Moblin/Various/BluetoothScanner.swift | app/src/main/java/com/moblin/android/various/BluetoothScanner.kt | deepseek-flash | 10.3 |
| Moblin/Various/BondingStatisticsFormatter.swift | app/src/main/java/com/moblin/android/various/BondingStatisticsFormatter.kt | deepseek-flash | 12.5 |
| Moblin/Various/CacheAsyncImage.swift | app/src/main/java/com/moblin/android/various/CacheAsyncImage.kt | deepseek-flash | 13.9 |
| Moblin/Various/ChatBotCommand.swift | app/src/main/java/com/moblin/android/various/ChatBotCommand.kt | deepseek-flash | 10.2 |
| Moblin/Various/ChatPost.swift | app/src/main/java/com/moblin/android/various/ChatPost.kt | deepseek-flash | 13.0 |
| Moblin/Various/ChatTextToSpeech.swift | app/src/main/java/com/moblin/android/various/ChatTextToSpeech.kt | deepseek-flash | 15.2 |
| Moblin/Various/Detection.swift | app/src/main/java/com/moblin/android/various/Detection.kt | deepseek-flash | 21.6 |
| Moblin/Various/FaxReceiver.swift | app/src/main/java/com/moblin/android/various/FaxReceiver.kt | deepseek-flash | 20.4 |
| Moblin/Various/Gimbal.swift | app/src/main/java/com/moblin/android/various/Gimbal.kt | deepseek-flash | 113.9 |
| Moblin/Various/KeepSpeakerAlive.swift | app/src/main/java/com/moblin/android/various/KeepSpeakerAlive.kt | deepseek-flash | 61.1 |
| Moblin/Various/Keychain.swift | app/src/main/java/com/moblin/android/various/Keychain.kt | deepseek-flash | 33.7 |
| Moblin/Various/Logger.swift | app/src/main/java/com/moblin/android/various/Logger.kt | deepseek-flash | 9.6 |
| Moblin/Various/MainTimer.swift | app/src/main/java/com/moblin/android/various/MainTimer.kt | deepseek-flash | 6.7 |
| Moblin/Various/Managers/GForceManager.swift | app/src/main/java/com/moblin/android/various/managers/GForceManager.kt | deepseek-flash | 17.4 |
| Moblin/Various/Managers/GeographyManager.swift | app/src/main/java/com/moblin/android/various/managers/GeographyManager.kt | deepseek-flash | 12.4 |
| Moblin/Various/Managers/Location.swift | app/src/main/java/com/moblin/android/various/managers/Location.kt | deepseek-flash | 35.7 |
| Moblin/Various/Managers/WeatherManager.swift | app/src/main/java/com/moblin/android/various/managers/WeatherManager.kt | deepseek-flash | 10.5 |
| Moblin/Various/Media.swift | app/src/main/java/com/moblin/android/various/Media.kt | deepseek-flash | 44.1 |
| Moblin/Various/MediaPlayer.swift | app/src/main/java/com/moblin/android/various/MediaPlayer.kt | deepseek-flash | 18.2 |
| Moblin/Various/MoblinSettingsUrl.swift | app/src/main/java/com/moblin/android/various/MoblinSettingsUrl.kt | deepseek-flash | 66.4 |
| Moblin/Various/Model/Chat/ChatProvider.swift | app/src/main/java/com/moblin/android/various/model/chat/ChatProvider.kt | deepseek-flash | 10.9 |
| Moblin/Various/Model/Model.swift | app/src/main/java/com/moblin/android/various/model/Model.kt | deepseek-flash | 94.1 |
| Moblin/Various/Model/ModelAppIntents.swift | app/src/main/java/com/moblin/android/various/model/ModelAppIntents.kt | deepseek-flash | 2.9 |
| Moblin/Various/Model/ModelAppMode.swift | app/src/main/java/com/moblin/android/various/model/ModelAppMode.kt | deepseek-flash | 25.7 |
| Moblin/Various/Model/ModelAppleWatch.swift | app/src/main/java/com/moblin/android/various/model/ModelAppleWatch.kt | deepseek-flash | 6.7 |
| Moblin/Various/Model/ModelAudio.swift | app/src/main/java/com/moblin/android/various/model/ModelAudio.kt | deepseek-flash | 35.1 |
| Moblin/Various/Model/ModelAutoSceneSwitcher.swift | app/src/main/java/com/moblin/android/various/model/ModelAutoSceneSwitcher.kt | deepseek-flash | 64.7 |
| Moblin/Various/Model/ModelBlackSharkCoolerDevice.swift | app/src/main/java/com/moblin/android/various/model/ModelBlackSharkCoolerDevice.kt | deepseek-flash | 42.8 |
| Moblin/Various/Model/ModelBluetooth.swift | app/src/main/java/com/moblin/android/various/model/ModelBluetooth.kt | deepseek-flash | 7.5 |
| Moblin/Various/Model/ModelCamera.swift | app/src/main/java/com/moblin/android/various/model/ModelCamera.kt | deepseek-flash | 54.6 |
| Moblin/Various/Model/ModelCatPrinters.swift | app/src/main/java/com/moblin/android/various/model/ModelCatPrinters.kt | deepseek-flash | 51.9 |
| Moblin/Various/Model/ModelChat.swift | app/src/main/java/com/moblin/android/various/model/ModelChat.kt | deepseek-flash | 13.3 |
| Moblin/Various/Model/ModelChatBot.swift | app/src/main/java/com/moblin/android/various/model/ModelChatBot.kt | deepseek-flash | 27.3 |
| Moblin/Various/Model/ModelControlBar.swift | app/src/main/java/com/moblin/android/various/model/ModelControlBar.kt | deepseek-flash | 37.0 |
| Moblin/Various/Model/ModelDisconnectProtection.swift | app/src/main/java/com/moblin/android/various/model/ModelDisconnectProtection.kt | deepseek-flash | 10.7 |
| Moblin/Various/Model/ModelDjiDevice.swift | app/src/main/java/com/moblin/android/various/model/ModelDjiDevice.kt | deepseek-flash | 55.9 |
| Moblin/Various/Model/ModelFaceBackgroundImage.swift | app/src/main/java/com/moblin/android/various/model/ModelFaceBackgroundImage.kt | deepseek-flash | 10.6 |
| Moblin/Various/Model/ModelGameController.swift | app/src/main/java/com/moblin/android/various/model/ModelGameController.kt | deepseek-flash | 75.9 |
| Moblin/Various/Model/ModelGimbal.swift | app/src/main/java/com/moblin/android/various/model/ModelGimbal.kt | deepseek-flash | 45.4 |
| Moblin/Various/Model/ModelGoProDevice.swift | app/src/main/java/com/moblin/android/various/model/ModelGoProDevice.kt | deepseek-flash | 48.3 |
| Moblin/Various/Model/ModelHttpProxy.swift | app/src/main/java/com/moblin/android/various/model/ModelHttpProxy.kt | deepseek-flash | 27.8 |
| Moblin/Various/Model/ModelKeyboard.swift | app/src/main/java/com/moblin/android/various/model/ModelKeyboard.kt | deepseek-flash | 42.3 |
| Moblin/Various/Model/ModelKick.swift | app/src/main/java/com/moblin/android/various/model/ModelKick.kt | deepseek-flash | 16.1 |
| Moblin/Various/Model/ModelLiveActivity.swift | app/src/main/java/com/moblin/android/various/model/ModelLiveActivity.kt | deepseek-flash | 37.7 |
| Moblin/Various/Model/ModelLocation.swift | app/src/main/java/com/moblin/android/various/model/ModelLocation.kt | deepseek-flash | 32.3 |
| Moblin/Various/Model/ModelLog.swift | app/src/main/java/com/moblin/android/various/model/ModelLog.kt | deepseek-flash | 35.3 |
| Moblin/Various/Model/ModelMacStatusItem.swift | app/src/main/java/com/moblin/android/various/model/ModelMacStatusItem.kt | deepseek-flash | 25.4 |
| Moblin/Various/Model/ModelMacros.swift | app/src/main/java/com/moblin/android/various/model/ModelMacros.kt | deepseek-flash | 23.1 |
| Moblin/Various/Model/ModelMediaPlayer.swift | app/src/main/java/com/moblin/android/various/model/ModelMediaPlayer.kt | deepseek-flash | 10.8 |
| Moblin/Various/Model/ModelMoblinWebsite.swift | app/src/main/java/com/moblin/android/various/model/ModelMoblinWebsite.kt | deepseek-flash | 28.9 |
| Moblin/Various/Model/ModelMoblink.swift | app/src/main/java/com/moblin/android/various/model/ModelMoblink.kt | deepseek-flash | 50.7 |
| Moblin/Various/Model/ModelMusic.swift | app/src/main/java/com/moblin/android/various/model/ModelMusic.kt | deepseek-flash | 40.1 |
| Moblin/Various/Model/ModelNavigation.swift | app/src/main/java/com/moblin/android/various/model/ModelNavigation.kt | deepseek-flash | 16.5 |
| Moblin/Various/Model/ModelObs.swift | app/src/main/java/com/moblin/android/various/model/ModelObs.kt | deepseek-flash | 96.2 |
| Moblin/Various/Model/ModelPhotoShoot.swift | app/src/main/java/com/moblin/android/various/model/ModelPhotoShoot.kt | deepseek-flash | 16.8 |
| Moblin/Various/Model/ModelPictureInPicture.swift | app/src/main/java/com/moblin/android/various/model/ModelPictureInPicture.kt | deepseek-flash | 36.4 |
| Moblin/Various/Model/ModelRecording.swift | app/src/main/java/com/moblin/android/various/model/ModelRecording.kt | deepseek-flash | 12.7 |
| Moblin/Various/Model/ModelRemoteControl.swift | app/src/main/java/com/moblin/android/various/model/ModelRemoteControl.kt | deepseek-flash | 169.6 |
| Moblin/Various/Model/ModelReplay.swift | app/src/main/java/com/moblin/android/various/model/ModelReplay.kt | deepseek-flash | 8.1 |
| Moblin/Various/Model/ModelRistServer.swift | app/src/main/java/com/moblin/android/various/model/ModelRistServer.kt | deepseek-flash | 10.7 |
| Moblin/Various/Model/ModelRtmpServer.swift | app/src/main/java/com/moblin/android/various/model/ModelRtmpServer.kt | deepseek-flash | 6.5 |
| Moblin/Various/Model/ModelRtspClient.swift | app/src/main/java/com/moblin/android/various/model/ModelRtspClient.kt | deepseek-flash | 4.5 |
| Moblin/Various/Model/ModelScene.swift | app/src/main/java/com/moblin/android/various/model/ModelScene.kt | deepseek-flash | 46.3 |
| Moblin/Various/Model/ModelScoreboard.swift | app/src/main/java/com/moblin/android/various/model/ModelScoreboard.kt | deepseek-flash | 104.6 |
| Moblin/Various/Model/ModelScreenCapture.swift | app/src/main/java/com/moblin/android/various/model/ModelScreenCapture.kt | deepseek-flash | 45.0 |
| Moblin/Various/Model/ModelSettingsImportExport.swift | app/src/main/java/com/moblin/android/various/model/ModelSettingsImportExport.kt | deepseek-flash | 3.8 |
| Moblin/Various/Model/ModelSettingsUrl.swift | app/src/main/java/com/moblin/android/various/model/ModelSettingsUrl.kt | deepseek-flash | 52.3 |
| Moblin/Various/Model/ModelSnapshot.swift | app/src/main/java/com/moblin/android/various/model/ModelSnapshot.kt | deepseek-flash | 88.0 |
| Moblin/Various/Model/ModelSoop.swift | app/src/main/java/com/moblin/android/various/model/ModelSoop.kt | deepseek-flash | 16.0 |
| Moblin/Various/Model/ModelSpeechToText.swift | app/src/main/java/com/moblin/android/various/model/ModelSpeechToText.kt | deepseek-flash | 41.7 |
| Moblin/Various/Model/ModelSrtClient.swift | app/src/main/java/com/moblin/android/various/model/ModelSrtClient.kt | deepseek-flash | 7.0 |
| Moblin/Various/Model/ModelSrtlaServer.swift | app/src/main/java/com/moblin/android/various/model/ModelSrtlaServer.kt | deepseek-flash | 7.1 |
| Moblin/Various/Model/ModelStealthMode.swift | app/src/main/java/com/moblin/android/various/model/ModelStealthMode.kt | deepseek-flash | 18.4 |
| Moblin/Various/Model/ModelStore.swift | app/src/main/java/com/moblin/android/various/model/ModelStore.kt | deepseek-flash | 40.6 |
| Moblin/Various/Model/ModelStream.swift | app/src/main/java/com/moblin/android/various/model/ModelStream.kt | deepseek-flash | 31.1 |
| Moblin/Various/Model/ModelStreamDeck.swift | app/src/main/java/com/moblin/android/various/model/ModelStreamDeck.kt | deepseek-flash | 43.2 |
| Moblin/Various/Model/ModelStreamWizard.swift | app/src/main/java/com/moblin/android/various/model/ModelStreamWizard.kt | deepseek-flash | 94.9 |
| Moblin/Various/Model/ModelTesla.swift | app/src/main/java/com/moblin/android/various/model/ModelTesla.kt | deepseek-flash | 43.3 |
| Moblin/Various/Model/ModelTextToSpeech.swift | app/src/main/java/com/moblin/android/various/model/ModelTextToSpeech.kt | deepseek-flash | 6.9 |
| Moblin/Various/Model/ModelTwitch.swift | app/src/main/java/com/moblin/android/various/model/ModelTwitch.kt | deepseek-flash | 67.7 |
| Moblin/Various/Model/ModelVariables.swift | app/src/main/java/com/moblin/android/various/model/ModelVariables.kt | deepseek-flash | 24.9 |
| Moblin/Various/Model/ModelVideoPreview.swift | app/src/main/java/com/moblin/android/various/model/ModelVideoPreview.kt | deepseek-flash | 8.9 |
| Moblin/Various/Model/ModelWebBrowser.swift | app/src/main/java/com/moblin/android/various/model/ModelWebBrowser.kt | deepseek-flash | 38.6 |
| Moblin/Various/Model/ModelWhepClient.swift | app/src/main/java/com/moblin/android/various/model/ModelWhepClient.kt | deepseek-flash | 9.4 |
| Moblin/Various/Model/ModelWhipServer.swift | app/src/main/java/com/moblin/android/various/model/ModelWhipServer.kt | deepseek-flash | 6.5 |
| Moblin/Various/Model/ModelWiFiAware.swift | app/src/main/java/com/moblin/android/various/model/ModelWiFiAware.kt | deepseek-flash | 97.5 |
| Moblin/Various/Model/ModelWorkout.swift | app/src/main/java/com/moblin/android/various/model/ModelWorkout.kt | deepseek-flash | 34.4 |
| Moblin/Various/Model/ModelWorkoutDevice.swift | app/src/main/java/com/moblin/android/various/model/ModelWorkoutDevice.kt | deepseek-flash | 43.6 |
| Moblin/Various/Model/ModelYouTube.swift | app/src/main/java/com/moblin/android/various/model/ModelYouTube.kt | deepseek-flash | 58.6 |
| Moblin/Various/Model/ModelZoom.swift | app/src/main/java/com/moblin/android/various/model/ModelZoom.kt | deepseek-flash | 65.5 |
| Moblin/Various/Network/DnsLookup.swift | app/src/main/java/com/moblin/android/various/network/DnsLookup.kt | deepseek-flash | 6.9 |
| Moblin/Various/Network/HttpClient.swift | app/src/main/java/com/moblin/android/various/network/HttpClient.kt | deepseek-flash | 13.0 |
| Moblin/Various/Network/HttpProxyServer.swift | app/src/main/java/com/moblin/android/various/network/HttpProxyServer.kt | deepseek-flash | 80.7 |
| Moblin/Various/Network/HttpServer.swift | app/src/main/java/com/moblin/android/various/network/HttpServer.kt | deepseek-flash | 85.3 |
| Moblin/Various/Network/HttpUrlSession.swift | app/src/main/java/com/moblin/android/various/network/HttpUrlSession.kt | deepseek-flash | 4.2 |
| Moblin/Various/Network/IpMonitor.swift | app/src/main/java/com/moblin/android/various/network/IpMonitor.kt | deepseek-flash | 52.0 |
| Moblin/Various/Network/NetworkInterfaceTypeSelector.swift | app/src/main/java/com/moblin/android/various/network/NetworkInterfaceTypeSelector.kt | deepseek-flash | 30.4 |
| Moblin/Various/Network/NetworkUtils.swift | app/src/main/java/com/moblin/android/various/network/NetworkUtils.kt | deepseek-flash | 65.6 |
| Moblin/Various/Network/Ports.swift | app/src/main/java/com/moblin/android/various/network/Ports.kt | deepseek-flash | 3.6 |
| Moblin/Various/Network/WebSocketClient.swift | app/src/main/java/com/moblin/android/various/network/WebSocketClient.kt | deepseek-flash | 93.2 |
| Moblin/Various/ReplayFrameExtractor.swift | app/src/main/java/com/moblin/android/various/ReplayFrameExtractor.kt | deepseek-flash | 8.9 |
| Moblin/Various/Settings/Settings.swift | app/src/main/java/com/moblin/android/various/settings/Settings.kt | deepseek-flash | 71.0 |
| Moblin/Various/Settings/SettingsAudio.swift | app/src/main/java/com/moblin/android/various/settings/SettingsAudio.kt | deepseek-flash | 9.3 |
| Moblin/Various/Settings/SettingsCatPrinter.swift | app/src/main/java/com/moblin/android/various/settings/SettingsCatPrinter.kt | deepseek-flash | 111.6 |
| Moblin/Various/Settings/SettingsChat.swift | app/src/main/java/com/moblin/android/various/settings/SettingsChat.kt | deepseek-flash | 23.6 |
| Moblin/Various/Settings/SettingsDebug.swift | app/src/main/java/com/moblin/android/various/settings/SettingsDebug.kt | deepseek-flash | 7.2 |
| Moblin/Various/Settings/SettingsDeepLinkCreator.swift | app/src/main/java/com/moblin/android/various/settings/SettingsDeepLinkCreator.kt | deepseek-flash | 67.4 |
| Moblin/Various/Settings/SettingsDjiDevice.swift | app/src/main/java/com/moblin/android/various/settings/SettingsDjiDevice.kt | deepseek-flash | 39.5 |
| Moblin/Various/Settings/SettingsGameController.swift | app/src/main/java/com/moblin/android/various/settings/SettingsGameController.kt | deepseek-flash | 59.8 |
| Moblin/Various/Settings/SettingsGimbal.swift | app/src/main/java/com/moblin/android/various/settings/SettingsGimbal.kt | deepseek-flash | 125.2 |
| Moblin/Various/Settings/SettingsGoPro.swift | app/src/main/java/com/moblin/android/various/settings/SettingsGoPro.kt | deepseek-flash | 97.8 |
| Moblin/Various/Settings/SettingsHttpProxy.swift | app/src/main/java/com/moblin/android/various/settings/SettingsHttpProxy.kt | deepseek-flash | 61.3 |
| Moblin/Various/Settings/SettingsIngests.swift | app/src/main/java/com/moblin/android/various/settings/SettingsIngests.kt | deepseek-flash | 13.8 |
| Moblin/Various/Settings/SettingsKeyboard.swift | app/src/main/java/com/moblin/android/various/settings/SettingsKeyboard.kt | deepseek-flash | 106.6 |
| Moblin/Various/Settings/SettingsLocation.swift | app/src/main/java/com/moblin/android/various/settings/SettingsLocation.kt | deepseek-flash | 140.2 |
| Moblin/Various/Settings/SettingsMacros.swift | app/src/main/java/com/moblin/android/various/settings/SettingsMacros.kt | deepseek-flash | 18.0 |
| Moblin/Various/Settings/SettingsMoblink.swift | app/src/main/java/com/moblin/android/various/settings/SettingsMoblink.kt | deepseek-flash | 57.5 |
| Moblin/Various/Settings/SettingsNavigation.swift | app/src/main/java/com/moblin/android/various/settings/SettingsNavigation.kt | deepseek-flash | 22.4 |
| Moblin/Various/Settings/SettingsQuickButtons.swift | app/src/main/java/com/moblin/android/various/settings/SettingsQuickButtons.kt | deepseek-flash | 94.2 |
| Moblin/Various/Settings/SettingsRemoteControl.swift | app/src/main/java/com/moblin/android/various/settings/SettingsRemoteControl.kt | deepseek-flash | 154.4 |
| Moblin/Various/Settings/SettingsScene.swift | app/src/main/java/com/moblin/android/various/settings/SettingsScene.kt | deepseek-flash | 106.3 |
| Moblin/Various/Settings/SettingsSelfieStick.swift | app/src/main/java/com/moblin/android/various/settings/SettingsSelfieStick.kt | deepseek-flash | 70.7 |
| Moblin/Various/Settings/SettingsStream.swift | app/src/main/java/com/moblin/android/various/settings/SettingsStream.kt | deepseek-flash | 63.2 |
| Moblin/Various/Settings/SettingsStreamDeck.swift | app/src/main/java/com/moblin/android/various/settings/SettingsStreamDeck.kt | deepseek-flash | 101.4 |
| Moblin/Various/Settings/SettingsTalkback.swift | app/src/main/java/com/moblin/android/various/settings/SettingsTalkback.kt | deepseek-flash | 29.2 |
| Moblin/Various/SimpleTimer.swift | app/src/main/java/com/moblin/android/various/SimpleTimer.kt | deepseek-flash | 4.5 |
| Moblin/Various/SpeechToText.swift | app/src/main/java/com/moblin/android/various/SpeechToText.kt | deepseek-flash | 54.7 |
| Moblin/Various/Storages/AlertMediaStorage.swift | app/src/main/java/com/moblin/android/various/storages/AlertMediaStorage.kt | deepseek-flash | 3.8 |
| Moblin/Various/Storages/FileStorage.swift | app/src/main/java/com/moblin/android/various/storages/FileStorage.kt | deepseek-flash | 58.0 |
| Moblin/Various/Storages/ImageStorage.swift | app/src/main/java/com/moblin/android/various/storages/ImageStorage.kt | deepseek-flash | 15.4 |
| Moblin/Various/Storages/LogsStorage.swift | app/src/main/java/com/moblin/android/various/storages/LogsStorage.kt | deepseek-flash | 20.3 |
| Moblin/Various/Storages/MediaPlayerStorage.swift | app/src/main/java/com/moblin/android/various/storages/MediaPlayerStorage.kt | deepseek-flash | 28.4 |
| Moblin/Various/Storages/PngTuberStorage.swift | app/src/main/java/com/moblin/android/various/storages/PngTuberStorage.kt | deepseek-flash | 5.1 |
| Moblin/Various/Storages/RecordingsStorage.swift | app/src/main/java/com/moblin/android/various/storages/RecordingsStorage.kt | deepseek-flash | 19.1 |
| Moblin/Various/Storages/ReplayTransitionsStorage.swift | app/src/main/java/com/moblin/android/various/storages/ReplayTransitionsStorage.kt | deepseek-flash | 15.8 |
| Moblin/Various/Storages/ReplaysStorage.swift | app/src/main/java/com/moblin/android/various/storages/ReplaysStorage.kt | deepseek-flash | 21.6 |
| Moblin/Various/Storages/SimpleStorage.swift | app/src/main/java/com/moblin/android/various/storages/SimpleStorage.kt | deepseek-flash | 4.6 |
| Moblin/Various/Storages/StreamingHistory.swift | app/src/main/java/com/moblin/android/various/storages/StreamingHistory.kt | deepseek-flash | 124.6 |
| Moblin/Various/Storages/VTuberStorage.swift | app/src/main/java/com/moblin/android/various/storages/VTuberStorage.kt | deepseek-flash | 2.8 |
| Moblin/Various/Subtitles/Subtitles.swift | app/src/main/java/com/moblin/android/various/subtitles/Subtitles.kt | deepseek-flash | 10.6 |
| Moblin/Various/Subtitles/TextAligner.swift | app/src/main/java/com/moblin/android/various/subtitles/TextAligner.kt | deepseek-flash | 7.7 |
| Moblin/Various/Subtitles/Translator.swift | app/src/main/java/com/moblin/android/various/subtitles/Translator.kt | deepseek-flash | 46.0 |
| Moblin/Various/Utils/CameraUtils.swift | app/src/main/java/com/moblin/android/various/utils/CameraUtils.kt | deepseek-flash | 12.2 |
| Moblin/Various/Utils/FileSystemUtils.swift | app/src/main/java/com/moblin/android/various/utils/FileSystemUtils.kt | deepseek-flash | 7.5 |
| Moblin/Various/Utils/LocationUtils.swift | app/src/main/java/com/moblin/android/various/utils/LocationUtils.kt | deepseek-flash | 19.9 |
| Moblin/Various/Utils/UiUtils.swift | app/src/main/java/com/moblin/android/various/utils/UiUtils.kt | deepseek-flash | 25.0 |
| Moblin/Various/Utils/Utils.swift | app/src/main/java/com/moblin/android/various/utils/Utils.kt | deepseek-flash | 13.5 |
| Moblin/Various/Utils/WiFiUtils.swift | app/src/main/java/com/moblin/android/various/utils/WiFiUtils.kt | deepseek-flash | 5.1 |
| Moblin/Various/Variables.swift | app/src/main/java/com/moblin/android/various/Variables.kt | deepseek-flash | 20.9 |
| Moblin/Various/WebBrowserController.swift | app/src/main/java/com/moblin/android/various/WebBrowserController.kt | deepseek-flash | 41.9 |
| Moblin/VideoEffects/Alerts/AlertsEffect.swift | app/src/main/java/com/moblin/android/videoeffects/alerts/AlertsEffect.kt | deepseek-flash | 202.4 |
| Moblin/VideoEffects/Alerts/AlertsEffectFace.swift | app/src/main/java/com/moblin/android/videoeffects/alerts/AlertsEffectFace.kt | deepseek-flash | 11.3 |
| Moblin/VideoEffects/Alerts/AlertsEffectMedia.swift | app/src/main/java/com/moblin/android/videoeffects/alerts/AlertsEffectMedia.kt | deepseek-flash | 76.0 |
| Moblin/VideoEffects/Alerts/AlertsEffectVideoReader.swift | app/src/main/java/com/moblin/android/videoeffects/alerts/AlertsEffectVideoReader.kt | deepseek-flash | 18.9 |
| Moblin/VideoEffects/AnamorphicLensEffect.swift | app/src/main/java/com/moblin/android/videoeffects/AnamorphicLensEffect.kt | deepseek-flash | 7.7 |
| Moblin/VideoEffects/BeautyEffect.swift | app/src/main/java/com/moblin/android/videoeffects/BeautyEffect.kt | deepseek-flash | 38.3 |
| Moblin/VideoEffects/BingoCardEffect.swift | app/src/main/java/com/moblin/android/videoeffects/BingoCardEffect.kt | deepseek-flash | 103.0 |
| Moblin/VideoEffects/Browser/BrowserEffect.swift | app/src/main/java/com/moblin/android/videoeffects/browser/BrowserEffect.kt | deepseek-flash | 10.9 |
| Moblin/VideoEffects/Browser/BrowserEffectServer.swift | app/src/main/java/com/moblin/android/videoeffects/browser/BrowserEffectServer.kt | deepseek-flash | 98.6 |
| Moblin/VideoEffects/CameraManEffect.swift | app/src/main/java/com/moblin/android/videoeffects/CameraManEffect.kt | deepseek-flash | 21.3 |
| Moblin/VideoEffects/ChatEffect.swift | app/src/main/java/com/moblin/android/videoeffects/ChatEffect.kt | deepseek-flash | 14.6 |
| Moblin/VideoEffects/ChatEmoteComboEffect.swift | app/src/main/java/com/moblin/android/videoeffects/ChatEmoteComboEffect.kt | deepseek-flash | 95.9 |
| Moblin/VideoEffects/Crt/CrtBarrelDistortionFilter.swift | app/src/main/java/com/moblin/android/videoeffects/crt/CrtBarrelDistortionFilter.kt | deepseek-flash | 13.4 |
| Moblin/VideoEffects/Crt/CrtEffect.swift | app/src/main/java/com/moblin/android/videoeffects/crt/CrtEffect.kt | deepseek-flash | 16.1 |
| Moblin/VideoEffects/Dewarp360/Dewarp360Effect.swift | app/src/main/java/com/moblin/android/videoeffects/dewarp360/Dewarp360Effect.kt | deepseek-flash | 19.1 |
| Moblin/VideoEffects/Dewarp360/Dewarp360Filter.swift | app/src/main/java/com/moblin/android/videoeffects/dewarp360/Dewarp360Filter.kt | deepseek-flash | 39.2 |
| Moblin/VideoEffects/DrawOnStreamEffect.swift | app/src/main/java/com/moblin/android/videoeffects/DrawOnStreamEffect.kt | deepseek-flash | 51.6 |
| Moblin/VideoEffects/EffectUtils.swift | app/src/main/java/com/moblin/android/videoeffects/EffectUtils.kt | deepseek-flash | 39.1 |
| Moblin/VideoEffects/FaceEffect.swift | app/src/main/java/com/moblin/android/videoeffects/FaceEffect.kt | deepseek-flash | 113.2 |
| Moblin/VideoEffects/FixedHorizonEffect.swift | app/src/main/java/com/moblin/android/videoeffects/FixedHorizonEffect.kt | deepseek-flash | 37.6 |
| Moblin/VideoEffects/FourThreeEffect.swift | app/src/main/java/com/moblin/android/videoeffects/FourThreeEffect.kt | deepseek-flash | 9.6 |
| Moblin/VideoEffects/GrayScaleEffect.swift | app/src/main/java/com/moblin/android/videoeffects/GrayScaleEffect.kt | deepseek-flash | 9.5 |
| Moblin/VideoEffects/ImageEffect.swift | app/src/main/java/com/moblin/android/videoeffects/ImageEffect.kt | deepseek-flash | 26.0 |
| Moblin/VideoEffects/LutEffect.swift | app/src/main/java/com/moblin/android/videoeffects/LutEffect.kt | deepseek-flash | 37.0 |
| Moblin/VideoEffects/MapEffect.swift | app/src/main/java/com/moblin/android/videoeffects/MapEffect.kt | deepseek-flash | 11.7 |
| Moblin/VideoEffects/MaskEffect.swift | app/src/main/java/com/moblin/android/videoeffects/MaskEffect.kt | deepseek-flash | 77.6 |
| Moblin/VideoEffects/MovieEffect.swift | app/src/main/java/com/moblin/android/videoeffects/MovieEffect.kt | deepseek-flash | 7.7 |
| Moblin/VideoEffects/OpacityEffect.swift | app/src/main/java/com/moblin/android/videoeffects/OpacityEffect.kt | deepseek-flash | 9.5 |
| Moblin/VideoEffects/PinchEffect.swift | app/src/main/java/com/moblin/android/videoeffects/PinchEffect.kt | deepseek-flash | 9.0 |
| Moblin/VideoEffects/PixellateEffect.swift | app/src/main/java/com/moblin/android/videoeffects/PixellateEffect.kt | deepseek-flash | 9.7 |
| Moblin/VideoEffects/PngTuberEffect.swift | app/src/main/java/com/moblin/android/videoeffects/PngTuberEffect.kt | deepseek-flash | 9.7 |
| Moblin/VideoEffects/PollEffect.swift | app/src/main/java/com/moblin/android/videoeffects/PollEffect.kt | deepseek-flash | 42.6 |
| Moblin/VideoEffects/PomodoroTimerEffect.swift | app/src/main/java/com/moblin/android/videoeffects/PomodoroTimerEffect.kt | deepseek-flash | 83.0 |
| Moblin/VideoEffects/QrCodeEffect.swift | app/src/main/java/com/moblin/android/videoeffects/QrCodeEffect.kt | deepseek-flash | 30.3 |
| Moblin/VideoEffects/RemoveBackgroundEffect.swift | app/src/main/java/com/moblin/android/videoeffects/RemoveBackgroundEffect.kt | deepseek-flash | 53.0 |
| Moblin/VideoEffects/Replay/ReplayEffect.swift | app/src/main/java/com/moblin/android/videoeffects/replay/ReplayEffect.kt | deepseek-flash | 17.9 |
| Moblin/VideoEffects/Replay/ReplayEffectReplayReader.swift | app/src/main/java/com/moblin/android/videoeffects/replay/ReplayEffectReplayReader.kt | deepseek-flash | 29.9 |
| Moblin/VideoEffects/Replay/ReplayEffectStingerReader.swift | app/src/main/java/com/moblin/android/videoeffects/replay/ReplayEffectStingerReader.kt | deepseek-flash | 110.6 |
| Moblin/VideoEffects/Scoreboard/ScoreboardEffect.swift | app/src/main/java/com/moblin/android/videoeffects/scoreboard/ScoreboardEffect.kt | deepseek-flash | 48.1 |
| Moblin/VideoEffects/Scoreboard/ScoreboardEffectGenericView.swift | app/src/main/java/com/moblin/android/videoeffects/scoreboard/ScoreboardEffectGenericView.kt | deepseek-flash | 27.9 |
| Moblin/VideoEffects/Scoreboard/ScoreboardEffectGolfFullScorecardView.swift | app/src/main/java/com/moblin/android/videoeffects/scoreboard/ScoreboardEffectGolfFullScorecardView.kt | deepseek-flash | 56.1 |
| Moblin/VideoEffects/Scoreboard/ScoreboardEffectGolfView.swift | app/src/main/java/com/moblin/android/videoeffects/scoreboard/ScoreboardEffectGolfView.kt | deepseek-flash | 79.2 |
| Moblin/VideoEffects/Scoreboard/ScoreboardEffectModularView.swift | app/src/main/java/com/moblin/android/videoeffects/scoreboard/ScoreboardEffectModularView.kt | deepseek-flash | 177.1 |
| Moblin/VideoEffects/Scoreboard/ScoreboardEffectPadelView.swift | app/src/main/java/com/moblin/android/videoeffects/scoreboard/ScoreboardEffectPadelView.kt | deepseek-flash | 51.0 |
| Moblin/VideoEffects/SepiaEffect.swift | app/src/main/java/com/moblin/android/videoeffects/SepiaEffect.kt | deepseek-flash | 11.3 |
| Moblin/VideoEffects/ShapeEffect.swift | app/src/main/java/com/moblin/android/videoeffects/ShapeEffect.kt | deepseek-flash | 32.8 |
| Moblin/VideoEffects/SlideshowEffect.swift | app/src/main/java/com/moblin/android/videoeffects/SlideshowEffect.kt | deepseek-flash | 11.2 |
| Moblin/VideoEffects/SnapshotEffect.swift | app/src/main/java/com/moblin/android/videoeffects/SnapshotEffect.kt | deepseek-flash | 18.1 |
| Moblin/VideoEffects/Text/TextEffect.swift | app/src/main/java/com/moblin/android/videoeffects/text/TextEffect.kt | deepseek-flash | 26.9 |
| Moblin/VideoEffects/Text/TextEffectFormatter.swift | app/src/main/java/com/moblin/android/videoeffects/text/TextEffectFormatter.kt | deepseek-flash | 212.4 |
| Moblin/VideoEffects/Text/TextFormatStringLoader.swift | app/src/main/java/com/moblin/android/videoeffects/text/TextFormatStringLoader.kt | deepseek-flash | 73.3 |
| Moblin/VideoEffects/TripleEffect.swift | app/src/main/java/com/moblin/android/videoeffects/TripleEffect.kt | deepseek-flash | 14.0 |
| Moblin/VideoEffects/TwinEffect.swift | app/src/main/java/com/moblin/android/videoeffects/TwinEffect.kt | deepseek-flash | 12.7 |
| Moblin/VideoEffects/VTuber/VTuberEffect.swift | app/src/main/java/com/moblin/android/videoeffects/vtuber/VTuberEffect.kt | deepseek-flash | 49.3 |
| Moblin/VideoEffects/VTuber/VTuberLive2DEffect.swift | app/src/main/java/com/moblin/android/videoeffects/vtuber/VTuberLive2DEffect.kt | deepseek-flash | 50.0 |
| Moblin/VideoEffects/VTuber/VTuberVrmEffect.swift | app/src/main/java/com/moblin/android/videoeffects/vtuber/VTuberVrmEffect.kt | deepseek-flash | 38.7 |
| Moblin/VideoEffects/VideoSourceEffect.swift | app/src/main/java/com/moblin/android/videoeffects/VideoSourceEffect.kt | deepseek-flash | 16.4 |
| Moblin/VideoEffects/WheelOfLuckEffect.swift | app/src/main/java/com/moblin/android/videoeffects/WheelOfLuckEffect.kt | deepseek-flash | 8.4 |
| Moblin/VideoEffects/WhirlpoolEffect.swift | app/src/main/java/com/moblin/android/videoeffects/WhirlpoolEffect.kt | deepseek-flash | 12.5 |
| Moblin/View/ControlBar/BatteryView.swift | app/src/main/java/com/moblin/android/view/controlbar/BatteryView.kt | deepseek-flash | 20.0 |
| Moblin/View/ControlBar/ControlBarBackgroundView.swift | app/src/main/java/com/moblin/android/view/controlbar/ControlBarBackgroundView.kt | deepseek-flash | 10.2 |
| Moblin/View/ControlBar/ControlBarLandscapeView.swift | app/src/main/java/com/moblin/android/view/controlbar/ControlBarLandscapeView.kt | deepseek-flash | 97.8 |
| Moblin/View/ControlBar/ControlBarPortraitView.swift | app/src/main/java/com/moblin/android/view/controlbar/ControlBarPortraitView.kt | deepseek-flash | 12.9 |
| Moblin/View/ControlBar/ControlBarUtils.swift | app/src/main/java/com/moblin/android/view/controlbar/ControlBarUtils.kt | deepseek-flash | 4.7 |
| Moblin/View/ControlBar/QuickButton/Chat/Moderation/QuickButtonChatModerationKickView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/chat/moderation/QuickButtonChatModerationKickView.kt | deepseek-flash | 13.1 |
| Moblin/View/ControlBar/QuickButton/Chat/Moderation/QuickButtonChatModerationTwitchView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/chat/moderation/QuickButtonChatModerationTwitchView.kt | deepseek-flash | 33.9 |
| Moblin/View/ControlBar/QuickButton/Chat/QuickButtonChatChatterInfoView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/chat/QuickButtonChatChatterInfoView.kt | deepseek-flash | 60.9 |
| Moblin/View/ControlBar/QuickButton/Chat/QuickButtonChatLinkConfirmation.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/chat/QuickButtonChatLinkConfirmation.kt | deepseek-flash | 9.8 |
| Moblin/View/ControlBar/QuickButton/Chat/QuickButtonChatModerationView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/chat/QuickButtonChatModerationView.kt | deepseek-flash | 133.3 |
| Moblin/View/ControlBar/QuickButton/Chat/QuickButtonChatView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/chat/QuickButtonChatView.kt | deepseek-flash | 40.8 |
| Moblin/View/ControlBar/QuickButton/QuickButtonAutoSceneSwitcherView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonAutoSceneSwitcherView.kt | deepseek-flash | 8.7 |
| Moblin/View/ControlBar/QuickButton/QuickButtonBitrateView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonBitrateView.kt | deepseek-flash | 37.6 |
| Moblin/View/ControlBar/QuickButton/QuickButtonDjiDevicesView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonDjiDevicesView.kt | deepseek-flash | 3.2 |
| Moblin/View/ControlBar/QuickButton/QuickButtonGoProView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonGoProView.kt | deepseek-flash | 84.0 |
| Moblin/View/ControlBar/QuickButton/QuickButtonLiveView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonLiveView.kt | deepseek-flash | 48.3 |
| Moblin/View/ControlBar/QuickButton/QuickButtonLutsView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonLutsView.kt | deepseek-flash | 29.0 |
| Moblin/View/ControlBar/QuickButton/QuickButtonMacrosView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonMacrosView.kt | deepseek-flash | 36.0 |
| Moblin/View/ControlBar/QuickButton/QuickButtonMicView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonMicView.kt | deepseek-flash | 8.9 |
| Moblin/View/ControlBar/QuickButton/QuickButtonObsView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonObsView.kt | deepseek-flash | 16.4 |
| Moblin/View/ControlBar/QuickButton/QuickButtonSceneWidgetsView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonSceneWidgetsView.kt | deepseek-flash | 29.7 |
| Moblin/View/ControlBar/QuickButton/QuickButtonStreamSwitcherView.swift | app/src/main/java/com/moblin/android/view/controlbar/quickbutton/QuickButtonStreamSwitcherView.kt | deepseek-flash | 21.0 |
| Moblin/View/ControlBar/QuickButtonsView.swift | app/src/main/java/com/moblin/android/view/controlbar/QuickButtonsView.kt | deepseek-flash | 19.4 |
| Moblin/View/ControlBar/RemoteControlAssistant/ControlBarRemoteControlAssistantView.swift | app/src/main/java/com/moblin/android/view/controlbar/remotecontrolassistant/ControlBarRemoteControlAssistantView.kt | deepseek-flash | 170.7 |
| Moblin/View/ControlBar/StreamButton.swift | app/src/main/java/com/moblin/android/view/controlbar/StreamButton.kt | deepseek-flash | 48.9 |
| Moblin/View/ControlBar/ThermalStateSheetView.swift | app/src/main/java/com/moblin/android/view/controlbar/ThermalStateSheetView.kt | deepseek-flash | 18.3 |
| Moblin/View/ExternalDisplay/ExternalDisplayView.swift | app/src/main/java/com/moblin/android/view/externaldisplay/ExternalDisplayView.kt | deepseek-flash | 11.4 |
| Moblin/View/Main/KeyPressView.swift | app/src/main/java/com/moblin/android/view/main/KeyPressView.kt | deepseek-flash | 48.7 |
| Moblin/View/Main/LockScreenView.swift | app/src/main/java/com/moblin/android/view/main/LockScreenView.kt | deepseek-flash | 4.1 |
| Moblin/View/Main/SnapshotCountdownView.swift | app/src/main/java/com/moblin/android/view/main/SnapshotCountdownView.kt | deepseek-flash | 3.8 |
| Moblin/View/Main/StealthModeView.swift | app/src/main/java/com/moblin/android/view/main/StealthModeView.kt | deepseek-flash | 79.5 |
| Moblin/View/MainView.swift | app/src/main/java/com/moblin/android/view/MainView.kt | deepseek-flash | 79.3 |
| Moblin/View/Settings/About/AboutAttributionsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/about/AboutAttributionsSettingsView.kt | deepseek-flash | 20.8 |
| Moblin/View/Settings/About/AboutSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/about/AboutSettingsView.kt | deepseek-flash | 15.0 |
| Moblin/View/Settings/About/AboutVersionHistorySettingsView.swift | app/src/main/java/com/moblin/android/view/settings/about/AboutVersionHistorySettingsView.kt | deepseek-flash | 53.2 |
| Moblin/View/Settings/AppleMusic/AppleMusicSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/applemusic/AppleMusicSettingsView.kt | deepseek-flash | 60.6 |
| Moblin/View/Settings/Audio/AudioSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/audio/AudioSettingsView.kt | deepseek-flash | 59.7 |
| Moblin/View/Settings/Audio/MicsDelaySettingsView.swift | app/src/main/java/com/moblin/android/view/settings/audio/MicsDelaySettingsView.kt | deepseek-flash | 19.3 |
| Moblin/View/Settings/BitratePresets/BitratePresetsPresetSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/bitratepresets/BitratePresetsPresetSettingsView.kt | deepseek-flash | 19.8 |
| Moblin/View/Settings/BitratePresets/BitratePresetsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/bitratepresets/BitratePresetsSettingsView.kt | deepseek-flash | 55.5 |
| Moblin/View/Settings/BlackSharkCoolers/BlackSharkCoolerDeviceScannerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/blacksharkcoolers/BlackSharkCoolerDeviceScannerSettingsView.kt | deepseek-flash | 24.8 |
| Moblin/View/Settings/BlackSharkCoolers/BlackSharkCoolerDeviceSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/blacksharkcoolers/BlackSharkCoolerDeviceSettingsView.kt | deepseek-flash | 11.3 |
| Moblin/View/Settings/BlackSharkCoolers/BlackSharkCoolerDevicesSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/blacksharkcoolers/BlackSharkCoolerDevicesSettingsView.kt | deepseek-flash | 17.0 |
| Moblin/View/Settings/Camera/CameraControls/CameraControlsView.swift | app/src/main/java/com/moblin/android/view/settings/camera/cameracontrols/CameraControlsView.kt | deepseek-flash | 3.7 |
| Moblin/View/Settings/Camera/CameraSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/camera/CameraSettingsView.kt | deepseek-flash | 14.6 |
| Moblin/View/Settings/Camera/FixedHorizon/FixedHorizonView.swift | app/src/main/java/com/moblin/android/view/settings/camera/fixedhorizon/FixedHorizonView.kt | deepseek-flash | 3.3 |
| Moblin/View/Settings/Camera/MirrorFrontCamera/MirrorFrontCameraView.swift | app/src/main/java/com/moblin/android/view/settings/camera/mirrorfrontcamera/MirrorFrontCameraView.kt | deepseek-flash | 2.5 |
| Moblin/View/Settings/Camera/TapScreenToFocus/TapScreenToFocusSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/camera/tapscreentofocus/TapScreenToFocusSettingsView.kt | deepseek-flash | 9.9 |
| Moblin/View/Settings/Camera/VideoStabilization/VideoStabilizationSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/camera/videostabilization/VideoStabilizationSettingsView.kt | deepseek-flash | 8.7 |
| Moblin/View/Settings/Camera/Zoom/ZoomPresetSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/camera/zoom/ZoomPresetSettingsView.kt | deepseek-flash | 4.1 |
| Moblin/View/Settings/Camera/Zoom/ZoomSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/camera/zoom/ZoomSettingsView.kt | deepseek-flash | 6.4 |
| Moblin/View/Settings/Camera/Zoom/ZoomSwitchToSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/camera/zoom/ZoomSwitchToSettingsView.kt | deepseek-flash | 34.7 |
| Moblin/View/Settings/CatPrinters/CatPrinterScannerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/catprinters/CatPrinterScannerSettingsView.kt | deepseek-flash | 53.7 |
| Moblin/View/Settings/CatPrinters/CatPrinterSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/catprinters/CatPrinterSettingsView.kt | deepseek-flash | 53.3 |
| Moblin/View/Settings/CatPrinters/CatPrintersSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/catprinters/CatPrintersSettingsView.kt | deepseek-flash | 10.0 |
| Moblin/View/Settings/Chat/ChatBotSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/chat/ChatBotSettingsView.kt | deepseek-flash | 21.7 |
| Moblin/View/Settings/Chat/ChatFiltersSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/chat/ChatFiltersSettingsView.kt | deepseek-flash | 7.0 |
| Moblin/View/Settings/Chat/ChatNicknamesSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/chat/ChatNicknamesSettingsView.kt | deepseek-flash | 112.3 |
| Moblin/View/Settings/Chat/ChatSettingsAppearanceView.swift | app/src/main/java/com/moblin/android/view/settings/chat/ChatSettingsAppearanceView.kt | deepseek-flash | 15.1 |
| Moblin/View/Settings/Chat/ChatSettingsLayoutView.swift | app/src/main/java/com/moblin/android/view/settings/chat/ChatSettingsLayoutView.kt | deepseek-flash | 26.8 |
| Moblin/View/Settings/Chat/ChatSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/chat/ChatSettingsView.kt | deepseek-flash | 5.0 |
| Moblin/View/Settings/Chat/ChatTextToSpeechSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/chat/ChatTextToSpeechSettingsView.kt | deepseek-flash | 9.5 |
| Moblin/View/Settings/Debug/DebugLogSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/debug/DebugLogSettingsView.kt | deepseek-flash | 34.2 |
| Moblin/View/Settings/Debug/DebugSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/debug/DebugSettingsView.kt | deepseek-flash | 24.1 |
| Moblin/View/Settings/Debug/DebugVideoSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/debug/DebugVideoSettingsView.kt | deepseek-flash | 86.7 |
| Moblin/View/Settings/DeepLinkCreator/DeepLinkCreatorQuickButtonsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/deeplinkcreator/DeepLinkCreatorQuickButtonsSettingsView.kt | deepseek-flash | 36.5 |
| Moblin/View/Settings/DeepLinkCreator/DeepLinkCreatorSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/deeplinkcreator/DeepLinkCreatorSettingsView.kt | deepseek-flash | 69.5 |
| Moblin/View/Settings/DeepLinkCreator/DeepLinkCreatorStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/deeplinkcreator/DeepLinkCreatorStreamSettingsView.kt | deepseek-flash | 100.8 |
| Moblin/View/Settings/DeepLinkCreator/DeepLinkCreatorStreamsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/deeplinkcreator/DeepLinkCreatorStreamsSettingsView.kt | deepseek-flash | 21.6 |
| Moblin/View/Settings/DeepLinkCreator/DeepLinkCreatorWebBrowserSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/deeplinkcreator/DeepLinkCreatorWebBrowserSettingsView.kt | deepseek-flash | 9.0 |
| Moblin/View/Settings/Display/DisplaySettingsView.swift | app/src/main/java/com/moblin/android/view/settings/display/DisplaySettingsView.kt | deepseek-flash | 19.7 |
| Moblin/View/Settings/Display/LocalOverlays/LocalOverlaysSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/display/localoverlays/LocalOverlaysSettingsView.kt | deepseek-flash | 36.3 |
| Moblin/View/Settings/Display/NetworkInterfaceNames/LocalOverlaysNetworkInterfaceNamesSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/display/networkinterfacenames/LocalOverlaysNetworkInterfaceNamesSettingsView.kt | deepseek-flash | 5.7 |
| Moblin/View/Settings/Display/QuickButtons/QuickButtonsButtonSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/display/quickbuttons/QuickButtonsButtonSettingsView.kt | deepseek-flash | 27.8 |
| Moblin/View/Settings/Display/QuickButtons/QuickButtonsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/display/quickbuttons/QuickButtonsSettingsView.kt | deepseek-flash | 12.5 |
| Moblin/View/Settings/Display/StreamButton/StreamButtonsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/display/streambutton/StreamButtonsSettingsView.kt | deepseek-flash | 47.9 |
| Moblin/View/Settings/DjiDevices/DjiDeviceScannerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/djidevices/DjiDeviceScannerSettingsView.kt | deepseek-flash | 22.3 |
| Moblin/View/Settings/DjiDevices/DjiDeviceSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/djidevices/DjiDeviceSettingsView.kt | deepseek-flash | 19.1 |
| Moblin/View/Settings/DjiDevices/DjiDevicesSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/djidevices/DjiDevicesSettingsView.kt | deepseek-flash | 14.6 |
| Moblin/View/Settings/GameControllers/GameControllersControllerButtonSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/gamecontrollers/GameControllersControllerButtonSettingsView.kt | deepseek-flash | 76.2 |
| Moblin/View/Settings/GameControllers/GameControllersControllerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/gamecontrollers/GameControllersControllerSettingsView.kt | deepseek-flash | 10.7 |
| Moblin/View/Settings/GameControllers/GameControllersControllerThumbStickSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/gamecontrollers/GameControllersControllerThumbStickSettingsView.kt | deepseek-flash | 26.1 |
| Moblin/View/Settings/GameControllers/GameControllersSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/gamecontrollers/GameControllersSettingsView.kt | deepseek-flash | 26.6 |
| Moblin/View/Settings/Gimbal/GimbalSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/gimbal/GimbalSettingsView.kt | deepseek-flash | 149.3 |
| Moblin/View/Settings/GoPro/GoProBleDeviceSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/gopro/GoProBleDeviceSettingsView.kt | deepseek-flash | 21.7 |
| Moblin/View/Settings/GoPro/GoProSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/gopro/GoProSettingsView.kt | deepseek-flash | 92.0 |
| Moblin/View/Settings/HelpAndSupport/HelpAndSupportSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/helpandsupport/HelpAndSupportSettingsView.kt | deepseek-flash | 5.6 |
| Moblin/View/Settings/HttpProxy/HttpProxySettingsView.swift | app/src/main/java/com/moblin/android/view/settings/httpproxy/HttpProxySettingsView.kt | deepseek-flash | 5.2 |
| Moblin/View/Settings/ImportExport/ExportSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/importexport/ExportSettingsView.kt | deepseek-flash | 15.0 |
| Moblin/View/Settings/ImportExport/ImportExportSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/importexport/ImportExportSettingsView.kt | deepseek-flash | 5.5 |
| Moblin/View/Settings/ImportExport/ImportSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/importexport/ImportSettingsView.kt | deepseek-flash | 5.6 |
| Moblin/View/Settings/Ingests/IngestsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/IngestsSettingsView.kt | deepseek-flash | 4.4 |
| Moblin/View/Settings/Ingests/RistServer/RistServerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/ristserver/RistServerSettingsView.kt | deepseek-flash | 7.1 |
| Moblin/View/Settings/Ingests/RistServer/RistServerStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/ristserver/RistServerStreamSettingsView.kt | deepseek-flash | 30.7 |
| Moblin/View/Settings/Ingests/RtmpServer/RtmpServerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/rtmpserver/RtmpServerSettingsView.kt | deepseek-flash | 5.6 |
| Moblin/View/Settings/Ingests/RtmpServer/RtmpServerStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/rtmpserver/RtmpServerStreamSettingsView.kt | deepseek-flash | 9.4 |
| Moblin/View/Settings/Ingests/RtspClient/RtspClientSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/rtspclient/RtspClientSettingsView.kt | deepseek-flash | 6.8 |
| Moblin/View/Settings/Ingests/RtspClient/RtspClientStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/rtspclient/RtspClientStreamSettingsView.kt | deepseek-flash | 8.2 |
| Moblin/View/Settings/Ingests/SrtClient/SrtClientSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/srtclient/SrtClientSettingsView.kt | deepseek-flash | 4.4 |
| Moblin/View/Settings/Ingests/SrtClient/SrtClientStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/srtclient/SrtClientStreamSettingsView.kt | deepseek-flash | 49.3 |
| Moblin/View/Settings/Ingests/SrtlaServer/SrtlaServerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/srtlaserver/SrtlaServerSettingsView.kt | deepseek-flash | 7.4 |
| Moblin/View/Settings/Ingests/SrtlaServer/SrtlaServerStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/srtlaserver/SrtlaServerStreamSettingsView.kt | deepseek-flash | 7.7 |
| Moblin/View/Settings/Ingests/WhepClient/WhepClientSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/whepclient/WhepClientSettingsView.kt | deepseek-flash | 7.9 |
| Moblin/View/Settings/Ingests/WhepClient/WhepClientStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/whepclient/WhepClientStreamSettingsView.kt | deepseek-flash | 10.2 |
| Moblin/View/Settings/Ingests/WhipServer/WhipServerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/whipserver/WhipServerSettingsView.kt | deepseek-flash | 5.3 |
| Moblin/View/Settings/Ingests/WhipServer/WhipServerStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/ingests/whipserver/WhipServerStreamSettingsView.kt | deepseek-flash | 28.2 |
| Moblin/View/Settings/Keyboard/KeyboardKeySettingsView.swift | app/src/main/java/com/moblin/android/view/settings/keyboard/KeyboardKeySettingsView.kt | deepseek-flash | 5.9 |
| Moblin/View/Settings/Keyboard/KeyboardSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/keyboard/KeyboardSettingsView.kt | deepseek-flash | 43.5 |
| Moblin/View/Settings/Location/LocationSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/location/LocationSettingsView.kt | deepseek-flash | 7.3 |
| Moblin/View/Settings/Macros/MacrosSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/macros/MacrosSettingsView.kt | deepseek-flash | 20.5 |
| Moblin/View/Settings/MediaPlayer/MediaPlayerFileSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/mediaplayer/MediaPlayerFileSettingsView.kt | deepseek-flash | 21.9 |
| Moblin/View/Settings/MediaPlayer/MediaPlayerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/mediaplayer/MediaPlayerSettingsView.kt | deepseek-flash | 5.7 |
| Moblin/View/Settings/MediaPlayer/MediaPlayersSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/mediaplayer/MediaPlayersSettingsView.kt | deepseek-flash | 5.6 |
| Moblin/View/Settings/Moblink/MoblinkSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/moblink/MoblinkSettingsView.kt | deepseek-flash | 20.4 |
| Moblin/View/Settings/Recordings/RecordingsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/recordings/RecordingsSettingsView.kt | deepseek-flash | 46.7 |
| Moblin/View/Settings/RemoteControl/RemoteControlSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/remotecontrol/RemoteControlSettingsView.kt | deepseek-flash | 24.5 |
| Moblin/View/Settings/SaveReset/SettingsResetView.swift | app/src/main/java/com/moblin/android/view/settings/savereset/SettingsResetView.kt | deepseek-flash | 5.6 |
| Moblin/View/Settings/SaveReset/SettingsSaveView.swift | app/src/main/java/com/moblin/android/view/settings/savereset/SettingsSaveView.kt | deepseek-flash | 7.0 |
| Moblin/View/Settings/Scenes/AutoSwitchers/AutoSwitchersSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/autoswitchers/AutoSwitchersSettingsView.kt | deepseek-flash | 127.3 |
| Moblin/View/Settings/Scenes/DisconnectProtection/DisconnectProtectionSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/disconnectprotection/DisconnectProtectionSettingsView.kt | deepseek-flash | 31.4 |
| Moblin/View/Settings/Scenes/Scene/SceneSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/scene/SceneSettingsView.kt | deepseek-flash | 20.1 |
| Moblin/View/Settings/Scenes/Scene/SceneWidgetSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/scene/SceneWidgetSettingsView.kt | deepseek-flash | 14.1 |
| Moblin/View/Settings/Scenes/ScenesSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/ScenesSettingsView.kt | deepseek-flash | 101.4 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Alerts/WidgetAlertsChatBotSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/alerts/WidgetAlertsChatBotSettingsView.kt | deepseek-flash | 24.7 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Alerts/WidgetAlertsImageSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/alerts/WidgetAlertsImageSettingsView.kt | deepseek-flash | 8.2 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Alerts/WidgetAlertsKickSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/alerts/WidgetAlertsKickSettingsView.kt | deepseek-flash | 82.2 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Alerts/WidgetAlertsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/alerts/WidgetAlertsSettingsView.kt | deepseek-flash | 15.8 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Alerts/WidgetAlertsSoundSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/alerts/WidgetAlertsSoundSettingsView.kt | deepseek-flash | 12.4 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Alerts/WidgetAlertsSpeechToTextSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/alerts/WidgetAlertsSpeechToTextSettingsView.kt | deepseek-flash | 6.8 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Alerts/WidgetAlertsTextSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/alerts/WidgetAlertsTextSettingsView.kt | deepseek-flash | 31.6 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Alerts/WidgetAlertsTwitchSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/alerts/WidgetAlertsTwitchSettingsView.kt | deepseek-flash | 12.4 |
| Moblin/View/Settings/Scenes/Widgets/Widget/BingoCard/WidgetBingoCardSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/bingocard/WidgetBingoCardSettingsView.kt | deepseek-flash | 51.0 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Browser/WidgetBrowserSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/browser/WidgetBrowserSettingsView.kt | deepseek-flash | 51.5 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Chat/WidgetChatSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/chat/WidgetChatSettingsView.kt | deepseek-flash | 27.6 |
| Moblin/View/Settings/Scenes/Widgets/Widget/ChatEmoteCombo/WidgetChatEmoteComboSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/chatemotecombo/WidgetChatEmoteComboSettingsView.kt | deepseek-flash | 25.0 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Crop/WidgetCropSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/crop/WidgetCropSettingsView.kt | deepseek-flash | 41.8 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Effects/AnamorphicLensEffectView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/effects/AnamorphicLensEffectView.kt | deepseek-flash | 14.7 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Effects/Dewarp360EffectView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/effects/Dewarp360EffectView.kt | deepseek-flash | 22.9 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Effects/LutEffectView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/effects/LutEffectView.kt | deepseek-flash | 19.7 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Effects/MaskEffectView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/effects/MaskEffectView.kt | deepseek-flash | 130.4 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Effects/OpacityEffectView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/effects/OpacityEffectView.kt | deepseek-flash | 11.5 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Effects/RemoveBackgroundEffectView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/effects/RemoveBackgroundEffectView.kt | deepseek-flash | 23.1 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Effects/ShapeEffectView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/effects/ShapeEffectView.kt | deepseek-flash | 8.7 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Effects/WidgetEffectWizardSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/effects/WidgetEffectWizardSettingsView.kt | deepseek-flash | 15.1 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Effects/WidgetEffectsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/effects/WidgetEffectsView.kt | deepseek-flash | 78.7 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Image/WidgetImageSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/image/WidgetImageSettingsView.kt | deepseek-flash | 31.5 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Map/WidgetMapSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/map/WidgetMapSettingsView.kt | deepseek-flash | 19.2 |
| Moblin/View/Settings/Scenes/Widgets/Widget/PngTuber/WidgetPngTuberSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/pngtuber/WidgetPngTuberSettingsView.kt | deepseek-flash | 7.6 |
| Moblin/View/Settings/Scenes/Widgets/Widget/PomodoroTimer/WidgetPomodoroTimerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/pomodorotimer/WidgetPomodoroTimerSettingsView.kt | deepseek-flash | 102.6 |
| Moblin/View/Settings/Scenes/Widgets/Widget/QrCode/WidgetQrCodeSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/qrcode/WidgetQrCodeSettingsView.kt | deepseek-flash | 6.0 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Scene/WidgetSceneSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/scene/WidgetSceneSettingsView.kt | deepseek-flash | 11.6 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Scoreboard/WidgetScoreboardGenericSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/scoreboard/WidgetScoreboardGenericSettingsView.kt | deepseek-flash | 77.1 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Scoreboard/WidgetScoreboardGolfFullScorecardSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/scoreboard/WidgetScoreboardGolfFullScorecardSettingsView.kt | deepseek-flash | 13.3 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Scoreboard/WidgetScoreboardGolfSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/scoreboard/WidgetScoreboardGolfSettingsView.kt | deepseek-flash | 54.0 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Scoreboard/WidgetScoreboardModularSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/scoreboard/WidgetScoreboardModularSettingsView.kt | deepseek-flash | 91.1 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Scoreboard/WidgetScoreboardPadelSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/scoreboard/WidgetScoreboardPadelSettingsView.kt | deepseek-flash | 8.0 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Scoreboard/WidgetScoreboardSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/scoreboard/WidgetScoreboardSettingsView.kt | deepseek-flash | 75.3 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Slideshow/WidgetSlideshowSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/slideshow/WidgetSlideshowSettingsView.kt | deepseek-flash | 86.6 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Snapshot/WidgetSnapshotSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/snapshot/WidgetSnapshotSettingsView.kt | deepseek-flash | 21.1 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Text/WidgetTextSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/text/WidgetTextSettingsView.kt | deepseek-flash | 42.1 |
| Moblin/View/Settings/Scenes/Widgets/Widget/VTuber/WidgetVTuberSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/vtuber/WidgetVTuberSettingsView.kt | deepseek-flash | 11.2 |
| Moblin/View/Settings/Scenes/Widgets/Widget/VideoSource/WidgetVideoSourceSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/videosource/WidgetVideoSourceSettingsView.kt | deepseek-flash | 9.6 |
| Moblin/View/Settings/Scenes/Widgets/Widget/WheelOfLuck/WidgetWheelOfLuckSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wheelofluck/WidgetWheelOfLuckSettingsView.kt | deepseek-flash | 106.0 |
| Moblin/View/Settings/Scenes/Widgets/Widget/WidgetSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/WidgetSettingsView.kt | deepseek-flash | 88.6 |
| Moblin/View/Settings/Scenes/Widgets/Widget/WidgetWizardSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/WidgetWizardSettingsView.kt | deepseek-flash | 10.0 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardBingoCardSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardBingoCardSettingsView.kt | deepseek-flash | 19.8 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardBrowserSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardBrowserSettingsView.kt | deepseek-flash | 25.1 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardImageSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardImageSettingsView.kt | deepseek-flash | 21.5 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardPngTuberSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardPngTuberSettingsView.kt | deepseek-flash | 10.9 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardScoreboardSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardScoreboardSettingsView.kt | deepseek-flash | 31.0 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardSlideshowSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardSlideshowSettingsView.kt | deepseek-flash | 57.2 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardTextSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardTextSettingsView.kt | deepseek-flash | 12.3 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardVTuberSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardVTuberSettingsView.kt | deepseek-flash | 13.9 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardVideoSourceSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardVideoSourceSettingsView.kt | deepseek-flash | 6.9 |
| Moblin/View/Settings/Scenes/Widgets/Widget/Wizard/WidgetWizardWheelOfLuckSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/widget/wizard/WidgetWizardWheelOfLuckSettingsView.kt | deepseek-flash | 27.5 |
| Moblin/View/Settings/Scenes/Widgets/WidgetsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/scenes/widgets/WidgetsSettingsView.kt | deepseek-flash | 73.7 |
| Moblin/View/Settings/SelfieStick/SelfieStickSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/selfiestick/SelfieStickSettingsView.kt | deepseek-flash | 25.7 |
| Moblin/View/Settings/SettingsView.swift | app/src/main/java/com/moblin/android/view/settings/SettingsView.kt | deepseek-flash | 14.1 |
| Moblin/View/Settings/Store/StoreSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/store/StoreSettingsView.kt | deepseek-flash | 8.1 |
| Moblin/View/Settings/StreamDeck/StreamDeckLayoutSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streamdeck/StreamDeckLayoutSettingsView.kt | deepseek-flash | 136.7 |
| Moblin/View/Settings/StreamingHistory/StreamingHistorySettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streaminghistory/StreamingHistorySettingsView.kt | deepseek-flash | 43.9 |
| Moblin/View/Settings/StreamingHistory/StreamingHistoryStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streaminghistory/StreamingHistoryStreamSettingsView.kt | deepseek-flash | 33.7 |
| Moblin/View/Settings/Streams/Stream/Audio/StreamAudioSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/audio/StreamAudioSettingsView.kt | deepseek-flash | 69.7 |
| Moblin/View/Settings/Streams/Stream/Chat/StreamEmotesSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/chat/StreamEmotesSettingsView.kt | deepseek-flash | 7.8 |
| Moblin/View/Settings/Streams/Stream/GoLiveNotification/GoLiveNotificationSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/golivenotification/GoLiveNotificationSettingsView.kt | deepseek-flash | 57.9 |
| Moblin/View/Settings/Streams/Stream/Kick/StreamKickSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/kick/StreamKickSettingsView.kt | deepseek-flash | 66.7 |
| Moblin/View/Settings/Streams/Stream/Mobcam/StreamMobcamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/mobcam/StreamMobcamSettingsView.kt | deepseek-flash | 6.6 |
| Moblin/View/Settings/Streams/Stream/MultiStreaming/StreamMultiStreamingSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/multistreaming/StreamMultiStreamingSettingsView.kt | deepseek-flash | 119.1 |
| Moblin/View/Settings/Streams/Stream/ObsRemoteControl/StreamObsRemoteControlSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/obsremotecontrol/StreamObsRemoteControlSettingsView.kt | deepseek-flash | 40.4 |
| Moblin/View/Settings/Streams/Stream/OpenStreamingPlatform/StreamOpenStreamingPlatformSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/openstreamingplatform/StreamOpenStreamingPlatformSettingsView.kt | deepseek-flash | 14.6 |
| Moblin/View/Settings/Streams/Stream/PreviewStream/StreamPreviewStreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/previewstream/StreamPreviewStreamSettingsView.kt | deepseek-flash | 27.4 |
| Moblin/View/Settings/Streams/Stream/RealtimeIrl/StreamRealtimeIrlSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/realtimeirl/StreamRealtimeIrlSettingsView.kt | deepseek-flash | 6.8 |
| Moblin/View/Settings/Streams/Stream/Recording/StreamRecordingAudioSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/recording/StreamRecordingAudioSettingsView.kt | deepseek-flash | 16.2 |
| Moblin/View/Settings/Streams/Stream/Recording/StreamRecordingSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/recording/StreamRecordingSettingsView.kt | deepseek-flash | 10.7 |
| Moblin/View/Settings/Streams/Stream/Replay/StreamReplaySettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/replay/StreamReplaySettingsView.kt | deepseek-flash | 90.8 |
| Moblin/View/Settings/Streams/Stream/Rist/StreamRistSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/rist/StreamRistSettingsView.kt | deepseek-flash | 12.3 |
| Moblin/View/Settings/Streams/Stream/Rtmp/StreamRtmpSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/rtmp/StreamRtmpSettingsView.kt | deepseek-flash | 17.6 |
| Moblin/View/Settings/Streams/Stream/Snapshot/StreamSnapshotSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/snapshot/StreamSnapshotSettingsView.kt | deepseek-flash | 7.8 |
| Moblin/View/Settings/Streams/Stream/Soop/StreamSoopSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/soop/StreamSoopSettingsView.kt | deepseek-flash | 11.4 |
| Moblin/View/Settings/Streams/Stream/Srt/StreamSrtAdaptiveBitrateSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/srt/StreamSrtAdaptiveBitrateSettingsView.kt | deepseek-flash | 53.0 |
| Moblin/View/Settings/Streams/Stream/Srt/StreamSrtConnectionPriority2View.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/srt/StreamSrtConnectionPriority2View.kt | deepseek-flash | 49.4 |
| Moblin/View/Settings/Streams/Stream/Srt/StreamSrtSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/srt/StreamSrtSettingsView.kt | deepseek-flash | 58.8 |
| Moblin/View/Settings/Streams/Stream/StreamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/StreamSettingsView.kt | deepseek-flash | 12.9 |
| Moblin/View/Settings/Streams/Stream/StreamWizardSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/StreamWizardSettingsView.kt | deepseek-flash | 13.1 |
| Moblin/View/Settings/Streams/Stream/Twitch/StreamTwitchSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/twitch/StreamTwitchSettingsView.kt | deepseek-flash | 9.5 |
| Moblin/View/Settings/Streams/Stream/Url/StreamUrlSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/url/StreamUrlSettingsView.kt | deepseek-flash | 35.5 |
| Moblin/View/Settings/Streams/Stream/Video/StreamVideoSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/video/StreamVideoSettingsView.kt | deepseek-flash | 13.5 |
| Moblin/View/Settings/Streams/Stream/Whip/StreamWhipSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/whip/StreamWhipSettingsView.kt | deepseek-flash | 58.1 |
| Moblin/View/Settings/Streams/Stream/Wizard/Custom/StreamWizardCustomRistSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/custom/StreamWizardCustomRistSettingsView.kt | deepseek-flash | 44.3 |
| Moblin/View/Settings/Streams/Stream/Wizard/Custom/StreamWizardCustomRtmpSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/custom/StreamWizardCustomRtmpSettingsView.kt | deepseek-flash | 39.9 |
| Moblin/View/Settings/Streams/Stream/Wizard/Custom/StreamWizardCustomSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/custom/StreamWizardCustomSettingsView.kt | deepseek-flash | 14.8 |
| Moblin/View/Settings/Streams/Stream/Wizard/Custom/StreamWizardCustomSrtSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/custom/StreamWizardCustomSrtSettingsView.kt | deepseek-flash | 4.6 |
| Moblin/View/Settings/Streams/Stream/Wizard/Custom/StreamWizardCustomWhipSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/custom/StreamWizardCustomWhipSettingsView.kt | deepseek-flash | 28.5 |
| Moblin/View/Settings/Streams/Stream/Wizard/NetworkSetup/MyServers/StreamWizardNetworkSetupMyServersRtmpSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/networksetup/myservers/StreamWizardNetworkSetupMyServersRtmpSettingsView.kt | deepseek-flash | 52.0 |
| Moblin/View/Settings/Streams/Stream/Wizard/NetworkSetup/MyServers/StreamWizardNetworkSetupMyServersSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/networksetup/myservers/StreamWizardNetworkSetupMyServersSettingsView.kt | deepseek-flash | 11.3 |
| Moblin/View/Settings/Streams/Stream/Wizard/NetworkSetup/MyServers/StreamWizardNetworkSetupMyServersSrtSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/networksetup/myservers/StreamWizardNetworkSetupMyServersSrtSettingsView.kt | deepseek-flash | 29.7 |
| Moblin/View/Settings/Streams/Stream/Wizard/NetworkSetup/StreamWizardNetworkSetupBelaboxSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/networksetup/StreamWizardNetworkSetupBelaboxSettingsView.kt | deepseek-flash | 32.9 |
| Moblin/View/Settings/Streams/Stream/Wizard/NetworkSetup/StreamWizardNetworkSetupDirectSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/networksetup/StreamWizardNetworkSetupDirectSettingsView.kt | deepseek-flash | 56.7 |
| Moblin/View/Settings/Streams/Stream/Wizard/NetworkSetup/StreamWizardNetworkSetupObsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/networksetup/StreamWizardNetworkSetupObsSettingsView.kt | deepseek-flash | 38.4 |
| Moblin/View/Settings/Streams/Stream/Wizard/NetworkSetup/StreamWizardNetworkSetupSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/networksetup/StreamWizardNetworkSetupSettingsView.kt | deepseek-flash | 16.1 |
| Moblin/View/Settings/Streams/Stream/Wizard/Platform/StreamWizardKickSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/platform/StreamWizardKickSettingsView.kt | deepseek-flash | 41.9 |
| Moblin/View/Settings/Streams/Stream/Wizard/Platform/StreamWizardObsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/platform/StreamWizardObsSettingsView.kt | deepseek-flash | 11.4 |
| Moblin/View/Settings/Streams/Stream/Wizard/Platform/StreamWizardSoopSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/platform/StreamWizardSoopSettingsView.kt | deepseek-flash | 28.8 |
| Moblin/View/Settings/Streams/Stream/Wizard/Platform/StreamWizardTwitchSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/platform/StreamWizardTwitchSettingsView.kt | deepseek-flash | 32.5 |
| Moblin/View/Settings/Streams/Stream/Wizard/Platform/StreamWizardYouTubeSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/platform/StreamWizardYouTubeSettingsView.kt | deepseek-flash | 61.3 |
| Moblin/View/Settings/Streams/Stream/Wizard/StreamWizardGeneralSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/StreamWizardGeneralSettingsView.kt | deepseek-flash | 29.4 |
| Moblin/View/Settings/Streams/Stream/Wizard/StreamWizardMobcamSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/StreamWizardMobcamSettingsView.kt | deepseek-flash | 36.1 |
| Moblin/View/Settings/Streams/Stream/Wizard/StreamWizardObsRemoteControlSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/wizard/StreamWizardObsRemoteControlSettingsView.kt | deepseek-flash | 39.4 |
| Moblin/View/Settings/Streams/Stream/YouTube/StreamYouTubeSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/stream/youtube/StreamYouTubeSettingsView.kt | deepseek-flash | 79.2 |
| Moblin/View/Settings/Streams/StreamsSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/streams/StreamsSettingsView.kt | deepseek-flash | 57.3 |
| Moblin/View/Settings/Talkback/TalkbackSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/talkback/TalkbackSettingsView.kt | deepseek-flash | 44.4 |
| Moblin/View/Settings/Tesla/TeslaSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/tesla/TeslaSettingsView.kt | deepseek-flash | 44.3 |
| Moblin/View/Settings/Tesla/TeslaVehicleScannerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/tesla/TeslaVehicleScannerSettingsView.kt | deepseek-flash | 29.6 |
| Moblin/View/Settings/Watch/Chat/WatchChatSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/watch/chat/WatchChatSettingsView.kt | deepseek-flash | 3.7 |
| Moblin/View/Settings/Watch/Display/LocalOverlays/WatchLocalOverlaysSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/watch/display/localoverlays/WatchLocalOverlaysSettingsView.kt | deepseek-flash | 30.3 |
| Moblin/View/Settings/Watch/Display/WatchDisplaySettingsView.swift | app/src/main/java/com/moblin/android/view/settings/watch/display/WatchDisplaySettingsView.kt | deepseek-flash | 9.6 |
| Moblin/View/Settings/Watch/WatchSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/watch/WatchSettingsView.kt | deepseek-flash | 10.8 |
| Moblin/View/Settings/WiFiAware/WiFiAwareSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/wifiaware/WiFiAwareSettingsView.kt | deepseek-flash | 77.8 |
| Moblin/View/Settings/WorkoutDevices/WorkoutDeviceScannerSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/workoutdevices/WorkoutDeviceScannerSettingsView.kt | deepseek-flash | 53.5 |
| Moblin/View/Settings/WorkoutDevices/WorkoutDeviceSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/workoutdevices/WorkoutDeviceSettingsView.kt | deepseek-flash | 53.7 |
| Moblin/View/Settings/WorkoutDevices/WorkoutDevicesSettingsView.swift | app/src/main/java/com/moblin/android/view/settings/workoutdevices/WorkoutDevicesSettingsView.kt | deepseek-flash | 13.5 |
| Moblin/View/Stream/CameraLevelView.swift | app/src/main/java/com/moblin/android/view/stream/CameraLevelView.kt | deepseek-flash | 8.6 |
| Moblin/View/Stream/DrawOnStreamView.swift | app/src/main/java/com/moblin/android/view/stream/DrawOnStreamView.kt | deepseek-flash | 48.3 |
| Moblin/View/Stream/Overlay/Right/AudioLevelView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/AudioLevelView.kt | deepseek-flash | 30.2 |
| Moblin/View/Stream/Overlay/Right/CameraSettingsControlView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/CameraSettingsControlView.kt | deepseek-flash | 15.0 |
| Moblin/View/Stream/Overlay/Right/MediaPlayerControlsView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/MediaPlayerControlsView.kt | deepseek-flash | 4.5 |
| Moblin/View/Stream/Overlay/Right/ReplayView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/ReplayView.kt | deepseek-flash | 10.0 |
| Moblin/View/Stream/Overlay/Right/SceneSelectorView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/SceneSelectorView.kt | deepseek-flash | 62.7 |
| Moblin/View/Stream/Overlay/Right/SegmentedPicker.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/SegmentedPicker.kt | deepseek-flash | 4.5 |
| Moblin/View/Stream/Overlay/Right/StreamOverlayRightBeautyView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/StreamOverlayRightBeautyView.kt | deepseek-flash | 55.4 |
| Moblin/View/Stream/Overlay/Right/StreamOverlayRightFaceView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/StreamOverlayRightFaceView.kt | deepseek-flash | 76.3 |
| Moblin/View/Stream/Overlay/Right/StreamOverlayRightPinchView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/StreamOverlayRightPinchView.kt | deepseek-flash | 5.3 |
| Moblin/View/Stream/Overlay/Right/StreamOverlayRightPixellateView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/StreamOverlayRightPixellateView.kt | deepseek-flash | 8.5 |
| Moblin/View/Stream/Overlay/Right/StreamOverlayRightTorchView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/StreamOverlayRightTorchView.kt | deepseek-flash | 5.4 |
| Moblin/View/Stream/Overlay/Right/StreamOverlayRightWhirlpoolView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/StreamOverlayRightWhirlpoolView.kt | deepseek-flash | 9.2 |
| Moblin/View/Stream/Overlay/Right/VideoPreviewView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/VideoPreviewView.kt | deepseek-flash | 34.7 |
| Moblin/View/Stream/Overlay/Right/ZoomPresetSelctorView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/right/ZoomPresetSelctorView.kt | deepseek-flash | 6.4 |
| Moblin/View/Stream/Overlay/StreamOverlayChatView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/StreamOverlayChatView.kt | deepseek-flash | 18.1 |
| Moblin/View/Stream/Overlay/StreamOverlayDebugView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/StreamOverlayDebugView.kt | deepseek-flash | 5.2 |
| Moblin/View/Stream/Overlay/StreamOverlayLeftView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/StreamOverlayLeftView.kt | deepseek-flash | 60.7 |
| Moblin/View/Stream/Overlay/StreamOverlayNavigationView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/StreamOverlayNavigationView.kt | deepseek-flash | 125.6 |
| Moblin/View/Stream/Overlay/StreamOverlayRightView.swift | app/src/main/java/com/moblin/android/view/stream/overlay/StreamOverlayRightView.kt | deepseek-flash | 21.7 |
| Moblin/View/Stream/StreamGridView.swift | app/src/main/java/com/moblin/android/view/stream/StreamGridView.kt | deepseek-flash | 4.0 |
| Moblin/View/Stream/StreamOverlayView.swift | app/src/main/java/com/moblin/android/view/stream/StreamOverlayView.kt | deepseek-flash | 82.2 |
| Moblin/View/Stream/StreamView.swift | app/src/main/java/com/moblin/android/view/stream/StreamView.kt | deepseek-flash | 73.3 |
| Moblin/View/Stream/StreamViewLayout.swift | app/src/main/java/com/moblin/android/view/stream/StreamViewLayout.kt | deepseek-flash | 14.0 |
| Moblin/View/Utils/AddButtonView.swift | app/src/main/java/com/moblin/android/view/utils/AddButtonView.kt | deepseek-flash | 2.6 |
| Moblin/View/Utils/BannersView.swift | app/src/main/java/com/moblin/android/view/utils/BannersView.kt | deepseek-flash | 20.5 |
| Moblin/View/Utils/BorderlessButtonView.swift | app/src/main/java/com/moblin/android/view/utils/BorderlessButtonView.kt | deepseek-flash | 2.4 |
| Moblin/View/Utils/ButtonView.swift | app/src/main/java/com/moblin/android/view/utils/ButtonView.kt | deepseek-flash | 8.3 |
| Moblin/View/Utils/ChatActionButtonsView.swift | app/src/main/java/com/moblin/android/view/utils/ChatActionButtonsView.kt | deepseek-flash | 67.2 |
| Moblin/View/Utils/ChatLineStyle.swift | app/src/main/java/com/moblin/android/view/utils/ChatLineStyle.kt | deepseek-flash | 10.4 |
| Moblin/View/Utils/ChatLineView.swift | app/src/main/java/com/moblin/android/view/utils/ChatLineView.kt | deepseek-flash | 32.6 |
| Moblin/View/Utils/CloseToolbarView.swift | app/src/main/java/com/moblin/android/view/utils/CloseToolbarView.kt | deepseek-flash | 5.2 |
| Moblin/View/Utils/CommandCopyView.swift | app/src/main/java/com/moblin/android/view/utils/CommandCopyView.kt | deepseek-flash | 5.9 |
| Moblin/View/Utils/ContextMenuDeleteButtonView.swift | app/src/main/java/com/moblin/android/view/utils/ContextMenuDeleteButtonView.kt | deepseek-flash | 16.5 |
| Moblin/View/Utils/ContextMenuDuplicateButtonView.swift | app/src/main/java/com/moblin/android/view/utils/ContextMenuDuplicateButtonView.kt | deepseek-flash | 3.3 |
| Moblin/View/Utils/CreateButtonView.swift | app/src/main/java/com/moblin/android/view/utils/CreateButtonView.kt | deepseek-flash | 3.6 |
| Moblin/View/Utils/DraggableItemPrefixView.swift | app/src/main/java/com/moblin/android/view/utils/DraggableItemPrefixView.kt | deepseek-flash | 2.9 |
| Moblin/View/Utils/EmotesPlayer.swift | app/src/main/java/com/moblin/android/view/utils/EmotesPlayer.kt | deepseek-flash | 135.4 |
| Moblin/View/Utils/FontPickerViews.swift | app/src/main/java/com/moblin/android/view/utils/FontPickerViews.kt | deepseek-flash | 58.0 |
| Moblin/View/Utils/FormFieldError.swift | app/src/main/java/com/moblin/android/view/utils/FormFieldError.kt | deepseek-flash | 2.9 |
| Moblin/View/Utils/HCenter.swift | app/src/main/java/com/moblin/android/view/utils/HCenter.kt | deepseek-flash | 11.1 |
| Moblin/View/Utils/IconAndTextView.swift | app/src/main/java/com/moblin/android/view/utils/IconAndTextView.kt | deepseek-flash | 24.9 |
| Moblin/View/Utils/InfoBannerView.swift | app/src/main/java/com/moblin/android/view/utils/InfoBannerView.kt | deepseek-flash | 5.5 |
| Moblin/View/Utils/InlinePickerView.swift | app/src/main/java/com/moblin/android/view/utils/InlinePickerView.kt | deepseek-flash | 33.1 |
| Moblin/View/Utils/MultiLineTextFieldView.swift | app/src/main/java/com/moblin/android/view/utils/MultiLineTextFieldView.kt | deepseek-flash | 18.7 |
| Moblin/View/Utils/NameEditView.swift | app/src/main/java/com/moblin/android/view/utils/NameEditView.kt | deepseek-flash | 7.3 |
| Moblin/View/Utils/OpenAiSettingsView.swift | app/src/main/java/com/moblin/android/view/utils/OpenAiSettingsView.kt | deepseek-flash | 14.4 |
| Moblin/View/Utils/PositionEditView.swift | app/src/main/java/com/moblin/android/view/utils/PositionEditView.kt | deepseek-flash | 8.4 |
| Moblin/View/Utils/QrCodeImageView.swift | app/src/main/java/com/moblin/android/view/utils/QrCodeImageView.kt | deepseek-flash | 11.1 |
| Moblin/View/Utils/RgbColorPickerView.swift | app/src/main/java/com/moblin/android/view/utils/RgbColorPickerView.kt | deepseek-flash | 12.3 |
| Moblin/View/Utils/ShareSheetView.swift | app/src/main/java/com/moblin/android/view/utils/ShareSheetView.kt | deepseek-flash | 3.3 |
| Moblin/View/Utils/ShortcutView.swift | app/src/main/java/com/moblin/android/view/utils/ShortcutView.kt | deepseek-flash | 24.0 |
| Moblin/View/Utils/SizeEditView.swift | app/src/main/java/com/moblin/android/view/utils/SizeEditView.kt | deepseek-flash | 6.7 |
| Moblin/View/Utils/SliderView.swift | app/src/main/java/com/moblin/android/view/utils/SliderView.kt | deepseek-flash | 2.7 |
| Moblin/View/Utils/StrokeModifier.swift | app/src/main/java/com/moblin/android/view/utils/StrokeModifier.kt | deepseek-flash | 43.3 |
| Moblin/View/Utils/SwipeLeftToDeleteButtonView.swift | app/src/main/java/com/moblin/android/view/utils/SwipeLeftToDeleteButtonView.kt | deepseek-flash | 2.9 |
| Moblin/View/Utils/SwipeLeftToDeleteHelpView.swift | app/src/main/java/com/moblin/android/view/utils/SwipeLeftToDeleteHelpView.kt | deepseek-flash | 1.9 |
| Moblin/View/Utils/SwipeLeftToDuplicateButtonView.swift | app/src/main/java/com/moblin/android/view/utils/SwipeLeftToDuplicateButtonView.kt | deepseek-flash | 3.9 |
| Moblin/View/Utils/SwipeLeftToDuplicateOrDeleteHelpView.swift | app/src/main/java/com/moblin/android/view/utils/SwipeLeftToDuplicateOrDeleteHelpView.kt | deepseek-flash | 3.0 |
| Moblin/View/Utils/SwipeLeftToRemoveHelpView.swift | app/src/main/java/com/moblin/android/view/utils/SwipeLeftToRemoveHelpView.kt | deepseek-flash | 3.3 |
| Moblin/View/Utils/TextEditNavigationView.swift | app/src/main/java/com/moblin/android/view/utils/TextEditNavigationView.kt | deepseek-flash | 36.7 |
| Moblin/View/Utils/TextEditView.swift | app/src/main/java/com/moblin/android/view/utils/TextEditView.kt | deepseek-flash | 37.4 |
| Moblin/View/Utils/TextItemView.swift | app/src/main/java/com/moblin/android/view/utils/TextItemView.kt | deepseek-flash | 5.4 |
| Moblin/View/Utils/TextValueView.swift | app/src/main/java/com/moblin/android/view/utils/TextValueView.kt | deepseek-flash | 4.0 |
| Moblin/View/Utils/UrlsView.swift | app/src/main/java/com/moblin/android/view/utils/UrlsView.kt | deepseek-flash | 49.5 |
| Moblin/View/Utils/ValueEditView.swift | app/src/main/java/com/moblin/android/view/utils/ValueEditView.kt | deepseek-flash | 47.0 |
| Moblin/View/Utils/VideoSourceRotationView.swift | app/src/main/java/com/moblin/android/view/utils/VideoSourceRotationView.kt | deepseek-flash | 5.3 |
| Moblin/View/Utils/VoicesView.swift | app/src/main/java/com/moblin/android/view/utils/VoicesView.kt | deepseek-flash | 90.3 |
| Moblin/View/Utils/WiFiSsidEditView.swift | app/src/main/java/com/moblin/android/view/utils/WiFiSsidEditView.kt | deepseek-flash | 40.4 |
| Moblin/View/WebBrowser/WebBrowserView.swift | app/src/main/java/com/moblin/android/view/webbrowser/WebBrowserView.kt | deepseek-flash | 102.1 |
| MoblinTests/Common/Various/ValidateSuite.swift | app/src/test/java/com/moblin/android/common/various/ValidateSuite.kt | deepseek-flash | 6.0 |
| MoblinTests/Moblin/Integrations/Dji/DjiDevice/DjiDeviceSuite.swift | app/src/test/java/com/moblin/android/integrations/dji/djidevice/DjiDeviceSuite.kt | deepseek-flash | 6.4 |
| MoblinTests/Moblin/Integrations/Emotes/EmotesSuite.swift | app/src/test/java/com/moblin/android/integrations/emotes/EmotesSuite.kt | deepseek-flash | 5.1 |
| MoblinTests/Moblin/Integrations/GoPro/GoProBleProtocolSuite.swift | app/src/test/java/com/moblin/android/integrations/gopro/GoProBleProtocolSuite.kt | deepseek-flash | 48.2 |
| MoblinTests/Moblin/Integrations/WorkoutDevice/WorkoutDeviceCrankCadenceSuite.swift | app/src/test/java/com/moblin/android/integrations/workoutdevice/WorkoutDeviceCrankCadenceSuite.kt | deepseek-flash | 10.1 |
| MoblinTests/Moblin/Integrations/WorkoutDevice/WorkoutDeviceCyclingPowerSuite.swift | app/src/test/java/com/moblin/android/integrations/workoutdevice/WorkoutDeviceCyclingPowerSuite.kt | deepseek-flash | 20.7 |
| MoblinTests/Moblin/Integrations/WorkoutDevice/WorkoutDeviceCyclingSpeedCadenceSuite.swift | app/src/test/java/com/moblin/android/integrations/workoutdevice/WorkoutDeviceCyclingSpeedCadenceSuite.kt | deepseek-flash | 50.9 |
| MoblinTests/Moblin/Media/AdaptiveBitrate/AdaptiveBitrateSuite.swift | app/src/test/java/com/moblin/android/media/adaptivebitrate/AdaptiveBitrateSuite.kt | deepseek-flash | 24.9 |
| MoblinTests/Moblin/Media/HaishinKit/Extension/VideoDimensionsSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/extension/VideoDimensionsSuite.kt | deepseek-flash | 11.1 |
| MoblinTests/Moblin/Media/HaishinKit/Media/Audio/AudioUnitSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/media/audio/AudioUnitSuite.kt | deepseek-flash | 9.9 |
| MoblinTests/Moblin/Media/HaishinKit/Media/Audio/BufferedAudioSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/media/audio/BufferedAudioSuite.kt | deepseek-flash | 32.5 |
| MoblinTests/Moblin/Media/HaishinKit/Media/DriftTrackerSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/media/DriftTrackerSuite.kt | deepseek-flash | 21.4 |
| MoblinTests/Moblin/Media/HaishinKit/Media/RecorderSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/media/RecorderSuite.kt | deepseek-flash | 78.6 |
| MoblinTests/Moblin/Media/HaishinKit/Media/Video/BufferedVideoSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/media/video/BufferedVideoSuite.kt | deepseek-flash | 11.6 |
| MoblinTests/Moblin/Media/HaishinKit/Media/Video/VideoColorSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/media/video/VideoColorSuite.kt | deepseek-flash | 121.7 |
| MoblinTests/Moblin/Media/HaishinKit/Mpeg/MpegTsReaderSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/mpeg/MpegTsReaderSuite.kt | deepseek-flash | 34.7 |
| MoblinTests/Moblin/Media/HaishinKit/Mpeg/MpegTsSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/mpeg/MpegTsSuite.kt | deepseek-flash | 16.0 |
| MoblinTests/Moblin/Media/HaishinKit/Mpeg/MpegTsTimecodeGeneratorSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/mpeg/MpegTsTimecodeGeneratorSuite.kt | deepseek-flash | 106.9 |
| MoblinTests/Moblin/Media/HaishinKit/Mpeg/NalUnitReaderSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/mpeg/NalUnitReaderSuite.kt | deepseek-flash | 25.5 |
| MoblinTests/Moblin/Media/HaishinKit/Mpeg/NalUnitSeiSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/mpeg/NalUnitSeiSuite.kt | deepseek-flash | 61.9 |
| MoblinTests/Moblin/Media/HaishinKit/Mpeg/NalUnitWriterSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/mpeg/NalUnitWriterSuite.kt | deepseek-flash | 13.7 |
| MoblinTests/Moblin/Media/HaishinKit/Mpeg/TSTimestampSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/mpeg/TSTimestampSuite.kt | deepseek-flash | 46.8 |
| MoblinTests/Moblin/Media/HaishinKit/Rist/RistSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/rist/RistSuite.kt | deepseek-flash | 37.2 |
| MoblinTests/Moblin/Media/HaishinKit/Rtmp/Amf/AmfSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/rtmp/amf/AmfSuite.kt | deepseek-flash | 94.1 |
| MoblinTests/Moblin/Media/HaishinKit/Rtmp/RtmpStreamInfoSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/rtmp/RtmpStreamInfoSuite.kt | deepseek-flash | 53.6 |
| MoblinTests/Moblin/Media/HaishinKit/Rtmp/RtmpStreamSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/rtmp/RtmpStreamSuite.kt | deepseek-flash | 27.6 |
| MoblinTests/Moblin/Media/HaishinKit/Rtmp/RtmpSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/rtmp/RtmpSuite.kt | deepseek-flash | 42.1 |
| MoblinTests/Moblin/Media/HaishinKit/Srt/SrtSenderSuite.swift | app/src/test/java/com/moblin/android/media/haishinkit/srt/SrtSenderSuite.kt | deepseek-flash | 61.2 |
| MoblinTests/Moblin/Media/HaishinKit/Util/Md5Suite.swift | app/src/test/java/com/moblin/android/media/haishinkit/util/Md5Suite.kt | deepseek-flash | 7.9 |
| MoblinTests/Moblin/Media/MobcamStream/MobcamStreamSuite.swift | app/src/test/java/com/moblin/android/media/mobcamstream/MobcamStreamSuite.kt | deepseek-flash | 38.4 |
| MoblinTests/Moblin/Media/RtspClient/RtspClientSuite.swift | app/src/test/java/com/moblin/android/media/rtspclient/RtspClientSuite.kt | deepseek-flash | 8.9 |
| MoblinTests/Moblin/Media/Srtla/SrtSuite.swift | app/src/test/java/com/moblin/android/media/srtla/SrtSuite.kt | deepseek-flash | 8.3 |
| MoblinTests/Moblin/Media/WavSuite.swift | app/src/test/java/com/moblin/android/media/WavSuite.kt | deepseek-flash | 35.4 |
| MoblinTests/Moblin/Media/Webrtc/WebrtcIngestClientSuite.swift | app/src/test/java/com/moblin/android/media/webrtc/WebrtcIngestClientSuite.kt | deepseek-flash | 14.2 |
| MoblinTests/Moblin/Media/WrappingTimestampSuite.swift | app/src/test/java/com/moblin/android/media/WrappingTimestampSuite.kt | deepseek-flash | 15.1 |
| MoblinTests/Moblin/Moblink/MoblinkSuite.swift | app/src/test/java/com/moblin/android/moblink/MoblinkSuite.kt | deepseek-flash | 25.3 |
| MoblinTests/Moblin/Obs/ObsWebSocketServerMock.swift | app/src/test/java/com/moblin/android/obs/ObsWebSocketServerMock.kt | deepseek-flash | 93.1 |
| MoblinTests/Moblin/Obs/ObsWebSocketSuite.swift | app/src/test/java/com/moblin/android/obs/ObsWebSocketSuite.kt | deepseek-flash | 140.0 |
| MoblinTests/Moblin/RemoteControl/RemoteControlSuite.swift | app/src/test/java/com/moblin/android/remotecontrol/RemoteControlSuite.kt | deepseek-flash | 122.8 |
| MoblinTests/Moblin/StreamingPlatforms/Kick/KickChatSegmentsSuite.swift | app/src/test/java/com/moblin/android/streamingplatforms/kick/KickChatSegmentsSuite.kt | deepseek-flash | 5.5 |
| MoblinTests/Moblin/StreamingPlatforms/Twitch/CheermotesSuite.swift | app/src/test/java/com/moblin/android/streamingplatforms/twitch/CheermotesSuite.kt | deepseek-flash | 17.3 |
| MoblinTests/Moblin/StreamingPlatforms/Twitch/TwitchChatSegmentsSuite.swift | app/src/test/java/com/moblin/android/streamingplatforms/twitch/TwitchChatSegmentsSuite.kt | deepseek-flash | 10.3 |
| MoblinTests/Moblin/StreamingPlatforms/Twitch/TwitchChatSuite.swift | app/src/test/java/com/moblin/android/streamingplatforms/twitch/TwitchChatSuite.kt | deepseek-flash | 75.3 |
| MoblinTests/Moblin/StreamingPlatforms/Twitch/TwitchEventSubSuite.swift | app/src/test/java/com/moblin/android/streamingplatforms/twitch/TwitchEventSubSuite.kt | deepseek-flash | 28.1 |
| MoblinTests/Moblin/StreamingPlatforms/Twitch/TwitchRaidHistorySuite.swift | app/src/test/java/com/moblin/android/streamingplatforms/twitch/TwitchRaidHistorySuite.kt | deepseek-flash | 5.8 |
| MoblinTests/Moblin/StreamingPlatforms/YouTube/YouTubeLiveChatSuite.swift | app/src/test/java/com/moblin/android/streamingplatforms/youtube/YouTubeLiveChatSuite.kt | deepseek-flash | 11.7 |
| MoblinTests/Moblin/Various/ChatBotCommandSuite.swift | app/src/test/java/com/moblin/android/various/ChatBotCommandSuite.kt | deepseek-flash | 45.2 |
| MoblinTests/Moblin/Various/ChatPostEmoteSuite.swift | app/src/test/java/com/moblin/android/various/ChatPostEmoteSuite.kt | deepseek-flash | 6.3 |
| MoblinTests/Moblin/Various/ChatPostTextSuite.swift | app/src/test/java/com/moblin/android/various/ChatPostTextSuite.kt | deepseek-flash | 16.2 |
| MoblinTests/Moblin/Various/Model/Chat/ChatProviderSuite.swift | app/src/test/java/com/moblin/android/various/model/chat/ChatProviderSuite.kt | deepseek-flash | 18.2 |
| MoblinTests/Moblin/Various/Network/HttpClientSuite.swift | app/src/test/java/com/moblin/android/various/network/HttpClientSuite.kt | deepseek-flash | 22.8 |
| MoblinTests/Moblin/Various/Network/HttpProxyServerSuite.swift | app/src/test/java/com/moblin/android/various/network/HttpProxyServerSuite.kt | deepseek-flash | 17.9 |
| MoblinTests/Moblin/Various/Network/NetworkUtilsSuite.swift | app/src/test/java/com/moblin/android/various/network/NetworkUtilsSuite.kt | deepseek-flash | 31.4 |
| MoblinTests/Moblin/Various/Settings/SettingsMacrosSuite.swift | app/src/test/java/com/moblin/android/various/settings/SettingsMacrosSuite.kt | deepseek-flash | 12.5 |
| MoblinTests/Moblin/Various/Settings/SettingsMoblinkSuite.swift | app/src/test/java/com/moblin/android/various/settings/SettingsMoblinkSuite.kt | deepseek-flash | 5.0 |
| MoblinTests/Moblin/Various/Settings/SettingsSuite.swift | app/src/test/java/com/moblin/android/various/settings/SettingsSuite.kt | deepseek-flash | 10.1 |
| MoblinTests/Moblin/Various/Storages/ReplaysStorageSuite.swift | app/src/test/java/com/moblin/android/various/storages/ReplaysStorageSuite.kt | deepseek-flash | 8.4 |
| MoblinTests/Moblin/Various/Subtitles/SubtitlesSuite.swift | app/src/test/java/com/moblin/android/various/subtitles/SubtitlesSuite.kt | deepseek-flash | 24.2 |
| MoblinTests/Moblin/Various/Subtitles/TextAlignerSuite.swift | app/src/test/java/com/moblin/android/various/subtitles/TextAlignerSuite.kt | deepseek-flash | 4.8 |
| MoblinTests/Moblin/Various/Utils/CameraUtilsSuite.swift | app/src/test/java/com/moblin/android/various/utils/CameraUtilsSuite.kt | deepseek-flash | 25.1 |
| MoblinTests/Moblin/Various/Utils/ChatLineViewSuite.swift | app/src/test/java/com/moblin/android/various/utils/ChatLineViewSuite.kt | deepseek-flash | 41.2 |
| MoblinTests/Moblin/Various/Utils/CmTimeSuite.swift | app/src/test/java/com/moblin/android/various/utils/CmTimeSuite.kt | deepseek-flash | 44.9 |
| MoblinTests/Moblin/Various/Utils/UtilsSuite.swift | app/src/test/java/com/moblin/android/various/utils/UtilsSuite.kt | deepseek-flash | 15.6 |
| MoblinTests/Moblin/VideoEffects/EffectUtilsSuite.swift | app/src/test/java/com/moblin/android/videoeffects/EffectUtilsSuite.kt | deepseek-flash | 56.4 |
| MoblinTests/Moblin/VideoEffects/LutEffectSuite.swift | app/src/test/java/com/moblin/android/videoeffects/LutEffectSuite.kt | deepseek-flash | 111.8 |
| MoblinTests/Moblin/VideoEffects/Text/TextEffectSuite.swift | app/src/test/java/com/moblin/android/videoeffects/text/TextEffectSuite.kt | deepseek-flash | 157.7 |
| MoblinTests/Moblin/View/Settings/Macros/MacrosSettingsViewSuite.swift | app/src/test/java/com/moblin/android/view/settings/macros/MacrosSettingsViewSuite.kt | deepseek-flash | 8.4 |
| MoblinTests/TestUtils.swift | app/src/test/java/com/moblin/android/TestUtils.kt | deepseek-flash | 38.4 |
