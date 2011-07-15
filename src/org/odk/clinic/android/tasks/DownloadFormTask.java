package org.odk.clinic.android.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.odk.clinic.android.utilities.FileUtils;

import android.util.Log;

public class DownloadFormTask extends DownloadTask {

    @Override
    protected String doInBackground(String... values) {

        String baseUrl = values[0];
        int count = values.length - 1;
        
        for (int i = 0; i < values.length; i++)
            Log.i("XXXX", "value: " + values[i]);

        if (count > 0) {
            try {
                // Ensure directory exists
                FileUtils.createFolder(FileUtils.FORMS_PATH);
                
                // Open db for editing
                mPatientDbAdapter.open();

                for (int i = 1; i < values.length; i++) {
                    try {
                        String formId = values[i];
                        publishProgress("form " + formId, Integer.valueOf(i).toString(), Integer.valueOf(count).toString());
                        
                        StringBuilder url = new StringBuilder(baseUrl);
                        url.append("&formId=");
                        url.append(formId);
                        
                        URL u = new URL(url.toString());
                        HttpURLConnection c = (HttpURLConnection) u.openConnection();
                        InputStream is = c.getInputStream();
                        
                        String path = FileUtils.FORMS_PATH + formId + ".xml";
                        
                        File f = new File(path);
                        OutputStream os = new FileOutputStream(f);
                        byte buf[] = new byte[1024];
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            os.write(buf, 0, len);
                        }
                        os.flush();
                        os.close();
                        is.close();
                        
                        mPatientDbAdapter.updateFormPath(Integer.valueOf(formId), path);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                mPatientDbAdapter.close();
            } catch (Exception e) {
                e.printStackTrace();
                return e.getLocalizedMessage();
            }
        }

        return null;
    }
}
