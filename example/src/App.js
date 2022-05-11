import React, { useState } from 'react'
import { View, Text, StatusBar, TouchableHighlight, Button, Alert } from 'react-native'
import Video from 'react-native-video'
import ScreenRecorder from 'react-native-screen-mic-recorder'
import { Lorem } from './Lorem'
import { styles } from './Styles'

export default function App () {
  const [uri, setUri] = useState('')
  const [recording, setRecording] = useState(false)

  const handleDeleteRecording = () => {
    ScreenRecorder.deleteRecording(uri)
    setUri('')
  }

  const handleStartRecording = async () => {
    // Deletes previous recording if exists
    uri && handleDeleteRecording()

    const recording = await ScreenRecorder.startRecording({ mic: false }).catch((error) => {
      console.warn(error) // handle native error
    })

    if (recording === 'started') setRecording(true)
    if (recording === 'userDeniedPermission') Alert.alert('Plesae grant permission in order to record screen')
  }

  const handleStopRecording = async () => {
    setRecording(false)
    const uri = await ScreenRecorder.stopRecording().catch((error) =>
      console.warn(error) // handle native error
    )
    uri && setUri(uri)
  }

  return (
    <>
      <StatusBar barStyle='light-content' />
      <View style={styles.navbar}>
        {recording
          ? <View style={styles.recordingMark}><Text style={styles.recordingMarkText}>Recording</Text></View>
          : (uri ? <View><Button onPress={handleDeleteRecording} title='Delete Recording' /></View> : null)}
      </View>
      <Lorem />
      <View style={styles.btnContainer}>
        <TouchableHighlight onPress={recording ? handleStopRecording : handleStartRecording}>
          <View style={styles.btnWrapper}>
            <View style={recording ? styles.btnActive : styles.btnDefault} />
          </View>
        </TouchableHighlight>
      </View>
      {uri ? <View style={styles.preview}><Video source={{ uri }} style={styles.video} /></View> : null}
    </>
  )
}
