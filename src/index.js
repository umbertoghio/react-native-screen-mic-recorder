import { NativeModules, Dimensions } from 'react-native'

const { ScreenRecorder } = NativeModules

class ReactNativeScreenRecorderClass {
  setup (config = {}) {
    const defaults = {
      mic: true,
      width: Dimensions.get('window').width,
      height: Dimensions.get('window').height
    }
    ScreenRecorder.setup({ ...defaults, ...config })
  }

  startRecording (config = {}) {
    this.setup(config)
    return new Promise((resolve, reject) => {
      ScreenRecorder.startRecording().then(resolve).catch(reject)
    })
  }

  stopRecording () {
    return new Promise((resolve, reject) => {
      ScreenRecorder.stopRecording().then(resolve).catch(reject)
    })
  }

  clean (file) {
    ScreenRecorder.clean(file)
  }
}

const ReactNativeScreenRecorder = new ReactNativeScreenRecorderClass()

export default ReactNativeScreenRecorder
