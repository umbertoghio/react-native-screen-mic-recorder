import { StyleSheet, Dimensions } from 'react-native'

export const styles = StyleSheet.create({
  navbar: {
    height: 80,
    backgroundColor: '#212121',
    alignItems: 'center',
    justifyContent: 'flex-end'
  },
  recordingMark: {
    backgroundColor: 'red',
    paddingVertical: 6,
    paddingHorizontal: 16,
    marginBottom: 10,
    borderRadius: 24
  },
  recordingMarkText: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#fff'
  },
  btnContainer: {
    height: 100,
    paddingTop: 12,
    alignItems: 'center',
    justifyContent: 'flex-start',
    backgroundColor: '#212121'
  },
  btnWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 60,
    height: 60,
    backgroundColor: '#fff',
    borderRadius: 30
  },
  btnDefault: {
    width: 48,
    height: 48,
    backgroundColor: '#fff',
    borderRadius: 24,
    borderWidth: 4,
    borderStyle: 'solid',
    borderColor: '#212121'
  },
  btnActive: {
    width: 36,
    height: 36,
    backgroundColor: 'red',
    borderRadius: 8
  },
  preview: {
    position: 'absolute',
    right: 12,
    bottom: 116,
    width: Dimensions.get('window').width / 2,
    height: Dimensions.get('window').height / 3,
    zIndex: 1,
    padding: 8,
    backgroundColor: '#aaa'
  },
  video: {
    flex: 1
  }
})
