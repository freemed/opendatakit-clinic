package org.odk.clinic.android.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.listeners.UploadFormListener;

import android.os.AsyncTask;
import android.util.Log;

public class UploadInstanceTask extends AsyncTask<String, String, ArrayList<String>> {
    private static String tag = "UploadFormTask";
    
    private static final int CONNECTION_TIMEOUT = 30000;
    
    protected UploadFormListener mStateListener;
    String mUrl;
    protected ClinicAdapter mClinicAdapter = new ClinicAdapter();
    
    public void setUploadServer(String newServer) {
        mUrl = newServer;
    }
    
    @Override
    protected ArrayList<String> doInBackground(String... params) {
        ArrayList<String> uploadedIntances = new ArrayList<String>();
        int instanceCount = params.length;
        for (int i = 0; i < instanceCount; i++) {

            // configure connection
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(httpParams, CONNECTION_TIMEOUT);
            HttpClientParams.setRedirecting(httpParams, false);

            // setup client
            DefaultHttpClient httpclient = new DefaultHttpClient(httpParams);
            HttpPost httppost = new HttpPost(mUrl);

            // get instance file
            File file = new File(params[i]);

            // find all files in parent directory
            File[] files = file.getParentFile().listFiles();
            System.out.println(file.getAbsolutePath());
            if (files == null) {
                Log.e(tag, "no files to upload in istance");
                continue;
            }

            // mime post
            MultipartEntity entity = new MultipartEntity();
            for (int j = 0; j < files.length; j++) {
                File f = files[j];
                FileBody fb;
                if (f.getName().endsWith(".xml")) {
                    fb = new FileBody(f, "text/xml");
                    entity.addPart("xml_submission_file", fb);
                    Log.i(tag, "added xml file " + f.getName());
                } else if (f.getName().endsWith(".jpg")) {
                    fb = new FileBody(f, "image/jpeg");
                    entity.addPart(f.getName(), fb);
                    Log.i(tag, "added image file " + f.getName());
                } else if (f.getName().endsWith(".3gpp")) {
                    fb = new FileBody(f, "audio/3gpp");
                    entity.addPart(f.getName(), fb);
                    Log.i(tag, "added audio file " + f.getName());
                } else if (f.getName().endsWith(".3gp")) {
                    fb = new FileBody(f, "video/3gpp");
                    entity.addPart(f.getName(), fb);
                    Log.i(tag, "added video file " + f.getName());
                } else if (f.getName().endsWith(".mp4")) {
                    fb = new FileBody(f, "video/mp4");
                    entity.addPart(f.getName(), fb);
                    Log.i(tag, "added video file " + f.getName());
                } else {
                    Log.w(tag, "unsupported file type, not adding file: " + f.getName());
                }
            }
            httppost.setEntity(entity);

            // prepare response and return uploaded
            HttpResponse response = null;
            try {
                response = httpclient.execute(httppost);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return uploadedIntances;
            } catch (IOException e) {
                e.printStackTrace();
                return uploadedIntances;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return uploadedIntances;
            }

            // check response.
            // TODO: This isn't handled correctly.
            String serverLocation = null;
            Header[] h = response.getHeaders("Location");
            if (h != null && h.length > 0) {
                serverLocation = h[0].getValue();
            } else {
                // something should be done here...
                Log.e(tag, "Location header was absent");
            }
            int responseCode = response.getStatusLine().getStatusCode();
            Log.e(tag, "Response code:" + responseCode);

            // verify that your response came from a known server
            if (serverLocation != null && mUrl.contains(serverLocation)) {
                uploadedIntances.add(params[i]);
            }
            uploadedIntances.add(params[i]);

        }

        return uploadedIntances;
    }

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
    protected void onPostExecute(ArrayList<String> result) {
    	System.out.println("Gathaiya ziko: " + result.size());
        synchronized (this) {
            if (mStateListener != null)
                mStateListener.uploadComplete(result);
        }
    }

    public void setUploadListener(UploadFormListener sl) {
        synchronized (this) {
            mStateListener = sl;
        }
    }
}
