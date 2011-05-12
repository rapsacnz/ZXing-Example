package com.example.merchant.activity;

import java.io.IOException;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.example.merchant.FinishListener;
import com.example.merchant.R;
import com.example.merchant.ViewfinderView;
import com.example.merchant.camera.CameraManager;
import com.example.merchant.decode.DecodeFormatManager;
import com.example.merchant.service.IParentActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;


public class CameraCaptureActivityDemo extends RotateableActivity implements SurfaceHolder.Callback, IParentActivity {

	private static final String TAG = CameraCaptureActivityDemo.class.getSimpleName();

	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;

	ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.camera_capture);

		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.camera_view_viewfinder_view);

		handler = null;
		hasSurface = false;

		preinitCamera();

	}

	@Override
	protected void onResume() {

		super.onResume();

		Intent intent = getIntent();
		decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
	}

	private void preinitCamera() {
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.camera_view_preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	public void handleDecode(Result rawResult, Bitmap barcode) {
		//here is were you take the result and process it, etc
	}



	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
			return;
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializating camera", e);
			displayFrameworkBugMessageAndExit();
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.app_name));
		builder.setMessage(getString(R.string.msg_camera_framework_bug));
		builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

}
