package org.fruct.oss.getssupplement.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.graphhopper.util.DistanceCalcEarth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.toRadians;

public class Utils {
	private static final Logger log = LoggerFactory.getLogger(Utils.class);
	private static final int MAX_RECURSION = 10;

	public static boolean isTrueString(String str) {
		str = str.toLowerCase();
		return "true".equals(str) || "1".equals(str) || "yes".equals(str);
	}

	public static boolean isNullOrEmpty(String string) {
		return string == null || string.isEmpty();
	}

	// Copied and modified from graphhopper's DistanceCalcEarth.java
	private static DistanceCalcEarth distanceCalc = new DistanceCalcEarth();
	public static double calcDist(double r_lat_deg, double r_lon_deg,
						   double a_lat_deg, double a_lon_deg,
						   double b_lat_deg, double b_lon_deg, int[] type, double[] outCoord) {
		type[0] = 0;
		double shrink_factor = cos((toRadians(a_lat_deg) + toRadians(b_lat_deg)) / 2);
		double a_lat = a_lat_deg;
		double a_lon = a_lon_deg * shrink_factor;

		double b_lat = b_lat_deg;
		double b_lon = b_lon_deg * shrink_factor;

		double r_lat = r_lat_deg;
		double r_lon = r_lon_deg * shrink_factor;

		double delta_lon = b_lon - a_lon;
		double delta_lat = b_lat - a_lat;

		if (delta_lat == 0) {
			// special case: horizontal edge
			outCoord[0] = a_lat_deg;
			outCoord[1] = r_lon_deg;
			return distanceCalc.calcDist(a_lat_deg, r_lon_deg, r_lat_deg, r_lon_deg);
		}
		if (delta_lon == 0) {
			// special case: vertical edge
			outCoord[0] = r_lat_deg;
			outCoord[1] = a_lon_deg;
			return distanceCalc.calcDist(r_lat_deg, a_lon_deg, r_lat_deg, r_lon_deg);
		}

		double norm = delta_lon * delta_lon + delta_lat * delta_lat;
		double factor = ((r_lon - a_lon) * delta_lon + (r_lat - a_lat) * delta_lat) / norm;

		if (factor > 1) {
			type[0] = 2;
			factor = 1;
		} else if (factor < 0) {
			type[0] = 1;
			factor = 0;
		}

		// x,y is projection of r onto segment a-b
		double c_lon = a_lon + factor * delta_lon;
		double c_lat = a_lat + factor * delta_lat;

		outCoord[0] = c_lat;
		outCoord[1] = c_lon / shrink_factor;

		return distanceCalc.calcDist(c_lat, c_lon / shrink_factor, r_lat_deg, r_lon_deg);
	}

	private static final char[] hexDigits = "0123456789abcdef".toCharArray();
	public static String toHex(byte[] arr) {
		final char[] str = new char[arr.length * 2];

		for (int i = 0; i < arr.length; i++) {
			final int v = arr[i] & 0xff;
			str[2 * i] = hexDigits[v >>> 4];
			str[2 * i + 1] = hexDigits[v & 0x0f];
		}

		return new String(str);
	}

	public static String hashString(String input) {
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return String.valueOf(input.hashCode());
		}
		md5.update(input.getBytes());
		byte[] hash = md5.digest();
		return toHex(hash);
	}

	public static String hashStream(InputStream in, String hash) throws IOException {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance(hash);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		int bsize = 4096;
		byte[] buffer = new byte[bsize];
		int length;

		while ((length = in.read(buffer, 0, bsize)) > 0) {
			md5.update(buffer, 0, length);
		}

		return toHex(md5.digest());
	}

	public static String hashStream(InputStream in) throws IOException {
		return hashStream(in, "MD5");
	}

	/*public static List<Turn> findTurns(List<GeoPoint> points) {
		// Two point line can't has turns
		if (points.size() < 3)
			return Collections.emptyList();

		double lastBearing = points.get(0).bearingTo(points.get(1));

		ArrayList<Turn> turns = new ArrayList<Turn>();
		for (int i = 1; i < points.size() - 1; i++) {
			double bearing = points.get(i).bearingTo(points.get(i + 1));
			double relBearing = Utils.normalizeAngle(bearing - lastBearing);

			double diff = Math.abs(relBearing);
			int turnDirection = relBearing > 0 ? -1 : 1;

			int turnSharpness;
			if (diff < 11) {
				continue;
			} else if (diff < 40) {
				turnSharpness = 1;
			} else if (diff < 103) {
				turnSharpness = 2;
			} else {
				turnSharpness = 3;
			}

			lastBearing = bearing;

			turns.add(new Turn(points.get(i), turnSharpness, turnDirection));
		}
		return turns;
	}*/

	public static double normalizeAngle(double degree) {
		return (StrictMath.IEEEremainder(degree, 360));
	}

	public static double normalizeAngleRad(double radian) {
		return (StrictMath.IEEEremainder(radian, 2 * Math.PI));
	}


	public static String inputStreamToString(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
		return readerToString(reader);
	}

	public static String readerToString(Reader reader) throws IOException {
		StringBuilder builder = new StringBuilder();
		int bufferSize = 4096;
		char[] buf = new char[bufferSize];

		int readed;
		while ((readed = reader.read(buf)) > 0) {
			builder.append(buf, 0, readed);
		}

		return builder.toString();
	}

	public static String downloadUrl(String urlString, String postQuery) throws IOException {
		HttpURLConnection conn = null;
		InputStream responseStream = null;

		try {
			URL url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);
			conn.setRequestMethod(postQuery == null ? "GET" : "POST");
			conn.setDoInput(true);
			conn.setDoOutput(postQuery != null);
			conn.setRequestProperty("User-Agent", "RoadSigns/0.2 (http://oss.fruct.org/projects/roadsigns/)");
			conn.setRequestProperty("Content-Type", "Content-Type: text/xml;charset=utf-8");

			if (postQuery != null) {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
				writer.write(postQuery);
				writer.flush();
				writer.close();
			}

			log.trace("Request url {} data {}", urlString, postQuery);
			conn.connect();

			int responseCode = conn.getResponseCode();
			responseStream = conn.getInputStream();
			String response = Utils.inputStreamToString(responseStream);

			log.trace("Response code {}, response {}", responseCode, response);

			return response;
		} finally {
			if (conn != null)
				conn.disconnect();

			if (responseStream != null)
				responseStream.close();
		}
	}

	public static HttpURLConnection getConnection(String urlStr) throws IOException {
		return getConnection(urlStr, MAX_RECURSION);
	}

	private static HttpURLConnection getConnection(String urlStr, final int recursionDepth) throws IOException {
		log.info("Downloading {}", urlStr);
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000);
		conn.setConnectTimeout(10000);

		conn.setRequestMethod("GET");
		conn.setDoInput(true);

		conn.connect();
		int code = conn.getResponseCode();
		log.info("Code {}", code);

		// TODO: not tested
		if (code != HttpURLConnection.HTTP_ACCEPTED) {
			if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP) {
				if (recursionDepth == 0)
					throw new IOException("Too many redirects");

				String newLocation = conn.getHeaderField("Location");
				log.info("Redirecting to {}", newLocation);

				conn.disconnect();
				return getConnection(newLocation, recursionDepth - 1);
			}
		}

		return conn;
	}

	public static void deleteDir(File dir) {
		if (!dir.exists() && !dir.isDirectory())
			return;

		File[] listFiles = dir.listFiles();
		for (File file : listFiles) {
			if (!file.isDirectory()) {
				file.delete();
			}
		}

		dir.delete();
	}

	public static void silentClose(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ignored) {
			}
		}
	}

	// http://stackoverflow.com/questions/4946295/android-expand-collapse-animation
	public static void expand(final View v) {
		v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		final int targetHeight = v.getMeasuredHeight();

		v.getLayoutParams().height = 0;
		v.setVisibility(View.VISIBLE);
		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				v.getLayoutParams().height = interpolatedTime == 1
						? ViewGroup.LayoutParams.WRAP_CONTENT
						: (int)(targetHeight * interpolatedTime);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
		v.startAnimation(a);
	}

	public static void collapse(final View v) {
		final int initialHeight = v.getMeasuredHeight();

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				if(interpolatedTime == 1){
					v.setVisibility(View.GONE);
				}else{
					v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
					v.requestLayout();
				}
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
		v.startAnimation(a);
	}

	public static String[] getSecondaryDirs() {
		List<String> ret = new ArrayList<String>();
		String secondaryStorageString = System.getenv("SECONDARY_STORAGE");
		if (secondaryStorageString != null && !secondaryStorageString.trim().isEmpty()) {
			String[] dirs = secondaryStorageString.split(":");

			for (String dir : dirs) {
				File file = new File(dir);
				if (file.isDirectory() && file.canWrite()) {
					ret.add(dir);
				}
			}

			if (ret.isEmpty())
				return null;
			else
				return ret.toArray(new String[ret.size()]);

		} else {
			return null;
		}
	}

	public static String[] getExternalDirs(Context context) {
		List<String> paths = new ArrayList<String>();
		String[] secondaryDirs = getSecondaryDirs();
		if (secondaryDirs != null) {
			for (String secondaryDir : secondaryDirs) {
				paths.add(secondaryDir + "/roadsigns");
			}
		}

		File externalStorageDir = Environment.getExternalStorageDirectory();
		if (externalStorageDir != null && externalStorageDir.isDirectory()) {
			paths.add(Environment.getExternalStorageDirectory().getPath() + "/roadsigns");
		}

		return paths.toArray(new String[paths.size()]);
	}



	public static class StorageDirDesc {
		public final int nameRes;
		public final String path;

		public StorageDirDesc(int nameRes, String path) {
			this.nameRes = nameRes;
			this.path = path;
		}
	}


}
