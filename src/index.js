import { NativeModules, Dimensions, NativeEventEmitter, Platform } from 'react-native'

const { ScreenRecorder } = NativeModules
const { stopRecording, deleteRecording } = ScreenRecorder

const listeners = {}

const ReactNativeScreenMicRecorder = {
  startRecording: (config = {}) => {
    let notificationActionEnabled = false
    if (
      config?.androidBannerStopRecordingHandler && typeof config?.androidBannerStopRecordingHandler === 'function' && Platform.OS === 'android') {
      notificationActionEnabled = true
      typeof listeners?.eventListener?.remove === 'function' && listeners.eventListener.remove()
      const eventEmitter = new NativeEventEmitter(ScreenRecorder)
      listeners.eventListener = eventEmitter.addListener('stopEvent', ({ value = '' } = {}) => config.androidBannerStopRecordingHandler(value))
    }

    return ScreenRecorder.startRecording({
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
