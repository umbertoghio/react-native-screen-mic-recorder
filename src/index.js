import { NativeModules, Dimensions } from 'react-native'

const { ScreenRecorder } = NativeModules
const { stopRecording, deleteRecording } = ScreenRecorder

const ReactNativeScreenRecorder = {
  startRecording: (config = {}) => {
    return ScreenRecorder.startRecording({
      mic: true,
      width: Dimensions.get('window').width,
      height: Dimensions.get('window').height,
      ...config
    })
  },
  stopRecording,
  deleteRecording
}

export default ReactNativeScreenRecorder
