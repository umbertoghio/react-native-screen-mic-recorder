import { NativeModules, Dimensions } from 'react-native'

const { ScreenRecorder } = NativeModules
const { stopRecording, deleteRecording } = ScreenRecorder

const ReactNativeScreenRecorder = {
  startRecording: (config = {}) => {
    ScreenRecorder.setup({
      mic: true,
      width: Dimensions.get('window').width,
      height: Dimensions.get('window').height,
      ...config
    })
    return ScreenRecorder.startRecording()
  },
  stopRecording,
  deleteRecording
}

export default ReactNativeScreenRecorder
