package org.odk.clinic.android.activities;

import java.util.ArrayList;

import org.odk.clinic.android.R;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.listeners.DownloadListener;
import org.odk.clinic.android.openmrs.Cohort;
import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.tasks.DownloadCohortTask;
import org.odk.clinic.android.tasks.DownloadPatientTask;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

// TODO Merge this activity into FindPatientActivity

public class DownloadPatientActivity extends Activity implements
        DownloadListener {

    private final static int COHORT_DIALOG = 1;
    private final static int COHORTS_PROGRESS_DIALOG = 2;
    private final static int PATIENTS_PROGRESS_DIALOG = 3;
    
    private AlertDialog mCohortDialog;
    private ProgressDialog mProgressDialog;
    
    private DownloadTask mDownloadTask;
    
    private ArrayList<Cohort> mCohorts = new ArrayList<Cohort>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.app_name) + " > "
                + getString(R.string.download_patients));

        if (!FileUtils.storageReady()) {
            showCustomToast(getString(R.string.error, R.string.storage_error));
            setResult(RESULT_CANCELED);
            finish();
        }
        
        // get the task if we've changed orientations. If it's null, open up the
        // cohort selection dialog
        mDownloadTask = (DownloadTask) getLastNonConfigurationInstance();
        if (mDownloadTask == null) {
            getCohorts();
            showDialog(COHORT_DIALOG);
        }
    }
    
    private void downloadCohorts()
    {
        if (mDownloadTask != null)
            return;
        
        // setup dialog and upload task
        showDialog(COHORTS_PROGRESS_DIALOG);

        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        String url = settings.getString(PreferencesActivity.KEY_SERVER,
                getString(R.string.default_server))
                + Constants.USER_DOWNLOAD_URL;
        String username = settings.getString(
                PreferencesActivity.KEY_USERNAME,
                getString(R.string.default_username));
        String password = settings.getString(
                PreferencesActivity.KEY_PASSWORD,
                getString(R.string.default_password));

        mDownloadTask = new DownloadCohortTask();
        mDownloadTask
                .setDownloadListener(DownloadPatientActivity.this);
        mDownloadTask.execute(url, username, password);
    }
    
    private void downloadPatients()
    {   
        if (mDownloadTask != null)
            return;
        
        // setup dialog and upload task
        showDialog(PATIENTS_PROGRESS_DIALOG);

        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        String url = settings.getString(PreferencesActivity.KEY_SERVER,
                getString(R.string.default_server))
                + Constants.USER_DOWNLOAD_URL;
        String username = settings.getString(PreferencesActivity.KEY_USERNAME,
                getString(R.string.default_username));
        String password = settings.getString(PreferencesActivity.KEY_PASSWORD,
                getString(R.string.default_password));
        int cohortId = settings.getInt(PreferencesActivity.KEY_COHORT, -1);

        mDownloadTask = new DownloadPatientTask();
        mDownloadTask.setDownloadListener(DownloadPatientActivity.this);
        mDownloadTask.execute(url, username, password, Integer
                .toString(cohortId));
    }
    
    private void getCohorts() {

        ClinicAdapter ca = new ClinicAdapter();

        ca.open();
        Cursor c = ca.fetchAllCohorts();

        if (c != null && c.getCount() >= 0) {

            mCohorts.clear();

            int cohortIdIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_COHORT_ID);
            int nameIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_COHORT_NAME);

            Cohort cohort;
            if (c.getCount() > 0) {
                do {
                    cohort = new Cohort();
                    cohort.setCohortId(c.getInt(cohortIdIndex));
                    cohort.setName(c.getString(nameIndex));
                    mCohorts.add(cohort);
                } while (c.moveToNext());
            }
        }

        if (c != null)
            c.close();

        ca.close();

    }
    
    private class CohortDialogListener implements
            DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which >= 0) {
                Cohort c = mCohorts.get(which);
                
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(PreferencesActivity.KEY_COHORT, c.getCohortId().intValue());
                editor.commit();
            } else {
                // Remove dialog to get a fresh instance next time we call showDialog()
                removeDialog(COHORT_DIALOG);
                mCohortDialog = null;
                
                switch (which) {
                    case DialogInterface.BUTTON_NEUTRAL: // refresh
                        downloadCohorts();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE: // cancel
                        setResult(RESULT_CANCELED);
                        finish();
                        break;
                    case DialogInterface.BUTTON_POSITIVE: // download
                        downloadPatients();
                        break;
                }
            }
        }
        
        @Override
        public void onCancel(DialogInterface dialog) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private AlertDialog createCohortDialog() {
        
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        int cohortId = settings.getInt(PreferencesActivity.KEY_COHORT, -1);

        CohortDialogListener listener = new CohortDialogListener();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_cohort));

        if (!mCohorts.isEmpty()) {
            
            int selectedCohortIndex = -1;
            String[] cohortNames = new String[mCohorts.size()];
            for (int i = 0; i < mCohorts.size(); i++) {
                Cohort c = mCohorts.get(i);
                cohortNames[i] = c.getName();
                if (cohortId == c.getCohortId()) {
                    selectedCohortIndex = i;
                }
            }
            builder.setSingleChoiceItems(cohortNames, selectedCohortIndex, listener);
            builder.setPositiveButton(getString(R.string.download), listener);
        } else {
            builder.setMessage(getString(R.string.no_cohort));
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
        
        if (id == COHORT_DIALOG) {
            mCohortDialog = createCohortDialog();
            return mCohortDialog;
        } else if (id == COHORTS_PROGRESS_DIALOG || id == PATIENTS_PROGRESS_DIALOG) {
            mProgressDialog = createDownloadDialog();
            return mProgressDialog;
        }

        return null;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        
        if (id == COHORTS_PROGRESS_DIALOG || id == PATIENTS_PROGRESS_DIALOG) {
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
            showDialog(COHORT_DIALOG);
        } else if (mDownloadTask instanceof DownloadCohortTask) {
            getCohorts();
            showDialog(COHORT_DIALOG);
            
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
        
        if (mCohortDialog != null && !mCohortDialog.isShowing()) {
            mCohortDialog.show();
        }
        if (mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (mCohortDialog != null && mCohortDialog.isShowing()) {
            mCohortDialog.dismiss();
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