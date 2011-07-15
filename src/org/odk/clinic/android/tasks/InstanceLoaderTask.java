package org.odk.clinic.android.tasks;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.transport.payload.ByteArrayPayload;
import org.javarosa.model.xform.XFormSerializingVisitor;
import org.javarosa.xform.parse.XFormParseException;
import org.javarosa.xform.util.XFormUtils;
import org.odk.clinic.android.listeners.InstanceLoaderListener;

import android.os.AsyncTask;
import android.util.Log;

public class InstanceLoaderTask extends AsyncTask<String, String, String> {
    private final static String tag = "InstanceLoaderTask";

    protected InstanceLoaderListener mStateListener;
    
    @Override
    protected String doInBackground(String... path) {

        FormDef fd = null;
        FileInputStream fis = null;

        String formPath = path[0];
        String instancePath = path[1];
        String patientIdStr = path[2];
        Integer patientId = null;
        try {
        	patientId = Integer.valueOf(patientIdStr);
        }catch (NumberFormatException e) {
			// TODO: handle exception
		}

        // Load form
        try {
            Log.i(tag, "Attempting to load from: " + formPath);
            fis = new FileInputStream(formPath);
            // FIXME parsing crashes on TestForm
            fd = XFormUtils.getFormFromInputStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XFormParseException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (fd != null) {
            // TODO Some handler stuff here?
            fd.initialize(true);
            
            org.javarosa.core.model.instance.FormInstance datamodel = fd.getInstance();
            
            if (patientId != null)
            	injectPatientId(datamodel, patientId);
            
            // Save form instance
            ByteArrayPayload payload;
            try {
                // assume no binary data inside the model.
                XFormSerializingVisitor serializer = new XFormSerializingVisitor();
                payload = (ByteArrayPayload) serializer.createSerializedPayload(datamodel);

                // write out xml
                exportXmlFile(payload, instancePath);

            } catch (IOException e) {
                Log.e(tag, "Error creating serialized payload");
                e.printStackTrace();
                return null;
            }
        }
        
        return instancePath;
    }

    private boolean injectPatientId(org.javarosa.core.model.instance.FormInstance datamodel, Integer patientId) {
        TreeElement root = datamodel.getRoot();
        if (root != null && root.getName().equalsIgnoreCase("form")) {
            Vector<TreeElement> children = root.getChildrenWithName("patient");
            if (!children.isEmpty()) {
                TreeElement patientElement = children.firstElement();
                children = patientElement.getChildrenWithName("patient.patient_id");
                if (!children.isEmpty()) {
                    TreeElement patientIdElement = children.firstElement();
                    patientIdElement.setAnswer(new IntegerData(patientId));
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean exportXmlFile(ByteArrayPayload payload, String path) {

        // create data stream
        InputStream is = payload.getPayloadStream();
        int len = (int) payload.getLength();

        // read from data stream
        byte[] data = new byte[len];
        try {
            int read = is.read(data, 0, len);
            if (read > 0) {
                // write xml file
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(path));
                    bw.write(new String(data, "UTF-8"));
                    bw.flush();
                    bw.close();
                    return true;

                } catch (IOException e) {
                    Log.e(tag, "Error writing XML file");
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (IOException e) {
            Log.e(tag, "Error reading from payload data stream");
            e.printStackTrace();
            return false;
        }

        return false;

    }
    
    @Override
    protected void onProgressUpdate(String... values) {

    }

    @Override
    protected void onPostExecute(String result) {
        synchronized (this) {
            if (mStateListener != null)
                mStateListener.loadingComplete(result);
        }
    }

    public void setInstanceLoaderListener(InstanceLoaderListener sl) {
        synchronized (this) {
            mStateListener = sl;
        }
    }
}
