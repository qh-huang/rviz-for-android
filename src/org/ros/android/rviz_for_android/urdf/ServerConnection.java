/*
 * Copyright (c) 2012, Willow Garage, Inc.
 * All rights reserved.
 *
 * Willow Garage licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.ros.android.rviz_for_android.urdf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ros.android.rviz_for_android.MainActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Queries the Python server to download textures and mesh files or start and stop nodes. Any downloaded files are cached in app storage. Only one instance of this class may exist!
 * 
 * @author azimmerman
 * 
 */
public class ServerConnection {

	private String host;
	private Activity context;
	private Object lock = new Object();

	private ServerConnection.DownloadServerTask downloadTask;

	private static ServerConnection instance;

	private ProgressDialog mProgressDialog;

	/**
	 * Create the singleton instance using the specified hostname and context activity.
	 * 
	 * @param host
	 *            the hostname to connect to
	 * @param context
	 *            the main activity context, used to display dialogs and toast messages
	 * @return
	 */
	public static ServerConnection initialize(String host, Activity context) {
		if(instance == null) {
			// Initialize the new instance
			instance = new ServerConnection(host, context);
		}
		return instance;
	}

	/**
	 * @return the current instance (null if initialize hasn't been called)
	 */
	public static ServerConnection getInstance() {
		return instance;
	}

	private ServerConnection(String host, final Activity context) {
		Log.i("Downloader", "Host IP is " + host);
		this.context = context;
		this.host = host;

		context.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mProgressDialog = new ProgressDialog(MainActivity.getAppContext());
				mProgressDialog.setMessage("Download progress");
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setMax(100);
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			}
		});
	}

	/**
	 * Clear the local cache of downloaded models and textures.
	 * 
	 * @return the number of files deleted
	 */
	public int clearCache() {
		int deletedFiles = 0;
		synchronized(lock) {
			for(String file : context.fileList()) {
				Log.i("Downloader", "Cleared file " + file);
				context.deleteFile(file);
				deletedFiles++;
			}
			Log.d("Downloader", "Cleared cache");
		}
		return deletedFiles;
	}

	public Activity getContext() {
		return context;
	}

	/**
	 * @param filename
	 *            the complete package and mesh filename string
	 * @return the 'package://' prefix to prepend to image names when fetching them from the server
	 */
	public String getPrefix(String filename) {
		if(filename != null)
			return filename.substring(0, filename.lastIndexOf("/") + 1);
		else
			return null;
	}

	/**
	 * @param filename
	 *            the complete package and mesh filename string
	 * @return the sanitized prefix to prepend to image names when loading them from local storage
	 */
	public String getSanitizedPrefix(String filename) {
		if(filename != null) {
			return sanitizeFilename(getPrefix(filename));
		} else
			return null;
	}

	/**
	 * Check to see if a file exists in the local cache
	 * 
	 * @param filename
	 *            The name of the file to search for
	 * @return true if the file is present in local storage, false otherwise
	 */
	public boolean fileExists(String filename) {
		if(filename != null) {
			// Arrays.binarySearch doesn't work
			for(String s : context.fileList()) {
				if(s.equals(filename))
					return true;
			}
			return false;
		} else
			throw new IllegalArgumentException("Filename can't be null!");
	}

	private String sanitizeFilename(String filename) {
		if(filename.contains("/")) {
			filename = filename.substring(filename.indexOf("//") + 2).replace("/", "-");
		}
		return filename;
	}

	/**
	 * Download a file on the current thread. This will block until the file has been downloaded. No progress dialog is shown, so the calling thread will appear to hang. This is best called from an AsyncTask.
	 * 
	 * @param path
	 *            the URL (http or package) to download
	 * @return the name of the file accessible through the context-private file space
	 */
	public String getFile(final String path) {
		synchronized(lock) {
			final String filename = sanitizeFilename(path);
			if(fileExists(filename))
				return filename;
			try {
				if(path.startsWith("http://"))
					return queryServer(new URL(path), filename);
				else if(path.toLowerCase().startsWith("package://")) {
					return queryServer(new URL(host + "/PKG" + path.substring(9)), filename);
				} else {
					Log.e("Downloader", "Unexpected scheme: " + path);
					return null;
				}
			} catch(MalformedURLException e) {
				Log.e("Downloader", "Path and URL are malformed: " + host + "    " + path);
				e.printStackTrace();
			}
			Log.e("Downloader", "Couldn't download file!");
			return null;
		}
	}

	/**
	 * Download a file in a background thread using an AsyncTask. This will show a download progress dialog during download.
	 * 
	 * @param path
	 *            the URL (http or package) to download
	 * @return the name of the file accessible through the context-private file space
	 */
	public String getFileBackground(final String path) {
		synchronized(lock) {
			if(fileExists(path))
				return sanitizeFilename(path);
			final String filename = sanitizeFilename(path);
			try {
				if(path.startsWith("http://"))
					return queryServer(new URL(path), filename);
				else if(path.toLowerCase().startsWith("package://")) {
					downloadTask = new DownloadServerTask();
					downloadTask.execute(host + "/PKG" + path.substring(9), filename, path.substring(path.lastIndexOf('/') + 1, path.length()));
					return downloadTask.get(10, TimeUnit.SECONDS);
				} else {
					Log.e("Downloader", "Unexpected scheme: " + path);
					return null;
				}
			} catch(MalformedURLException e) {
				Log.e("Downloader", "Path and URL are malformed: " + host + "    " + path);
				e.printStackTrace();
			} catch(InterruptedException e) {
				Log.e("Downloader", "Interrupted exception");
				e.printStackTrace();
			} catch(ExecutionException e) {
				Log.e("Downloader", "Execution exception");
				e.printStackTrace();
			} catch(TimeoutException e) {
				Log.e("Downloader", "Timed out!!");
				e.printStackTrace();
			}
			Log.e("Downloader", "Couldn't download file!");
			return null;
		}
	}

	/**
	 * Start a node on the host computer with the specified ID handle. This will execute rosrun with the provided arguments.
	 * 
	 * @param ID
	 *            the ID of the process to be started. This ID can be used to stop the node by calling stopNode. Calling startNode with the ID of an existing node will stop the existing node and replace it with the requested node
	 * @param arguments
	 *            the arguments to pass to rosrun
	 */
	public void startNode(int ID, String... arguments) {
		StringBuilder pathBuilder = new StringBuilder().append(host).append("/ROSRUN/").append(ID);

		for(String arg : arguments)
			pathBuilder.append("/").append(arg);

		ConnectServerTask cst = new ConnectServerTask();
		cst.execute(pathBuilder.toString());
	}

	/**
	 * Stop a previously launched node
	 * 
	 * @param ID
	 *            the ID of the node to stop. If no node was started with this ID, no errors will be generated.
	 */
	public void stopNode(int ID) {
		StringBuilder pathBuilder = new StringBuilder(host).append("/ROSSTOP/").append(ID);
		ConnectServerTask cst = new ConnectServerTask();
		cst.execute(pathBuilder.toString());
	}

	private String queryServer(URL url, String filename) {
		Log.i("Downloader", "Fetching file with URL " + url.toString() + " to save to " + filename);
		HttpURLConnection conn = null;
		int retries = 0;
		while(retries < 10) {
			try {
				Log.d("Downloader", "Connecting...");
				conn = (HttpURLConnection) url.openConnection();

				int bytesRead = 0;
				BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
				BufferedOutputStream bout = new BufferedOutputStream(context.openFileOutput(filename, Context.MODE_PRIVATE), 1024);

				if(conn.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
					int toRead = conn.getContentLength();
					byte[] data = new byte[1024];
					int read = 0;
					while((read = in.read(data, 0, 1024)) >= 0) {
						bout.write(data, 0, read);
						bytesRead += read;
					}
					Log.d("Downloader", "Should have " + toRead + " bytes");
					Log.d("Downloader", "Wrote " + bytesRead + " bytes to sdcard");
					if(bytesRead < toRead)
						Log.e("Downloader", "Download incomplete!");
					bout.close();
					return filename;
				} else {
					Log.e("Downloader", "404'd!");
					retries++;
				}
			} catch(IOException e) {
				Log.e("Downloader", "IO Error!");
				e.printStackTrace();
				retries++;
			} finally {
				conn.disconnect();
			}
		}
		return null;
	}

	/**
	 * Connect to a server URL and immediately disconnect
	 * 
	 * @author azimmerman
	 */
	private class ConnectServerTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			HttpURLConnection conn = connectURL(params[0]);

			try {
				conn.getResponseCode();
			} catch(IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			conn.disconnect();
			return null;
		}

		private HttpURLConnection connectURL(String address) {
			Log.d("Downloader", "Connecting to URL " + address);
			URL url = null;
			try {
				url = new URL(address);
			} catch(MalformedURLException e) {
				e.printStackTrace();
			}

			try {
				return (HttpURLConnection) url.openConnection();
			} catch(IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Connect to a URL and attempt to download a file
	 * 
	 * @author azimmerman
	 */
	private class DownloadServerTask extends AsyncTask<String, Integer, String> {

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			mProgressDialog.dismiss();
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if(!context.isFinishing())
				mProgressDialog.show();
			else
				Log.e("Downloader", "Context is not ready to display a dialog");
			mProgressDialog.setProgress(0);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			mProgressDialog.setProgress(values[0]);
		}

		@Override
		protected String doInBackground(String... params) {
			Log.d("Downloader", "Beginning execution...");
			mProgressDialog.setMessage("Downloading " + params[2]);
			URL url = null;
			try {
				url = new URL(params[0]);
			} catch(MalformedURLException e1) {
				e1.printStackTrace();
				return null;
			}
			String filename = params[1];

			int retries = 0;
			while(retries < 5) {
				try {
					Log.d("Downloader", "Connecting...");
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();

					int bytesRead = 0;
					BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
					BufferedOutputStream bout = new BufferedOutputStream(context.openFileOutput(filename, Context.MODE_PRIVATE), 1024);

					if(conn.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
						int toRead = conn.getContentLength();
						byte[] data = new byte[1024];
						int read = 0;
						while((read = in.read(data, 0, 1024)) >= 0) {
							bout.write(data, 0, read);
							bytesRead += read;
							publishProgress((int) ((100.0 * bytesRead) / toRead));
						}
						Log.d("Downloader", "Should have " + toRead + " bytes");
						Log.d("Downloader", "Wrote " + bytesRead + " bytes to sdcard");
						if(bytesRead < toRead)
							Log.e("Downloader", "Download incomplete!");
						bout.close();
						publishProgress(100);
						return filename;
					} else {
						Log.e("Downloader", "404'd!");
						retries++;
					}
				} catch(IOException e) {
					Log.e("Downloader", "IO Error!");
					retries++;
					e.printStackTrace();
				}
			}
			return null;
		}

	}
}
