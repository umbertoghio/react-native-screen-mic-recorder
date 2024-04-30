import { NativeModules, Platform, Dimensions, NativeEventEmitter, } from 'react-native'

const LINKING_ERROR =
  `The package 'react-native-screen-mic-recorder' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const ScreenMicRecorder = NativeModules.ScreenMicRecorder
  ? NativeModules.ScreenMicRecorder
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const { stopRecording, deleteRecording } = ScreenMicRecorder
const listeners = {}

const ReactNativeScreenMicRecorder = {
  startRecording: (config = {}) => {
    let notificationActionEnabled = false
    if (
      config?.androidBannerStopRecordingHandler && typeof config?.androidBannerStopRecordingHandler === 'function' && Platform.OS === 'android') {
      notificationActionEnabled = true
      typeof listeners?.eventListener?.remove === 'function' && listeners.eventListener.remove()
      const eventEmitter = new NativeEventEmitter(ScreenMicRecorder)
      listeners.eventListener = eventEmitter.addListener('stopEvent', ({ value = '' } = {}) => config.androidBannerStopRecordingHandler(value))
    }

    return ScreenMicRecorder.startRecording({
      mic: !config.mic === false,
      notificationActionEnabled,
      width: Dimensions.get('window').width,
      height: Dimensions.get('window').height,
      ...config
    })
  },
  stopRecording,
  deleteRecording
}

export default ReactNativeScreenMicRecorder
