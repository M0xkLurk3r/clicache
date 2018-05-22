package com.aiclk.android.clicache.universe;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by anthony on 4/27/18.
 */

public class utils {

	public static String ClassNameStripper(String original_class_name) {
		// Strip the class name into relative class name (package-relative)
		// Example: "android.util.Xml" ==> "Xml"
		if (original_class_name == null)
			throw new NullPointerException("ClassNameStripper: original class name should not be null!");

		int size = original_class_name.length() - 1;
		for (; size >= 0; size--) {
			if (original_class_name.charAt(size) == '.')
				return original_class_name.substring(size + 1, original_class_name.length());
		}
		return "";
	}

	public static class IOUtils {

		public static long CopyIToO(
				//
				//	public static long CopyIToO():	Copy InputStream to OutputStream
				//
				InputStream input,        // Input stream
				OutputStream output        // Output stream
				//	Return value: long:	data totally copied.
		) throws IOException {
			byte[] buffer = new byte[4096];
			long count = CopyIToO(buffer, input, output);
			buffer = null;
			return count;
		}

		public static long CopyIToO(
				//
				//	public static long CopyIToO():	Copy InputStream to OutputStream
				//
				byte[] buf,                // The buffer
				InputStream input,        // Input stream
				OutputStream output        // Output stream
				//	Return value: long:	data totally copied.
		) throws IOException {
			return CopyIToO(buf, input, output, 0, -1);
		}

		public static long CopyIToO(
				//
				//	public static long CopyIToO():	Copy InputStream to OutputStream
				//
				byte[] buf,                // The buffer
				InputStream input,        // Input stream
				OutputStream output,    // Output stream
				long off,                // From offset (0 for default)
				long len                // Copy length (-1 for infinity)
				//	Return value: long:	data totally copied.
		) throws IOException {
			byte[] buffer = buf;
			if (buf == null)
				throw new NullPointerException("buf cannot be null!");

			long count = 0;
			int counter = 0;

			long skip = input.skip(off);
			if (skip < off) {
				// The `off` offset exceed the InputStream's read limit
				return -1;
			}

			while ((counter = input.read(buffer)) != -1 && (len == -1 || count <= len)) {
				output.write(buffer, 0, counter);
				count += counter;
			}
			output.flush();
			return count;
		}

		public static long tee(
				byte[] buf,                    // The buffer
				InputStream input,            // Input Stream
				List<OutputStream> output    // Output Stream group
		) throws IOException {
			return tee(buf, input, output, 0, -1);
		}

		public static long tee(
				//
				//	public static long tee(): Read from InputStream and duplicate the data then write to multi OutputStream.
				//
				InputStream input,        // Input Stream
				List<OutputStream> output    // Output Stream group
		) throws IOException {
			byte[] buffer = new byte[4096];
			long result = tee(buffer, input, output);
			buffer = null;
			return result;
		}

		public static long tee(
				//
				//	public static long tee(): Read from InputStream and duplicate the data then write to multi OutputStream.
				//
				byte[] buf,                    // The buffer
				InputStream input,            // Input Stream
				List<OutputStream> output,    // Output Stream group
				long off,                    // From offset (0 for default)
				long len                    // Copy length (-1 for infinity)
		) throws IOException {
			byte[] buffer = buf;
			if (buf == null || input == null || output == null)
				throw new NullPointerException("arguments cannot be null!");
			long count = 0;
			int read = 0;

			long skip = input.skip(off);
			if (skip < off) {
				// The `off` offset exceed the InputStream's read limit
				// Fuck FileInputStream!!!!! The InputStream.skip() function were almost a pile of useless shit here!
				return -1;
			}

			while ((read = input.read(buffer)) != -1 && (len == -1 || count <= len)) {
				for (OutputStream ostream : output) {
					ostream.write(buffer, 0, read);
				}
				count += read;
			}
			for (OutputStream ostream : output) {
				ostream.flush();
			}
			return count;
		}
	}

	public static String Wget(
			//
			//	public static boolean Wget():	Download file
			//
			String url,		//	Request url
			String path,	//	Which way to store file
			String filename	//	File name (null to use default name)
			//	Return value: String:	Real downloaded file path
	) throws MalformedURLException {
		try {
			URL urlhandle = new URL(url);
			URLConnection conn = urlhandle.openConnection();

			String server_mime = conn.getHeaderField("Content-Type");
			server_mime = server_mime != null ? server_mime : "null";

			if (filename == null) {
				if (server_mime.equals("application/octet-stream")) {
					filename = conn.getHeaderField("Content-Disposition");
				} else if (server_mime.equals("text/html")) {
					filename = "index.html";
				}
				if (filename == null) {
					filename = "noname.dat";
				}
			}

			File file = new File(path + filename);
			FileOutputStream fos = new FileOutputStream(file);
			IOUtils.CopyIToO(conn.getInputStream(), fos);	// Copy data

			fos.flush();
			fos.close();

		} catch (IOException e) {
			return null;
		}
		return path + filename;
	}

	public static String byteMD5(
			//
			//	public static byteMD5(): Calculate md5 string from a buffer.
			//
			byte[] buf,	// The buffer
			int off,	// Where the buffer starts
			int len	// The count we reads
			//	Return value: String: The string contains the result md5 hash
	) {

		if (buf == null) {
			Log.e("byteMD5", "byteMD5 cannot proceed null!");
			return null;
		}

		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(buf, off, len);
			byte[] result = md.digest();

			StringBuilder strbuild = new StringBuilder();
			for (byte c : result) {
				strbuild.append(Integer.toHexString((c & 0xFF) | 0x100).substring(1,3));
			}
			return strbuild.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

	}

	public static String streamMD5(
			//
			//	public static byteMD5(): Calculate md5 string from a buffer.
			//
			InputStream istream, // The input stream
			int off,            // skip for `off` bytes
			int len                // proceed `len` bytes (pass -1 will read until stream gives EOF)
			//	Return value: String: The string contains the result md5 hash
	) {

		if (istream == null) {
			Log.e("byteMD5", "byteMD5 cannot proceed null!");
			return null;
		}

		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] result = new byte[1024];
			int read = 0;
			try {
				if (len == -1) {
					while ((read = istream.read(result)) > -1) {
						md.update(result, 0, read);
					}
				} else {
					while (len > 0) {
						if (len < result.length) {
							read = istream.read(result, 0, len);
						} else {
							read = istream.read(result);
						}
						md.update(result, 0, read);
						len = len - read;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			result = md.digest();	// Let previous buffer lose focus.

			return hexString(result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String hexString(
			//
			// private static String hexString(): Convert byte[] to String
			//
			// Example: (byte[])'\xBA\xAD\xEE\x12' => (String)"BAADEE12"
			//
			byte[] buf	// The buffer
			// Return value: String: The result string.
	) {
		if (buf == null) {
			return null;
		}

		StringBuilder strbuild = new StringBuilder();

		for (byte c : buf) {
			strbuild.append(Integer.toHexString((c & 0xFF) | 0x100).substring(1,3));
		}

		return strbuild.toString();
	}

	public static class CliResponse {
		public String mContent = "";
		public Map<String, String> mHeader = new TreeMap<>();
		public CliResponse() {

		}
		public CliResponse(String content) {
			setContent(content);
		}

		public void setContent(String content) {
			mContent = content;
		}

		public void setExtraHeader(Map<String, String> header) {
			mHeader.putAll(header);
		}

	}

	public static String getHeaderBanner(int responseCode) {
		switch (responseCode) {
			case 200:
				return "200 OK";
			case 204:
				return "204 No Content";
			case 206:
				return "206 Partial Content";
			case 301:
				return "301 Moved Permanently";
			case 302:
				return "302 Found";
			case 400:
			default:
				return "400 Bad Request";
			case 403:
				return "403 Forbidden";
			case 404:
				return "404 Not Found";
			case 416:
				return "416 Requested Range Not Satisfiable";
			case 500:
				return "500 Internal Server Error";
		}
	}

	public static class HTTPHeader implements java.io.Serializable {
		public static final int HEADER_SERVER = 0;
		public static final int HEADER_CLIENT = 1;

		private static final char whitespace = ' ';
		private static final String CRLF = "\r\n";
		private static final char colon = ':';

		public Map<String, String> mHeader = new TreeMap<>();
		public String mMethod = null;
		public String mPath = null;
		public String mProtoVer = "HTTP/1.1";
		public String mPayload = null;
		public int mResponseCode = 0;
		public int mHeaderType = HEADER_CLIENT;

		public HTTPHeader(int headerType) {
			mHeaderType = headerType;
		}

		public HTTPHeader(int headerType, String payload) throws InvalidObjectException {
			mHeaderType = headerType;
			offerPayload(payload);
		}

		public HTTPHeader(int headerType, byte[] buf) throws InvalidObjectException {
			mHeaderType = headerType;
			offerPayload(new String(buf));
		}

		private static String stripHeadWhiteSpace(String original) {
			int counter;
			for (counter = 0; counter < original.length(); counter++) {
				if (original.charAt(counter) != ' ')
					break;
			}
			return original.substring(counter, original.length());
		}

		private void serverHttpCommandProceed(String line)
				throws InvalidObjectException {
			if (line.startsWith("HTTP")) {
				String[] httpcmd = line.split(" ");
				if (httpcmd.length < 3) {
					throw new InvalidObjectException("bad header...");
				}
				mProtoVer = httpcmd[0];
				mResponseCode = Integer.valueOf(httpcmd[1]);

				// Save original string
				StringBuilder sb = new StringBuilder();
				for (int counter = 1; counter < httpcmd.length; counter++) {
					sb.append(httpcmd[counter]);
				}
			}
		}

		private void clientHttpCommandProceed(String line)
				throws InvalidObjectException {
			if (line.startsWith("GET")
					||	line.startsWith("POST")) {
				String[] httpcmd = line.split(" ");
				if (httpcmd.length < 2) {
					throw new InvalidObjectException("bad header...");
				}
				mMethod = httpcmd[0];
				mPath = httpcmd[1];
				if (httpcmd.length == 3) {
					mProtoVer = httpcmd[2];
				} else if (httpcmd.length == 2) {
					mProtoVer = "HTTP/1.0";
				}
			}
		}

		public void offerPayload(String buf) throws InvalidObjectException {

			if (buf == null || buf.length() <= 0)
				throw new InvalidObjectException("buf length cannot be zero!");

			String[] rawdata = buf.split("\r\n\r\n");

			if (rawdata[0] == null || rawdata[0].length() <= 0)
				throw new InvalidObjectException("buf length cannot be zero!");

			String[] header = rawdata[0].split("\r\n");

			if (header.length == 0)
				throw new InvalidObjectException("buf length cannot be zero!");

			TreeMap<String, String> result = new TreeMap<>();

			for (int counter = 0; counter < header.length; counter++) {
				String[] keyval = header[counter].split(":", 2);
				if (keyval.length == 2) {
					String key = keyval[0];
					String val = stripHeadWhiteSpace(keyval[1]);
					result.put(key, val);
				} else if (keyval.length == 1) {
					switch (mHeaderType) {
						case HEADER_CLIENT:
							clientHttpCommandProceed(header[counter]);
							break;
						case HEADER_SERVER:
							serverHttpCommandProceed(header[counter]);
					}
				}
			}
			mHeader = result;

			if (rawdata.length > 1)
				mPayload = rawdata[1];
		}

		public String acquirePayload() {
			switch (mHeaderType) {
				case HEADER_CLIENT:
				default:
					return clientAcquirePayload();
				case HEADER_SERVER:
					return serverAcquirePayload();
			}
		}

		private String clientAcquirePayload() {
			StringBuilder strbuilder = new StringBuilder()
					.append(mMethod).append(whitespace).append(mPath).append(whitespace).append(mProtoVer).append(CRLF);	// GET / HTTP/1.1
			strbuilder = appendHeaderMap(strbuilder);
			strbuilder.append(CRLF);
			if (mPayload != null)
				strbuilder.append(mPayload);
			return strbuilder.toString();
		}

		private String serverAcquirePayload() {

			StringBuilder strbuilder = new StringBuilder()
					.append(mProtoVer).append(whitespace).append(getHeaderBanner(mResponseCode)).append(CRLF);	// HTTP/1.1 200
			strbuilder = appendHeaderMap(strbuilder);
			strbuilder.append(CRLF);
			if (mPayload != null)
				strbuilder.append(mPayload);
			return strbuilder.toString();

		}

		private StringBuilder appendHeaderMap(StringBuilder builder) {
			if (mHeader == null && builder == null)
				return builder;
			for (Map.Entry<String, String> entry : mHeader.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();
				builder.append(key).append(colon).append(whitespace).append(val).append(CRLF);
			}
			return appendDefaultHeader(builder);
		}

		private StringBuilder appendDefaultHeader(StringBuilder builder) {
			if (! mHeader.containsKey("Server"))
				builder.append("Server").append(colon).append(whitespace).append(CliCash.DEFAULT_SERVER_NAME).append(CRLF);
			if (! mHeader.containsKey("Content-Type"))
				builder.append("Content-Type").append(colon).append(whitespace).append("text/plain").append(CRLF);
			if (! mHeader.containsKey("Content-Length"))
				builder.append("Content-Length").append(colon).append(whitespace).append(String.valueOf(mPayload.length())).append(CRLF);
			if (! mHeader.containsKey("Connection"))
				builder.append("Connection").append(colon).append(whitespace).append("close").append(CRLF);
			return builder;
		}
	}
}

