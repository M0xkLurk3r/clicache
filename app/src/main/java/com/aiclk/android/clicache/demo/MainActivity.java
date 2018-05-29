package com.aiclk.android.clicache.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import com.aiclk.android.clicache.universe.CliCash;
import com.aiclk.android.clicache.videocache.VideoCache;

public class MainActivity extends AppCompatActivity {

	private VideoCache vcache = null;
	private final Object vcacheObjectLock = new Object();
	private String videoUrl = "http://192.168.66.11:83/video/1925.mp4";
	private static final int REQUEST_SELF_PERM = 0xb00d;

	private void _setupVCacheImpl() {
		vcache = new VideoCache(getApplicationContext());
		vcache.prepare();
	}

	private void setupVCache() {
		synchronized (vcacheObjectLock) {
			try {
				if (vcache == null) {
					_setupVCacheImpl();
				}
				if (vcache.getCliCashStatus() == CliCash.CLICASH_STOP) {
					// Make damn sure that the vcache were dead dead.
					vcache.terminate();
					_setupVCacheImpl();
				}
			} catch (Exception e) {
				Log.i("MPlayerView", "Cannot prepare VCache, give up vcache initialization.");
				vcache = null;
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
			if (checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
			||	checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_SELF_PERM);
			}
		} else {
			setupVCache();
		}

		VideoView vv = (VideoView) findViewById(R.id.videoView);
		vv.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (! ((VideoView)v).isPlaying()) {
					if (vcache != null) {
						Log.i("demo", "using video cache");
						((VideoView) v).setVideoPath(vcache.getCacheBaseUrl(videoUrl));
					} else {
						Log.i("demo", "not using video cache");
						((VideoView) v).setVideoPath(videoUrl);
					}
					((VideoView) v).start();
				} else {
					((VideoView)v).stopPlayback();
				}
			}
		});
	}

	@Override
	public void onRequestPermissionsResult(
			int requestCode, String permissions[], int[] grantResults) {
		//	imp_adbrowser.whenPermDialogReturns(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_SELF_PERM) {
			if (grantResults.length > 0) {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					setupVCache();
				}
			}
		}
	}
}
