package com.aiclk.android.clicache.universe;

import android.util.Log;

import com.aiclk.android.clicache.universe.listener.CliCashEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

/**
 * Created by anthony on 4/26/18.
 */

public abstract class CliCash {

	private String mBaseURL;
	private static Thread mListenerThread = null;
	private static final String mTag = "CliCash";
	private static final String mVer = "8.8.2";
	private static final String DEFAULT_RESPONSE = "It works!";
	public static final String DEFAULT_SERVER_NAME = mTag + "/" + mVer;
	private int DEFAULT_LISTEN_PORT = 24691;

	public static final int CLICASH_WORKING = 1;
	public static final int CLICASH_RUNNING = 0;
	public static final int CLICASH_STOP = -1;

	private CliCashEventListener mListener;

	private int status = CLICASH_RUNNING;


	private ServerSocket ssc;

	protected final void Logging(int priority, String message) {
		final String ObjectInstanceTag = utils.ClassNameStripper(getClass().getName());
		Log.println(priority, ObjectInstanceTag, message);
	}

	public void setCliCashEventListener(CliCashEventListener listener) {
		mListener = listener;
	}

	private void listenOnCliCashBeginListening() {
		if (mListener != null)
			mListener.onCliCashBeginListening(this);
	}

	protected final void listenOnCliCashCaughtError(Exception exception) {
		if (exception instanceof SocketException) {
			// Just socket exception ... ignore it for sane
			Logging(Log.INFO, "socket: " + String.valueOf(exception.getMessage()));
		} else {
			if (mListener != null)
				mListener.onCliCashCaughtError(this, exception);
			exception.printStackTrace();
		}
	}

	protected final void listenOnCliCashCaughtError(Exception exception, boolean enforce) {
		if (enforce && mListener != null) {
			mListener.onCliCashCaughtError(this, exception);
			exception.printStackTrace();
		} else {
			listenOnCliCashCaughtError(exception);
		}
	}

	private void listenOnCliCashDestoryed() {
		if (mListener != null)
			mListener.onCliCashDestoryed(this);
	}

	private void LowLevelGetRoute(Socket socket, String path, Map<String, String> header) {
		utils.CliResponse response = GetRoute(path, header);
		if (response == null) {
			try {
				StreamingGetRoute(socket.getInputStream(), socket.getOutputStream(), path, header);
				socket.close();
			} catch (IOException e) {
				listenOnCliCashCaughtError(e);
			}
		} else {
			try {
				startResponse(socket.getOutputStream(), response.mContent, response.mHeader, 200);
				socket.close();
			} catch (Exception e) {
				listenOnCliCashCaughtError(e);
			}
		}
	}

	private void LowLevelPostRoute(Socket socket, String path, String payload, Map<String, String> header) {
		utils.CliResponse response = PostRoute(path, payload, header);
		if (response == null) {
			try {
				StreamingPostRoute(socket.getInputStream(), socket.getOutputStream(), path, payload, header);
				socket.close();
			} catch (Exception e) {
				listenOnCliCashCaughtError(e);
			}
		} else {
			try {
				startResponse(socket.getOutputStream(), response.mContent, response.mHeader, 200);
				socket.close();
			} catch (Exception e) {
				listenOnCliCashCaughtError(e);
			}
		}
	}

	protected final void startResponse(OutputStream ostream, String preContent, Map<String, String> header, int responseCode) {
		utils.HTTPHeader headerObj = new utils.HTTPHeader(utils.HTTPHeader.HEADER_SERVER);
		headerObj.mPayload = preContent;
		headerObj.mHeader.putAll(header);
		headerObj.mResponseCode = responseCode;
		String response_payload = headerObj.acquirePayload();
		try {
			ostream.write(response_payload.getBytes());
			ostream.flush();
		} catch (IOException e) {
			if (e instanceof SocketException) {
				// Just socket exception ... ignore it for sane
			} else {
				listenOnCliCashCaughtError(e);
				e.printStackTrace();
			}
		}

	}

	protected utils.CliResponse GetRoute(String path, Map<String, String> header) {
		// return new utils.CliResponse(DEFAULT_RESPONSE);
		return null;
	}

	protected utils.CliResponse PostRoute(String path, String payload, Map<String, String> header) {
		// return new utils.CliResponse(DEFAULT_RESPONSE);
		return null;
	}

	protected void StreamingGetRoute(InputStream istream, OutputStream ostream, String path, Map<String, String> header) {

	}

	protected void StreamingPostRoute(InputStream istream, OutputStream ostream, String path, String payload, Map<String, String> header) {

	}

	private void proceedClient(Socket socket) {
		byte[] buf = new byte[2048];
		try {
			InputStream inputStream = socket.getInputStream();
			inputStream.read(buf);
			/*
			Bundle headerData = getHTTPHeader(buf);
			String method = headerData.getString("method");
			String path = headerData.getString("path");
			*/
			utils.HTTPHeader header = new utils.HTTPHeader(utils.HTTPHeader.HEADER_CLIENT, buf);
			if ("POST".equals(header.mMethod)) {
				String payload = header.mPayload == null ? "" : header.mPayload;
				String path = header.mPath;
				if (path != null)
					LowLevelPostRoute(socket, path, payload, header.mHeader);
			} else if ("GET".equals(header.mMethod)) {
				String path = header.mPath;
				if (path != null)
					LowLevelGetRoute(socket, path, header.mHeader);
			}
		} catch (IOException e) {
			listenOnCliCashCaughtError(e);
			e.printStackTrace();
		} finally {
			buf = null;
		}


	}

	private final Runnable cacheRunnable = new Runnable() {
		@Override
		public void run() {
			try {


				int localport = DEFAULT_LISTEN_PORT;
				while (localport < 65536) {
					try {
						ssc = new ServerSocket();	// May throw IOException with "too many open files"...
						ssc.bind(new InetSocketAddress(localport));
						DEFAULT_LISTEN_PORT = localport;
						status = CLICASH_WORKING;

						listenOnCliCashBeginListening();

						break;
					} catch (IOException e) {
						// May got a "Address in use"
						// BUG? : The IOException doesn't throw with native error "Address in use" and there will got a dead loop
						listenOnCliCashCaughtError(e, true);
						String err = e.getMessage();
						Logging(Log.ERROR, new StringBuilder()
								.append("Port ").append(String.valueOf(localport))
								.append(" is in use? error = \"")
								.append(err == null ? "N/A" : err).append("\"").toString());
						try {
							ssc.close();
						} catch (IOException e1) {
							Logging(Log.ERROR, "Cannot close previous socket, reason = " + String.valueOf(e1.getMessage()));
						}
						localport++;
					}
				}
				if (localport >= 65536) {
					throw new IllegalStateException("Sorry ... we tried all ports from "
							+ String.valueOf(DEFAULT_LISTEN_PORT) + " to 65535 and all get in use...");
				}

				// Change `mBaseURL` to a valid value
				mBaseURL = "http://localhost:" + String.valueOf(localport) + "/";

				while (true) {
					final Socket client = ssc.accept();
					new Thread(new Runnable() {
						@Override
						public void run() {
							proceedClient(client);
						}
					}).start();
				}

			} catch (IOException e) {
				if (e instanceof SocketException) {
					// may be socket were closed
					Logging(Log.INFO, "Closing time!");
					listenOnCliCashDestoryed();
				} else {
					listenOnCliCashCaughtError(e);
					e.printStackTrace();
				}
			}

		}
	};

	protected final void setDefaultPort(int newPort) {
		DEFAULT_LISTEN_PORT = newPort;
	}

	protected final int getCurrentPort() {
		return DEFAULT_LISTEN_PORT;
	}

	public void terminate() {
		try {
			ssc.close();
			status = CLICASH_STOP;
		} catch (Exception e) {
			if (e instanceof SocketException) {
				Logging(Log.INFO, "Commit closing");
			}
		}
	}

	protected void finalize() {
		terminate();
		status = CLICASH_STOP;
	}

	public int getCliCashStatus() {
		return status;
	}

	public void prepare() {
		// TODO: Implement this
		if (mListenerThread != null) {
			Logging(Log.WARN, "Already started...");
		} else {
			mListenerThread = new Thread(cacheRunnable);
			mListenerThread.setName(utils.ClassNameStripper(getClass().getName()));
			mListenerThread.start();
		}

	}

	public String getCacheBaseUrl(String original_url) {
		// TODO: Implement this
		if (mBaseURL == null)
			throw new IllegalStateException("CliCash not ready!");

		return mBaseURL;
	}

}

