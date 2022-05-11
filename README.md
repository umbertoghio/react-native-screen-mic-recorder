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
}


// Start Recording
const recording = await ScreenRecorder.startRecording({mic: false}).catch((error) => {
  console.warn(error) // handle native error
})

if (recording === 'started') ... // Recording has started 
if (recording === 'userDeniedPermission') Alert.alert('Plesae grant permission in order to record screen')

// Stop Recording
const uri = await ScreenRecorder.stopRecording().catch((error) =>
  console.warn(error) // handle native error
)
// uri is the path to the recorded video
```
### Delete recorded video

The recorded video is saved inside the App Sandbox and can be added to the camera roll with appropriate code.
Once done you can delete the recorded video with 

```js
  ScreenRecorder.deleteRecording(uri)
```

## License

MIT