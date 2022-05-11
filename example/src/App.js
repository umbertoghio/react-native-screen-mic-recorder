import React, { useState } from 'react'
import { View, Text, StatusBar, TouchableHighlight, Button } from 'react-native'
import Video from 'react-native-video'
import ScreenRecorder from 'react-native-screen-recorder'
import { Lorem } from './Lorem'
import { styles } from './Styles'

export default function App () {
  const [uri, setUri] = useState('')
  const [recording, setRecording] = useState(false)

  const deleteRecording = () => {
    ScreenRecorder.deleteRecording(uri)
    setUri('')
  }

  const handleOnRecording = async () => {
    if (recording) {
      setRecording(false)
      const uri = await ScreenRecorder.stopRecording().catch((error) =>
        console.warn(error)
      )
      console.log('uri', uri)
      uri && setUri(uri)
    } else {
      uri && deleteRecording()
      setRecording(true)
      await ScreenRecorder.startRecording({ mic: false }).catch((error) => {
        console.warn(error)
        setRecording(false)
        setUri('')
      })
    }
  }

  return (
    <>
      <StatusBar barStyle='light-content' />
      <View style={styles.navbar}>
        {recording
          ? <View style={styles.recordingMark}><Text style={styles.recordingMarkText}>Recording</Text></View>
          : (uri ? <View><Button onPress={deleteRecording} title='Delete Recording' /></View> : null)}
      </View>
      <Lorem />
      <View style={styles.btnContainer}>
        <TouchableHighlight onPress={handleOnRecording}>
          <View style={styles.btnWrapper}>
            <View style={recording ? styles.btnActive : styles.btnDefault} />
          </View>
        </TouchableHighlight>
      </View>
      {uri ? <View style={styles.preview}><Video source={{ uri }} style={styles.video} /></View> : null}
    </>
  )
}
