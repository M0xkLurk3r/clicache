package com.aiclk.android.clicache.videocache;

import android.Manifest;
import android.content.Context;
import android.support.v4.content.PermissionChecker;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.aiclk.android.clicache.universe.CliCash;
import com.aiclk.android.clicache.universe.utils;
import com.aiclk.android.clicache.videocache.listener.VideoCacheEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by anthony on 4/28/18.
 */

public class VideoCache extends CliCash {

	private static final String cmdName = "iclicashreq";
	private static final Map<String, String> emptyMap = new TreeMap<>();
	private static final String _404 = "<h4>404 Not Found</h4>";
	private static final String _400 = "<h4>400 Bad Request</h4>";
	private static final String _403 = "<h4>403 You need provides a valid command</h4>";

	private String CLICACHE_DIR = null;

	private VideoCacheEventListener mListener = null;

	private void checkAndroidPerm(Context context) {
		if (PermissionChecker.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED
		||	PermissionChecker.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
			throw new IllegalStateException("Need READ/WRITE permission to sdcard! give up.");
		}
	}

	private void setup(Context context) {
		File clicache_dir = context.getExternalCacheDir();

		if (clicache_dir == null)
			throw new IllegalStateException("Get a null reference on accessing external storage even we're permitted to access external storage!");

		CLICACHE_DIR = clicache_dir.getAbsolutePath() + "/clicache/";
	}

	public VideoCache(Context context) {
		checkAndroidPerm(context);
		setup(context);
		setDefaultPort(49382);
	}

	public void setVideoCacheEventListener(VideoCacheEventListener listener) {
		mListener = listener;
	}

	private void listenOnHTTPError(Exception e) {
		if (mListener != null)
			mListener.onHTTPError(this, e);
	}

	private void listenOnIllegalCommandError(String desc) {
		if (mListener != null)
			mListener.onIllegalCommandError(this, desc);
	}

	private List<File> getCacheFile(String urlmd5) throws IOException {
		/*
		 * Every time call this function will create a new file under corresponding directory
		 * unless we found the latest file we'd created was 0 bytes length
		 */
		File targetPath = new File(CLICACHE_DIR + "/" + urlmd5);
		if (! targetPath.isDirectory()) {
			if (targetPath.isFile()) {
				if (! targetPath.delete())
					throw new IOException("Cannot delete file \"" + targetPath.getAbsolutePath() + "\"!");
			}
			if (! targetPath.mkdirs()) {
				throw new IOException("Cannot make dir at \"" + targetPath.getAbsolutePath() +"\"!");
			}
		}

		File[] currentFile = targetPath.listFiles();
		TreeMap<Integer, File> fileMap = new TreeMap<>();
		int biggestfilename = 0;

		if (currentFile == null) {
			throw new IOException("List dir at \"" + targetPath.getAbsolutePath() + "\" got a null array!");
		}

		for (File file : currentFile) {
			if ((!file.isFile()) || !file.canRead() || !file.canWrite()) {
				continue;
			}
			if (file.length() <= 0) {
				if (!file.delete()) {
					Logging(Log.ERROR, "ERROR: Cannot delete file \"" + file.getAbsolutePath() + "\"");
				}
				continue;
			}
			try {
				int current_filename = Integer.valueOf(file.getName());
				biggestfilename = current_filename > biggestfilename ? current_filename : biggestfilename;
				fileMap.put(current_filename, file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Robust design
		if (! fileMap.keySet().contains(Integer.valueOf(1)) && fileMap.size() > 0) {
			Logging(Log.ERROR, "ERROR: No video header in corresponding directory!");
			Logging(Log.ERROR, "Will delete ALL FILES and re-download");
			for (Map.Entry<Integer, File> entry : fileMap.entrySet()) {
				File posFile = entry.getValue();
				if (posFile.isFile()) {
					posFile.delete();
				}
			}
			fileMap.clear();
			biggestfilename = 0;
		}

		List<File> result = new ArrayList<>(fileMap.values());

		// Create a new file for this routine's cache operation.

		String cachefileName = targetPath.getAbsolutePath() + "/" + String.valueOf(++biggestfilename);

		File newcachefile = new File(cachefileName);
		if (! newcachefile.isFile()) {
			// Probably this gives `true' would be a big mistake...
			if (! newcachefile.createNewFile()) {
				Logging(Log.ERROR, "ERROR: Couldn't create new file for write!");
				Logging(Log.ERROR, "ERROR: The following operation may causes deadly error!");
			} else {
				result.add(newcachefile);
			}
		} else {
			result.add(newcachefile);
		}

		return result;
	}

	public long getFileTotalLength(List<File> fileList) {
		long result = 0;
		for (File file : fileList) {
			if (file.isFile() || file.canRead()) {
				result += file.length();
			}
		}
		return result;
	}

	private long RawFullTransmit(byte[] buf, URLConnection conn, File targetFile, OutputStream ostream, long rangeFrom, long rangeTo)
			throws IOException {

		if (buf == null || conn == null || targetFile == null || ostream == null)
			throw new NullPointerException("Arguments cannot be null!");

		FileOutputStream fostream = new FileOutputStream(targetFile);
		InputStream httpistream = conn.getInputStream();
		List<OutputStream> ostreamGroup = Arrays.asList(new OutputStream[]{ostream, fostream});

		long copyCount = utils.IOUtils.tee(buf, httpistream, ostreamGroup, rangeFrom, rangeTo - rangeFrom);
		httpistream.close();
		ostream.flush();
		fostream.flush();
		fostream.close();
		return copyCount;
	}

	private void FullTransmit(
			//
			//	private void FullTransmit(): Perform full transmit to a file / to any stream
			//
			byte[] buf,                // The sharing buffer
			URLConnection conn,    // The activated URLConnection object
			File targetFile,        // The created, *empty* file
			OutputStream ostream,    // The output stream
			long rangeFrom,            // The left offset of range
			long rangeTo            // The right offset of range
			//	Return value: void
	) throws IOException {

		if (conn == null || targetFile == null || ostream == null || buf == null)
			throw new NullPointerException("Arguments cannot be null!");

		int netlength = conn.getContentLength();

		Map<String, String> resp_header = new TreeMap<>();
		resp_header.put("Content-Type", conn.getContentType());
		resp_header.put("Content-Length", String.valueOf(netlength));

		if (rangeFrom == 0 && rangeTo == -1)
			startResponse(ostream, "", resp_header, 200);
		else if (rangeFrom >= netlength) {
			startResponse(ostream, "", resp_header, 416);
			return;
		} else {
			resp_header.put("Accept-Ranges", "bytes");
			resp_header.put("Content-Range", new StringBuilder()
					.append("bytes ")
					.append(String.valueOf(rangeFrom)).append("-").append(String.valueOf(rangeTo == -1 ? netlength : rangeTo))
					.append("/").append(String.valueOf(netlength)).toString());
			startResponse(ostream, "", resp_header, 206);
		}

		RawFullTransmit(buf, conn, targetFile, ostream, rangeFrom, rangeTo);
	}

	private void HalfTransmit(
			//
			//	private void HalfTransmit(): Perform half-transmit to a file / to any stream
			//	Call this if we already had some stashed local file
			//
			byte[] buf,            // The sharing buffer
			long localLength,        // The local cache length (already downloaded)
			URLConnection conn,    // The activated URLConnection object
			List<File> fileGroup,    // The *created*, local file group with a *empty* file
			OutputStream ostream,    // The output stream
			long rangeFrom,            // The left offset of range
			long rangeTo            // The right offset of range
			//	Return value: void
	) throws IOException {

		if (conn == null || fileGroup == null || ostream == null || buf == null)
			throw new NullPointerException("Arguments cannot be null!");

		int netlength = conn.getContentLength();

		long actual_rangeTo = rangeTo == -1 ? (localLength + netlength) - 1 : rangeTo;

		Map<String, String> resp_header = new TreeMap<>();
		resp_header.put("Content-Type", conn.getContentType());
		resp_header.put("Content-Length", String.valueOf(netlength + localLength));

		if (rangeFrom == 0 && rangeTo == -1)
			startResponse(ostream, "", resp_header, 200);
		else if (rangeFrom >= (localLength + netlength)) {
			startResponse(ostream, "", resp_header, 416);
			return;
		} else {
			resp_header.put("Accept-Ranges", "bytes");
			resp_header.put("Content-Range", new StringBuilder()
					.append("bytes ")
					.append(String.valueOf(rangeFrom)).append("-").append(String.valueOf(actual_rangeTo))
					.append("/").append(String.valueOf(netlength + localLength)).toString());
			startResponse(ostream, "", resp_header, 206);
		}

	/*	long localTransCount = */RawFullLocalResources(buf, fileGroup, ostream, rangeFrom, localLength);
//		RawFullTransmit(buf, conn, fileGroup.get(fileGroup.size() - 1), ostream, rangeFrom + localTransCount, actual_rangeTo);
		RawFullTransmit(buf, conn, fileGroup.get(fileGroup.size() - 1), ostream, 0, netlength);
	}

	private void FullLocalResources(
			//
			//	private void FullLocalResources(): Only provides local caches to any stream
			//	Call this if we had the target resources completely cached in local
			//
			byte[] buf,            // The sharing buffer
			List<File> fileGroup,    // The activated URLConnection object
			OutputStream ostream,    // The output stream
			long rangeFrom,            // The left offset of range
			long rangeTo            // The right offset of range
			//	Return value: void
	) throws IOException {

		if (fileGroup == null || ostream == null || buf == null)
			throw new NullPointerException("Arguments cannot be null!");

		long fileTotalLength = getFileTotalLength(fileGroup);

		long actual_rangeTo = rangeTo == -1 ? fileTotalLength - 1 : rangeTo;

		Map<String, String> resp_header = new TreeMap<>();
		resp_header.put("Content-Type", "video/mp4");
		resp_header.put("Content-Length", String.valueOf(fileTotalLength));

		if (rangeFrom == 0 && rangeTo == -1)
			startResponse(ostream, "", resp_header, 200);
		else if (rangeFrom >= fileTotalLength) {
			startResponse(ostream, "", resp_header, 416);
			return;
		} else {
			resp_header.put("Accept-Ranges", "bytes");
			resp_header.put("Content-Range", new StringBuilder()
					.append("bytes ")
					.append(String.valueOf(rangeFrom)).append("-")
					.append(String.valueOf(actual_rangeTo))
					.append("/").append(String.valueOf(fileTotalLength)).toString());
			startResponse(ostream, "", resp_header, 206);
		}


		RawFullLocalResources(buf, fileGroup, ostream, rangeFrom, actual_rangeTo);
	}

	private long RawFullLocalResources(byte[] buf, List<File> fileGroup, OutputStream ostream, long rangeFrom, long rangeTo) throws IOException {

		if (buf == null || fileGroup == null || ostream == null)
			throw new NullPointerException("Arguments cannot be null!");

		long _rangeFrom = rangeFrom;
		long copyLen = rangeTo - rangeFrom;
		long copyCount = copyLen;

		for (File file : fileGroup) {

			if (file.length() == 0)
				continue;

			if (file.length() < _rangeFrom) {
				_rangeFrom -= file.length();
				continue;	// Try next file, this "part" file were skipped by client's choice
			}

			InputStream fistream = new FileInputStream(file);
			long proceed = utils.IOUtils.CopyIToO(buf, fistream, ostream, _rangeFrom, copyLen);
			if (proceed != -1) {
				_rangeFrom = 0;
				copyLen -= proceed;
			} else {
				_rangeFrom -= file.length();
			}
			ostream.flush();
			fistream.close();
		}
		return (copyCount - copyLen);
	}

	private Pair<Long, Long> proceedRange(String clientRange) {

		long rangeTo = -1;
		long rangeFrom = 0;

		try {
			if (clientRange != null) {
				clientRange = clientRange.replace("bytes=", "");
				String[] range = clientRange.split("-");
				switch (range.length) {
					case 2:
						rangeTo = Long.valueOf(range[1]);
					case 1:
						rangeFrom = Long.valueOf(range[0]);
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Pair.create(rangeFrom, rangeTo);
	}

	@Override
	protected void StreamingGetRoute(InputStream istream, OutputStream ostream, String path, Map<String, String> header) {
		final byte[] buf = new byte[2048];
		final String defaultRoute = '/' + cmdName + '?';
		HttpURLConnection conn = null;
		try {
			if (path.startsWith(defaultRoute)) {

				long rangeFrom;
				long rangeTo;

				Pair<Long, Long> rangepack = proceedRange(header.get("Range"));
				rangeFrom = rangepack.first;
				rangeTo = rangepack.second;

				// Get range from client request

				byte[] urlbyte = Base64.decode(path.replace(defaultRoute, ""), Base64.DEFAULT);
				String url = new String(urlbyte);

				List<File> cachefile = getCacheFile(utils.byteMD5(urlbyte, 0, urlbyte.length));
				long fileLength = getFileTotalLength(cachefile);

				conn = (HttpURLConnection) new URL(url).openConnection();
				if (fileLength > 0) {
					conn.setRequestProperty("Range", "bytes=" + String.valueOf(fileLength) + "-");
				}
				conn.connect();

				switch (conn.getResponseCode()) {
					case 200:
						// TODO: No local cache, download the file, passed to the application then build the cache.
						FullTransmit(buf, conn, cachefile.get(cachefile.size() - 1), ostream, rangeFrom, rangeTo);
						break;

					case 206:
						// TODO: Has un-finished local cache, download the least cache and pass them to the application.
						HalfTransmit(buf, fileLength, conn, cachefile, ostream, rangeFrom, rangeTo);
						break;

					case 416:
						// TODO: Has finished local cache, do not proceed any network operation.
						FullLocalResources(buf, cachefile, ostream, rangeFrom, rangeTo);
						break;

					case 404:
						throw new FileNotFoundException(path);
				}
				// TODO: Try download from the point that we cut

			} else {
				listenOnIllegalCommandError(path);
				startResponse(ostream, _403, emptyMap, 403);
			}

		} catch (Exception e) {
			if (e instanceof MalformedURLException) {
				listenOnHTTPError(e);
				startResponse(ostream, _400, emptyMap, 400);
			} else if (e instanceof FileNotFoundException) {
				listenOnHTTPError(e);
				startResponse(ostream, _404, emptyMap, 404);
			} else if (e instanceof SocketException) {
				Logging(Log.INFO, "socket: " + String.valueOf(e.getMessage()));
			} else {
				listenOnCliCashCaughtError(e);
				startResponse(ostream, "", emptyMap, 500);
				e.printStackTrace();
			}
		} finally {
			try {
				if (conn != null) {
					conn.getInputStream().close();
				}
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	@Override
	protected void StreamingPostRoute(InputStream istream, OutputStream ostream, String path, String payload, Map<String, String> header) {
		StreamingGetRoute(istream, ostream, path, header);
	}

	@Override
	public String getCacheBaseUrl(String original_url) {

		try {
			if (original_url == null)
				throw new NullPointerException("original_url cannot be null!");

			String origBaseURL = super.getCacheBaseUrl(null);
			String origURLBase64 = Base64.encodeToString(original_url.getBytes(), Base64.URL_SAFE);
			return new StringBuilder()
					.append(origBaseURL)
					.append(cmdName).append('?').append(origURLBase64)
					.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return original_url;
		}
	}
}

