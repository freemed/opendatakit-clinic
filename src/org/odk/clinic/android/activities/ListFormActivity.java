package org.odk.clinic.android.activities;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.odk.clinic.android.R;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.listeners.InstanceLoaderListener;
import org.odk.clinic.android.listeners.UploadFormListener;
import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.openmrs.Form;
import org.odk.clinic.android.openmrs.FormInstance;
import org.odk.clinic.android.tasks.InstanceLoaderTask;
import org.odk.clinic.android.tasks.UploadInstanceTask;
import org.odk.clinic.android.utilities.FileUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListFormActivity extends ListActivity implements InstanceLoaderListener, UploadFormListener {
    public static final String tag = "ListFormActivity";

    // Menu ID's
    private static final int MENU_DOWNLOAD = Menu.FIRST;
    
    // Request codes
    public static final int DOWNLOAD_FORM = 1;
    public static final int COLLECT_FORM = 2;
    
    private static final int PROGRESS_DIALOG = 1;
    private static final int UPLOAD_DIALOG = 2;
    
    private static final String SELECTED_FORM_ID_KEY = "selectedFormId";
    
    private Integer mPatientId;
    
    private ArrayAdapter<Form> mFormAdapter;
    private ArrayList<Form> mForms = new ArrayList<Form>();
    
    private Integer mSelectedFormId = null;
    
    private AlertDialog mAlertDialog;
    private ProgressDialog mProgressDialog;
    
    private InstanceLoaderTask mInstanceLoaderTask;
    private UploadInstanceTask mUploadFormTask;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_FORM_ID_KEY)) {
                mSelectedFormId = savedInstanceState.getInt(SELECTED_FORM_ID_KEY);
            }
        }
        
        setContentView(R.layout.form_list);
        
        if (!FileUtils.storageReady()) {
            showCustomToast(getString(R.string.error, R.string.storage_error));
            finish();
        }

        // TODO Check for invalid patient IDs
        String patientIdStr = getIntent().getStringExtra(Constants.KEY_PATIENT_ID);
        try {
        	mPatientId = Integer.valueOf(patientIdStr);
        }catch (NumberFormatException e) {
			mPatientId = null;
			//here the id is empty meaning its a new patient
		}
        
        setTitle(getString(R.string.app_name) + " > "
                + getString(R.string.forms));

        Object data = getLastNonConfigurationInstance();
        if (data instanceof InstanceLoaderTask) {
            mInstanceLoaderTask = (InstanceLoaderTask) data;
        } else if (data instanceof UploadInstanceTask) {
            mUploadFormTask = (UploadInstanceTask) data;
        }
    }
    
    private void getDownloadedForms() {

        ClinicAdapter ca = new ClinicAdapter();

        ca.open();
        Cursor c = ca.fetchAllForms();

        if (c != null && c.getCount() >= 0) {

            mForms.clear();

            int formIdIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORM_ID);
            int nameIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORM_NAME);
            int pathIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORM_PATH);

            Form f;
            if (c.getCount() > 0) {
                do {
                    if (!c.isNull(pathIndex)) {
                        f = new Form();
                        f.setFormId(c.getInt(formIdIndex));
                        f.setPath(c.getString(pathIndex));
                        f.setName(c.getString(nameIndex));
                        mForms.add(f);
                    }
                } while (c.moveToNext());
            }
        }

        refreshView();
        
        if (c != null)
            c.close();

        ca.close();
    }
    
    private Form getForm(Integer formId) {
        Form f = null;
        ClinicAdapter ca = new ClinicAdapter();

        ca.open();
        Cursor c = ca.fetchForm(formId);

        if (c != null && c.getCount() > 0) {
            int nameIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORM_NAME);
            int pathIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORM_PATH);

            f = new Form();
            f.setFormId(formId);
            f.setPath(c.getString(pathIndex));
            f.setName(c.getString(nameIndex));
        }
        
        if (c != null)
            c.close();
        ca.close();
        
        return f;
    }
    
    private FormInstance getFormInstance(Integer patientId, Integer formId) {
        FormInstance fi = null;
        ClinicAdapter ca = new ClinicAdapter();

        ca.open();
        Cursor c = ca.fetchFormInstance(patientId, formId);

        if (c != null && c.getCount() > 0) {
            int statusIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORMINSTANCE_STATUS);
            int pathIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORMINSTANCE_PATH);
            
            fi = new FormInstance();
            fi.setPatientId(patientId);
            fi.setFormId(formId);
            fi.setStatus(c.getString(statusIndex));
            fi.setPath(c.getString(pathIndex));
        }

        if (c != null) {
            c.close();
        }
        ca.close();

        return fi;
    }
    
    private void refreshView() {

        mFormAdapter = new ArrayAdapter<Form>(this, android.R.layout.simple_list_item_1,
                mForms);
        setListAdapter(mFormAdapter);

    }
    
    private void initializeFormInstance(Form form, Integer patientId) {
        FormInstance fi = null;
        
        // Create instance folder
        String time =
            new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                    .format(Calendar.getInstance().getTime());
        String instanceName = form.getFormId() + "_" + (patientId!=null?patientId:"new") + "_" + time;
        
        String path = FileUtils.INSTANCES_PATH + instanceName;
        if (FileUtils.createFolder(path)) {
            String instancePath = path + "/" + instanceName + ".xml";
            
            // Save form instance to db
            fi = new FormInstance();
            fi.setPatientId(patientId!=null?patientId:0);
            fi.setFormId(form.getFormId());
            fi.setPath(instancePath);
            fi.setStatus(ClinicAdapter.STATUS_INITIALIZED);
            
            ClinicAdapter ca = new ClinicAdapter();
            ca.open();
            ca.createFormInstance(fi);
            ca.close();
            
            // Start instance loader task
            mInstanceLoaderTask = new InstanceLoaderTask();
            mInstanceLoaderTask.setInstanceLoaderListener(this);
            mInstanceLoaderTask.execute(form.getPath(), instancePath, patientId!=null?patientId.toString():null);
            showDialog(PROGRESS_DIALOG);
        }
    }
    
    private void launchFormEntry(String formPath, String instancePath) {
        
        Intent i = new Intent("org.odk.collect.android.action.FormEntry");
        i.putExtra("formpath", formPath);
        i.putExtra("instancepath", instancePath);
        startActivityForResult(i, COLLECT_FORM);
    }
    
    private void uploadFormInstance(String instancePath) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        StringBuilder url = new StringBuilder(settings.getString(
                PreferencesActivity.KEY_SERVER,
                getString(R.string.default_server)));
        url.append(Constants.INSTANCE_UPLOAD_URL);
        url.append("?uname=");
        url.append(settings.getString(PreferencesActivity.KEY_USERNAME,
                getString(R.string.default_username)));
        url.append("&pw=");
        url.append(settings.getString(PreferencesActivity.KEY_PASSWORD,
                getString(R.string.default_password)));

        mUploadFormTask = new UploadInstanceTask();
        mUploadFormTask.setUploadListener(this);
        mUploadFormTask.setUploadServer(url.toString());
        mUploadFormTask.execute(instancePath);
        showDialog(UPLOAD_DIALOG);
    }
    
    @Override
    protected void onListItemClick(ListView listView, View view, int position,
            long id) {
        // Get selected form
        Form f = (Form) getListAdapter().getItem(position);
        String formPath = f.getPath();
        mSelectedFormId = f.getFormId();
        
        FormInstance fi = getFormInstance(mPatientId, f.getFormId());
        if (fi == null) {
            initializeFormInstance(f, mPatientId);
        } else {
            String instancePath = fi.getPath();
            launchFormEntry(formPath, instancePath);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_DOWNLOAD, 0, getString(R.string.download_forms))
                .setIcon(R.drawable.ic_menu_download);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DOWNLOAD:
            Intent id = new Intent(getApplicationContext(),
                    DownloadFormActivity.class);
            startActivityForResult(id, DOWNLOAD_FORM);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        
        if (requestCode == COLLECT_FORM) {
            // Remove form instance from db if user "discarded"
            if (mSelectedFormId != null) {
                boolean discarded = false;
                
                FormInstance fi = getFormInstance(mPatientId, mSelectedFormId);
                if (fi != null) {
                    File file = new File(fi.getPath());
                    discarded = !file.exists();
                }
                
                if (discarded) {
                    ClinicAdapter ca = new ClinicAdapter();
                    ca.open();
                    ca.deleteFormInstance(mPatientId, mSelectedFormId);
                    ca.close();
                    
                    mSelectedFormId = null;
                } else {
                    // Ask user if they want to upload the form
                    createUploadDialog();
                }
            }
        }
        
        if (resultCode == RESULT_CANCELED) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, intent);

    }

    private void createUploadDialog() {
        mAlertDialog = new AlertDialog.Builder(this).create();
        //mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);

        mAlertDialog.setTitle("Upload Form");
        mAlertDialog.setMessage("Do you want to upload the form?");

        DialogInterface.OnClickListener uploadDialogListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1: // yes
                        FormInstance fi = getFormInstance(mPatientId, mSelectedFormId);
                        if (fi != null)
                            uploadFormInstance(fi.getPath());
                        // don't break
                    case DialogInterface.BUTTON2: // no
                        dialog.dismiss();
                        mSelectedFormId = null;
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton("Upload", uploadDialogListener);
        mAlertDialog.setButton2("No", uploadDialogListener);
        mAlertDialog.show();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mInstanceLoaderTask.setInstanceLoaderListener(null);
                            mInstanceLoaderTask.cancel(true);
                        }
                    };
                mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
                mProgressDialog.setTitle("Loading Instance");
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel),
                    loadingButtonListener);
                return mProgressDialog;
                
            case UPLOAD_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener uploadButtonListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mUploadFormTask.setUploadListener(null);
                            mUploadFormTask.cancel(true);
                        }
                    };
                mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
                mProgressDialog.setTitle("Uploading Form");
                mProgressDialog.setMessage(getString(R.string.please_wait));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel), uploadButtonListener);
                return mProgressDialog;
        }
        return null;
    }

    private void dismissDialogs() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onPause() {
        dismissDialogs();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mUploadFormTask != null) {
            mUploadFormTask.setUploadListener(this);
        }
        if (mInstanceLoaderTask != null) {
            mInstanceLoaderTask.setInstanceLoaderListener(this);
        }
        
        getDownloadedForms();
        
        super.onResume();
    }

    @Override
    protected void onDestroy() {

        if (mInstanceLoaderTask != null) {
            mInstanceLoaderTask.setInstanceLoaderListener(null);
            if (mInstanceLoaderTask.getStatus() == AsyncTask.Status.FINISHED) {
                // Allow saving to finish
                mInstanceLoaderTask.cancel(false);
            }
        }
        if (mUploadFormTask != null) {
            mUploadFormTask.setUploadListener(null);
            if (mUploadFormTask.getStatus() == AsyncTask.Status.FINISHED) {
                mUploadFormTask.cancel(true);
            }
        }

        super.onDestroy();

    }
    
    @Override
    public void loadingComplete(String result) {
        dismissDialog(PROGRESS_DIALOG);
        
        if (result != null && mSelectedFormId != null) {
            Form f = getForm(mSelectedFormId);
            String formPath = f.getPath();
            String instancePath = result;
            launchFormEntry(formPath, instancePath);
        }
    }

    @Override
    public void progressUpdate(String message, int progress, int max) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void uploadComplete(ArrayList<String> result) {
        dismissDialog(UPLOAD_DIALOG);
        // TODO Toast message?
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mInstanceLoaderTask != null && mInstanceLoaderTask.getStatus() != AsyncTask.Status.FINISHED)
            return mInstanceLoaderTask;

        if (mUploadFormTask != null && mUploadFormTask.getStatus() != AsyncTask.Status.FINISHED)
            return mUploadFormTask;

        return null;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        if (mSelectedFormId != null)
            outState.putInt(SELECTED_FORM_ID_KEY, mSelectedFormId);
    }
    
    private void showCustomToast(String message) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.toast_view, null);

        // set the text in the view
        TextView tv = (TextView) view.findViewById(R.id.message);
        tv.setText(message);

        Toast t = new Toast(this);
        t.setView(view);
        t.setDuration(Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }
}
