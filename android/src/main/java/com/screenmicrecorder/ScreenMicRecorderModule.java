package com.screenmicrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.os.Build;
import android.util.Log;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.content.Context;
import android.app.Activity;


import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.File;

@ReactModule(name = ScreenMicRecorderModule.NAME)
public class ScreenMicRecorderModule extends ReactContextBaseJavaModule implements HBRecorderListener {
  public static final String NAME = "ScreenMicRecorder";
  private final ReactApplicationContext reactContext;
  private final int SCREEN_RECORD_REQUEST_CODE = 1000;
  private Promise startPromise;
  private Promise stopPromise;
  private HBRecorder hbRecorder;

  public ScreenMicRecorderModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    // Listener to handle user acceptance of screen recording permissions
    reactContext.addActivityEventListener(new BaseActivityEventListener() {
      @Override
      public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if (requestCode != SCREEN_RECORD_REQUEST_CODE) return;

        if (resultCode == Activity.RESULT_CANCELED) {
          Log.d("ScreenMicRecorder", "User denied permission");
          startPromise.resolve("userDeniedPermission");
          return;
        }

        if (resultCode == Activity.RESULT_OK) {
          Log.d("ScreenMicRecorder", "User accepted permission");
          hbRecorder.startScreenRecording(intent, resultCode);
        }
      }
    });
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod public void addListener(String ignoredEventName) {}
  @ReactMethod public void removeListeners(Integer ignoredCount) {}

  @ReactMethod
  public void startRecording(ReadableMap config, Promise promise){
    startPromise = promise;
    File outputUri = this.reactContext.getExternalFilesDir("RecordScreen");
    Log.d("ScreenMicRecorder","startRecording path: " + outputUri.getAbsolutePath());

    hbRecorder= new HBRecorder(this.reactContext,this);
    hbRecorder.isAudioEnabled(!config.hasKey("mic") || (boolean) config.getBoolean("mic"));
    hbRecorder.setVideoEncoder("DEFAULT");
    hbRecorder.setOutputPath(outputUri.toString());

    boolean notificationActionEnabled = config.hasKey("notificationActionEnabled") && (boolean) config.getBoolean("notificationActionEnabled");
    // hbRecorder.setNotificationActionEnabled(notificationActionEnabled);
    if (!notificationActionEnabled) hbRecorder.setNotificationDescription("Stop recording from the application");

    try{ // Requesting user permissions
      MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) reactContext.getSystemService (Context.MEDIA_PROJECTION_SERVICE);
      getCurrentActivity().startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_RECORD_REQUEST_CODE);
    } catch (Exception e) {
      startPromise.reject("404",e.getMessage());
    }
  }

  @ReactMethod
  public void stopRecording(Promise promise){
    Log.d("ScreenMicRecorder","stopRecording");
    stopPromise=promise;
    hbRecorder.stopScreenRecording();
  }

  @ReactMethod
  public void deleteRecording(String filename, Promise promise){
    File fdelete = new File(filename);
    if (!fdelete.exists()) return;
    if (fdelete.delete()) {
      Log.d("ScreenMicRecorder","deleteRecording " + filename);
    } else {
      Log.d("ScreenMicRecorder","unable to delete " + filename);
    }
  }

  // HB Recorder Events
  @Override
  public void HBRecorderOnStart() {
    Log.d("ScreenMicRecorder","HBRecorder Started ");
    startPromise.resolve("started");
  }

  @Override
  public void HBRecorderOnComplete() {
    String uri = hbRecorder.getFilePath();
    Log.d("ScreenMicRecorder","HBRecorder Completed. URI: " + uri);
    if (stopPromise != null)  stopPromise.resolve(uri);

    // Send event to JS
    WritableMap params = Arguments.createMap();
    params.putString("value", uri);
    this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("stopEvent", params);
  }

  @Override
  public void HBRecorderOnError(int errorCode, String reason) {
    Log.d("ScreenMicRecorder", "HBRecorderOnError : " + errorCode + " " + reason);
    startPromise.reject("404", "RecorderOnError:" + errorCode + " " + reason);
  }

  @Override
  public void HBRecorderOnPause() {

  }

  @Override
  public void HBRecorderOnResume() {

  }
}
