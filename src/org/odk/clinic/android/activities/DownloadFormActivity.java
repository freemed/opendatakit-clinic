package org.odk.clinic.android.activities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.odk.clinic.android.R;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.listeners.DownloadListener;
import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.openmrs.Form;
import org.odk.clinic.android.tasks.DownloadFormListTask;
import org.odk.clinic.android.tasks.DownloadFormTask;
import org.odk.clinic.android.tasks.DownloadTask;
import org.odk.clinic.android.utilities.FileUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadFormActivity extends Activity implements DownloadListener {

    private final static int FORMLIST_DIALOG = 1;
    private final static int FORMLIST_PROGRESS_DIALOG = 2;
    private final static int FORMS_PROGRESS_DIALOG = 3;
    
    private AlertDialog mFormListDialog;
    private ProgressDialog mProgressDialog;
    
    private DownloadTask mDownloadTask;
    
    private ArrayList<Form> mForms = new ArrayList<Form>();
    private HashSet<Integer> mSelectedFormIds = new HashSet<Integer>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.app_name) + " > "
                + getString(R.string.download_forms));

        if (!FileUtils.storageReady()) {
            showCustomToast(getString(R.string.error, R.string.storage_error));
            setResult(RESULT_CANCELED);
            finish();
        }
        
        // get the task if we've changed orientations. If it's null, open up the
        // form selection dialog
        mDownloadTask = (DownloadTask) getLastNonConfigurationInstance();
        if (mDownloadTask == null) {
            getForms();
            showDialog(FORMLIST_DIALOG);
        }
    }
    
    private void downloadFormList()
    {
        if (mDownloadTask != null)
            return;
        
        // setup dialog and upload task
        showDialog(FORMLIST_PROGRESS_DIALOG);

        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        StringBuilder url = new StringBuilder(settings.getString(
                PreferencesActivity.KEY_SERVER,
                getString(R.string.default_server)));
        url.append(Constants.FORMLIST_DOWNLOAD_URL);
        url.append("&uname=");
        url.append(settings.getString(
                PreferencesActivity.KEY_USERNAME,
                getString(R.string.default_username)));
        url.append("&pw=");
        url.append(settings.getString(
                PreferencesActivity.KEY_PASSWORD,
                getString(R.string.default_password)));

        mDownloadTask = new DownloadFormListTask();
        mDownloadTask
                .setDownloadListener(DownloadFormActivity.this);
        mDownloadTask.execute(url.toString());
    }
    
    private void downloadForms()
    {
        if (mDownloadTask != null)
            return;
        
        // setup dialog and upload task
        showDialog(FORMS_PROGRESS_DIALOG);

        SharedPreferences settings = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());

        StringBuilder url = new StringBuilder(settings.getString(
                PreferencesActivity.KEY_SERVER,
                getString(R.string.default_server)));
        url.append(Constants.FORM_DOWNLOAD_URL);
        url.append("&uname=");
        url.append(settings.getString(
                PreferencesActivity.KEY_USERNAME,
                getString(R.string.default_username)));
        url.append("&pw=");
        url.append(settings.getString(
                PreferencesActivity.KEY_PASSWORD,
                getString(R.string.default_password)));

        mDownloadTask = new DownloadFormTask();
        mDownloadTask.setDownloadListener(DownloadFormActivity.this);
        
        Log.i("DFA", "size " + mSelectedFormIds.size());
        String[] args = new String[mSelectedFormIds.size() + 1];
        args[0] = url.toString();
        Iterator<Integer> iter = mSelectedFormIds.iterator();
        for (int i = 1; i < args.length && iter.hasNext(); i++) {
            args[i] = iter.next().toString();
        }
        
        mDownloadTask.execute(args);
    }
    
    private void getForms() {

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
                    f = new Form();
                    f.setFormId(c.getInt(formIdIndex));
                    f.setPath(c.getString(pathIndex));
                    f.setName(c.getString(nameIndex));
                    mForms.add(f);
                } while (c.moveToNext());
            }
        }

        if (c != null)
            c.close();

        ca.close();

    }
    
    private class FormListDialogListener implements
            DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener,
            DialogInterface.OnCancelListener {
        
        @Override
        public void onClick(DialogInterface dialog, int which) {
            
            if (which < 0) {
                // Remove dialog to get a fresh instance next time we call showDialog()
                removeDialog(FORMLIST_DIALOG);
                mFormListDialog = null;
                
                switch (which) {
                    case DialogInterface.BUTTON_NEUTRAL: // refresh
                        downloadFormList();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE: // cancel
                        setResult(RESULT_CANCELED);
                        finish();
                        break;
                    case DialogInterface.BUTTON_POSITIVE: // download
                        downloadForms();
                        break;
                }
            }
        }
        
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            
            if (which >= 0) {
                Form f = mForms.get(which);
                Integer formId = f.getFormId();
                
                if (isChecked && !mSelectedFormIds.contains(formId)) {
                    mSelectedFormIds.add(formId);
                } else if (!isChecked && mSelectedFormIds.contains(formId)) {
                    mSelectedFormIds.remove(formId);
                }
            }
        }
        
        @Override
        public void onCancel(DialogInterface dialog) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private AlertDialog createFormListDialog() {
        
        FormListDialogListener listener = new FormListDialogListener();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_forms));

        if (!mForms.isEmpty()) {
            
            CharSequence[] items = new CharSequence[mForms.size()];
            boolean[] checkedItems = new boolean[mForms.size()];
            for (int i = 0; i < mForms.size(); i++) {
                Form f = mForms.get(i);
                items[i] = f.getName();
                if (f.getPath() != null) {
                    mSelectedFormIds.add(f.getFormId());
                    checkedItems[i] = true;
                }
            }
            builder.setMultiChoiceItems(items, checkedItems, listener);
            builder.setPositiveButton(getString(R.string.download), listener);
        } else {
            builder.setMessage(getString(R.string.no_form));
        }
        builder.setNeutralButton(getString(R.string.refresh), listener);
        builder.setNegativeButton(getString(R.string.cancel), listener);
        builder.setOnCancelListener(listener);

        return builder.create();
    }
    
    private ProgressDialog createDownloadDialog() {
        
        ProgressDialog dialog = new ProgressDialog(this);
        DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mDownloadTask.setDownloadListener(null);
                setResult(RESULT_CANCELED);
                finish();
            }
        };
        dialog.setTitle(getString(R.string.connecting_server));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setButton(getString(R.string.cancel_download),
                loadingButtonListener);
        
        return dialog;
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        
        if (id == FORMLIST_DIALOG) {
            mFormListDialog = createFormListDialog();
            return mFormListDialog;
        } else if (id == FORMLIST_PROGRESS_DIALOG || id == FORMS_PROGRESS_DIALOG) {
            mProgressDialog = createDownloadDialog();
            return mProgressDialog;
        }

        return null;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        
        if (id == FORMLIST_PROGRESS_DIALOG || id == FORMS_PROGRESS_DIALOG) {
            ProgressDialog progress = (ProgressDialog) dialog;
            progress.setTitle(getString(R.string.connecting_server));
            progress.setProgress(0);
        }
    }

    public void downloadComplete(String result) {

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        
        if (result != null) {
            showCustomToast(getString(R.string.error, result));
            showDialog(FORMLIST_DIALOG);
        } else if (mDownloadTask instanceof DownloadFormListTask) {
            getForms();
            showDialog(FORMLIST_DIALOG);
            
        } else {
            setResult(RESULT_OK);
            finish();
        }
        
        mDownloadTask = null;
    }

    @Override
    public void progressUpdate(String message, int progress, int max) {
        mProgressDialog.setMax(max);
        mProgressDialog.setProgress(progress);
        mProgressDialog.setTitle(getString(R.string.downloading, message));
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mDownloadTask;
    }

    @Override
    protected void onDestroy() {
        if (mDownloadTask != null) {
            mDownloadTask.setDownloadListener(null);
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        if (mDownloadTask != null) {
            mDownloadTask.setDownloadListener(this);
        }
        
        if (mFormListDialog != null && !mFormListDialog.isShowing()) {
            mFormListDialog.show();
        }
        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (mFormListDialog != null && mFormListDialog.isShowing()) {
            mFormListDialog.dismiss();
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
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
