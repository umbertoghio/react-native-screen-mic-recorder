# react-native-screen-mic-recorder

A screen and microphone record module for React Native.

This is a cusomized and improved fork of [react-native-record-screen](https://github.com/yutasuzuki/react-native-record-screen)

- Support iOS >= 11.0 (only on real device)

- Support Android
  - minSdkVersion = 26
  - compileSdkVersion = 29
  - targetSdkVersion = 29
  - use [HBRecorder](https://github.com/HBiSoft/HBRecorder)

## Installation

```sh
yarn add react-native-screen-mic-recorder
```

### iOS

add Usage Description in info.plist

```
<key>NSCameraUsageDescription</key>
<string>Please allow use of camera</string>
<key>NSMicrophoneUsageDescription</key>
<string>Please allow use of microphone</string>
```

Install pods from the ios folder

```sh
npx pod-install
```

### Android

Add permissions in AndroidManifest.xml

```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_INTERNAL_STORAGE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## Usage

See the example application for full Android and iOS Example
### Record Screen

```js
import ScreenRecorder from 'react-native-screen-mic-recorder'

// This options can be passed to startRecording
const options = {
  mic: true | false // defaults to true
  width: ? // Defaults to Dimensions.get('window').width, ignored on Android
  height: ? // Defaults to Dimensions.get('window').height, ignored on Android
  androidBannerStopRecordingHandler: fn() // Android Only: Callback function to handle stop recording from notification baner
}


// Start Recording
const recordingStatus = await ScreenRecorder.startRecording(options).catch((error) => {
  console.warn(error) // handle native error
})

if (recordingStatus === 'started') ... // Recording has started 
if (recordingStatus === 'userDeniedPermission') Alert.alert('Plesae grant permission in order to record screen')

// Stop Recording
const uri = await ScreenRecorder.stopRecording().catch((error) =>
  console.warn(error) // handle native error
)
// uri is the path to the recorded video

const androidBannerStopRecordingHandler = (uri) => {
  console.log('video uri, recording stopped from Android notification banner', uri)
}
```
### Delete recorded video

The recorded video is saved inside the App Sandbox and can be added to the camera roll with appropriate code.
Once done you can delete the recorded video with 

```js
  ScreenRecorder.deleteRecording(uri)
```

## License

MIT