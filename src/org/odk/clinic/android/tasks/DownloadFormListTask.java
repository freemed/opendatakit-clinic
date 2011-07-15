package org.odk.clinic.android.tasks;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.odk.clinic.android.openmrs.Form;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DownloadFormListTask extends DownloadTask {

    @Override
    protected String doInBackground(String... values) {
        
        String url = values[0];

        try {
            URL u = new URL(url);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            InputStream is = c.getInputStream();
            
            Document doc = null;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(is);
            
            if (doc != null) {
                // open db and clean entries
                mPatientDbAdapter.open();
                mPatientDbAdapter.deleteAllForms();
    
                // download forms to file, and insert reference to db
                insertForms(doc);
    
                // close db
                mPatientDbAdapter.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }

        return null;
    }
    
    private void insertForms(Document doc) throws Exception {
        
        NodeList formElements = doc.getElementsByTagName("xform");
        int count = formElements.getLength();

        for (int i = 0; i < count; i++) {
            Element n = (Element)formElements.item(i);

            String formName = n.getElementsByTagName("name").item(0).getChildNodes().item(0).getNodeValue();
            String formId = n.getElementsByTagName("id").item(0).getChildNodes().item(0).getNodeValue();

            Form f = new Form();
            f.setName(formName);
            f.setFormId(Integer.valueOf(formId));
            
            mPatientDbAdapter.createForm(f);
            
            publishProgress("forms", Integer.valueOf(i).toString(), Integer
                    .valueOf(count).toString());
        }
    }

}
