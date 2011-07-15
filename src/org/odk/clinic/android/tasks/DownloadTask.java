package org.odk.clinic.android.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.listeners.DownloadListener;
import org.odk.clinic.android.openmrs.Constants;

import android.os.AsyncTask;

import com.jcraft.jzlib.ZInputStream;

public abstract class DownloadTask extends
		AsyncTask<String, String, String> {

	protected DownloadListener mStateListener;
	protected ClinicAdapter mPatientDbAdapter = new ClinicAdapter();

	@Override
	protected void onProgressUpdate(String... values) {
		synchronized (this) {
			if (mStateListener != null) {
				// update progress and total
				mStateListener.progressUpdate(values[0], new Integer(values[1])
						.intValue(), new Integer(values[2]).intValue());
			}
		}

	}

	@Override
	protected void onPostExecute(String result) {
		synchronized (this) {
			if (mStateListener != null)
				mStateListener.downloadComplete(result);
		}
	}

	public void setDownloadListener(DownloadListener sl) {
		synchronized (this) {
			mStateListener = sl;
		}
	}

	// url, username, password, serializer, locale, action, cohort
	protected DataInputStream connectToServer(String url, String username,
			String password, int action, String serializer, String locale,
			int cohort) throws Exception {

		// compose url
		URL u = null;
		u = new URL(url);

		// setup http url connection
		HttpURLConnection c = null;
		c = (HttpURLConnection) u.openConnection();
		c.setDoOutput(true);
		c.setRequestMethod("POST");
		c.addRequestProperty("Content-type", "application/octet-stream");
		// write auth details to connection
		DataOutputStream dos = null;
		dos = new DataOutputStream(c.getOutputStream());
		dos.writeUTF(username); // username
		dos.writeUTF(password); // password

		dos.writeUTF(serializer); // serializer
		dos.writeUTF(locale); // locale
		dos.writeByte(Integer.valueOf(action).byteValue());

		if (action == Constants.ACTION_DOWNLOAD_PATIENTS && cohort > 0) {
			dos.writeInt(cohort);
		}

		dos.flush();
		dos.close();

		// read connection status
		DataInputStream zdis = null;
		DataInputStream dis = new DataInputStream(c.getInputStream());
		ZInputStream zis = new ZInputStream(dis);
		zdis = new DataInputStream(zis);

		int status = zdis.readByte();

		if (status == Constants.STATUS_FAILURE) {
			zdis = null;
			throw new IOException("Connection failed. Please try again.");
		} else if (status == Constants.STATUS_ACCESS_DENIED) {
			zdis = null;
			throw new IOException(
					"Access denied. Check your username and password.");
		} else {
			assert (status == Constants.STATUS_SUCCESS); // success
			return zdis;
		}
	}
}

