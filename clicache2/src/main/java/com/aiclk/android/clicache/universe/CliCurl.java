package com.aiclk.android.clicache.universe;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;

/**
 * Created by anthony on 4/26/18.
 */

public abstract class CliCurl {

	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;

	private static final int DEFAULT_BUF_LEN = 2048;

	public void Logging(int priority, String message) {
		Log.println(priority, "CliCurl", new StringBuilder().append("ID: ")
				.append(String.valueOf(this.hashCode())).append(", msg: ").append(message).toString());
	}

	public static CliCurl open(String url) {
		// TODO: Implement this
		return new AsyncCliCurl(url);
	}

	public CliCurl setMethod(int method) {
		throw new RuntimeException("Instance not present!");
	}

	public CliCurl request() throws IOException {
		throw new RuntimeException("Instance not present!");
	}

	public CliCurl preparePostData(ByteBuffer postdata) {
		throw new RuntimeException("Instance not present!");
	}

	public Channel getReadChannel() {
		throw new RuntimeException("Instance not present!");
	}

	public void read(ByteBuffer buffer) throws IOException {
		throw new RuntimeException("Instance not present!");
	}

	public void write(ByteBuffer buffer) throws IOException {
		throw new RuntimeException("Instance not present!");
	}

	private static class AsyncCliCurl extends CliCurl {

		private int mMethod = METHOD_GET;
		private SocketChannel mChannel = null;
		private String mUrl = null;

		public AsyncCliCurl(String url) {
			AsyncCliCurlInit(url, DEFAULT_BUF_LEN);
		}

		@Deprecated
		public AsyncCliCurl(String url, int bufLength) {
			AsyncCliCurlInit(url, bufLength);
		}


		private void AsyncCliCurlInit(String url, int bufLength) {
			mUrl = url;
		}

		@Override
		public CliCurl setMethod(int method) {
			mMethod = method;
			return this;
		}

		@Override
		public CliCurl request() throws IOException {
			// TODO: Implement this
			mChannel = SocketChannel.open();


			return this;
		}

		@Override
		public Channel getReadChannel() {
			// TODO: Implement this
			return mChannel;
		}

		@Override
		public void read(ByteBuffer dest) throws IOException {
			if (mChannel != null)
				mChannel.read(dest);
			else
				Logging(Log.ERROR, "Did not ready!");
		}

		@Override
		public void write(ByteBuffer src) throws IOException {
			if (mChannel != null)
				mChannel.write(src);
			else
				Logging(Log.ERROR, "Did not ready!");
		}


		public CliCurl preparePostData(byte[] postData) {
			return this;
		}

	}
}

