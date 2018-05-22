package com.aiclk.android.clicache.videocache.listener;

import com.aiclk.android.clicache.videocache.VideoCache;

/**
 * Created by anthony on 5/5/18.
 */

public interface VideoCacheEventListener {
	void onHTTPError(VideoCache videoCache, Exception e);
	void onIllegalCommandError(VideoCache videoCache, String desc);
}
