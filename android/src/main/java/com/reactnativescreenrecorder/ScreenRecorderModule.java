package com.reactnativescreenrecorder;

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

@ReactModule(name = ScreenRecorderModule.NAME)
public class ScreenRecorderModule extends ReactContextBaseJavaModule implements HBRecorderListener {
  public static final String NAME = "ScreenRecorder";
  private final ReactApplicationContext reactContext;
  private final int SCREEN_RECORD_REQUEST_CODE = 1000;
  private Promise startPromise;
  private Promise stopPromise;
  private HBRecorder hbRecorder;

  public ScreenRecorderModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    // Listener to handle user acceptance of screen recording permissions
    reactContext.addActivityEventListener(new BaseActivityEventListener() {
      @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
      @Override
      public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if (requestCode != SCREEN_RECORD_REQUEST_CODE) return;

        if (resultCode == Activity.RESULT_CANCELED) {
          Log.d("ScreenRecorder", "User denied permission");
          startPromise.resolve("userDeniedPermission");
          return;
        }

        if (resultCode == Activity.RESULT_OK) {
          Log.d("ScreenRecorder", "User accepted permission");
          hbRecorder.startScreenRecording(intent, resultCode, getCurrentActivity());
        }
      }
    });
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @ReactMethod
  public void startRecording(ReadableMap config, Promise promise){
    startPromise = promise;
    File outputUri = this.reactContext.getExternalFilesDir("RecordScreen");
    Log.d("ScreenRecorder","startRecording path: " + outputUri.getAbsolutePath());

    hbRecorder= new HBRecorder(this.reactContext,this);
    hbRecorder.isAudioEnabled(!config.hasKey("mic") || (boolean) config.getBoolean("mic"));
    hbRecorder.setVideoEncoder("DEFAULT");
    hbRecorder.setOutputPath(outputUri.toString());

    boolean notificationActionEnabled = config.hasKey("notificationActionEnabled") && (boolean) config.getBoolean("notificationActionEnabled");
    hbRecorder.setNotificationActionEnabled(notificationActionEnabled);
    if (!notificationActionEnabled) hbRecorder.setNotificationDescription("Stop recording from the application");

    try{ // Requesting user permissions
      MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) reactContext.getSystemService (Context.MEDIA_PROJECTION_SERVICE);
      getCurrentActivity().startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_RECORD_REQUEST_CODE);
    } catch (Exception e) {
      startPromise.reject("404",e.getMessage());
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @ReactMethod
  public void stopRecording(Promise promise){
    Log.d("ScreenRecorder","stopRecording");
    stopPromise=promise;
    hbRecorder.stopScreenRecording();
  }

  @ReactMethod
  public void deleteRecording(String filename, Promise promise){
    File fdelete = new File(filename);
    if (!fdelete.exists()) return;
    if (fdelete.delete()) {
      Log.d("ScreenRecorder","deleteRecording " + filename);
    } else {
      Log.d("ScreenRecorder","unable to delete " + filename);
    }
  }

  // HB Recorder Events
  @Override
  public void HBRecorderOnStart() {
    Log.d("ScreenRecorder","HBRecorder Started ");
    startPromise.resolve("started");
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void HBRecorderOnComplete() {
    String uri = hbRecorder.getFilePath();
    Log.d("ScreenRecorder","HBRecorder Completed. URI: " + uri);
    if (stopPromise != null)  stopPromise.resolve(uri);

    // Send event to JS
    WritableMap params = Arguments.createMap();
    params.putString("value", uri);
    this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("stopEvent", params);
  }

  @Override
  public void HBRecorderOnError(int errorCode, String reason) {
    Log.d("ScreenRecorder", "HBRecorderOnError : " + errorCode + " " + reason);
    startPromise.reject("404", "RecorderOnError:" + errorCode + " " + reason);
  }
}
