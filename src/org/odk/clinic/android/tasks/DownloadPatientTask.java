package org.odk.clinic.android.tasks;

import java.io.DataInputStream;
import java.util.Date;
import java.util.Locale;

import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.openmrs.Observation;
import org.odk.clinic.android.openmrs.Patient;

public class DownloadPatientTask extends DownloadTask {

	public static final String KEY_ERROR = "error";
	public static final String KEY_PATIENTS = "patients";
	public static final String KEY_OBSERVATIONS = "observations";
	
	@Override
	protected String doInBackground(String... values) {

		String url = values[0];
		String username = values[1];
		String password = values[2];
		int cohort = Integer.valueOf(values[3]).intValue();

		int action = Constants.ACTION_DOWNLOAD_PATIENTS;
		String serializer = Constants.DEFAULT_PATIENT_SERIALIZER;
		String locale = Locale.getDefault().getLanguage();

		try {
			DataInputStream zdis = connectToServer(url, username, password,
					action, serializer, locale, cohort);
			if (zdis != null) {
				// open db and clean entries
				mPatientDbAdapter.open();
				mPatientDbAdapter.deleteAllPatients();
				mPatientDbAdapter.deleteAllObservations();

				// download and insert patients and obs
				insertPatients(zdis);
				insertObservations(zdis);

				// close db and stream
				mPatientDbAdapter.close();
				zdis.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return e.getLocalizedMessage();
		}

		return null;
	}

	private void insertPatients(DataInputStream zdis) throws Exception {

		int c = zdis.readInt();

		// List<Patient> patients = new ArrayList<Patient>(c);
		for (int i = 1; i < c + 1; i++) {
			Patient p = new Patient();
			if (zdis.readBoolean()) {
				p.setPatientId(zdis.readInt());
			}
			if (zdis.readBoolean()) {
				zdis.readUTF(); // ignore prefix
			}
			if (zdis.readBoolean()) {
				p.setFamilyName(zdis.readUTF());
			}
			if (zdis.readBoolean()) {
				p.setMiddleName(zdis.readUTF());
			}
			if (zdis.readBoolean()) {
				p.setGivenName(zdis.readUTF());
			}
			if (zdis.readBoolean()) {
				p.setGender(zdis.readUTF());
			}
			if (zdis.readBoolean()) {
				p.setBirthDate(new Date(zdis.readLong()));
			}
			if (zdis.readBoolean()) {
				p.setIdentifier(zdis.readUTF());
			}

			zdis.readBoolean(); // ignore new patient

			mPatientDbAdapter.createPatient(p);

			publishProgress("patients", Integer.valueOf(i).toString(), Integer
					.valueOf(c * 2).toString());
		}

	}

	private void insertObservations(DataInputStream zdis) throws Exception {

		// patient table fields
		int count = zdis.readInt();
		for (int i = 0; i < count; i++) {
			zdis.readInt(); // field id
			zdis.readUTF(); // field name
		}

		// Patient table field values
		count = zdis.readInt();
		for (int i = 0; i < count; i++) {
			zdis.readInt(); // field id
			zdis.readInt(); // patient id
			zdis.readUTF(); // value
		}

		// for every patient
		int icount = zdis.readInt();
		for (int i = 1; i < icount + 1; i++) {

			// get patient id
			int patientId = zdis.readInt();

			// loop through list of obs
			int jcount = zdis.readInt();
			for (int j = 0; j < jcount; j++) {

				// get field name
				String fieldName = zdis.readUTF();

				// get ob values
				int kcount = zdis.readInt();
				for (int k = 0; k < kcount; k++) {

					Observation o = new Observation();
					o.setPatientId(patientId);
					o.setFieldName(fieldName);

					byte dataType = zdis.readByte();
					if (dataType == Constants.TYPE_STRING) {
						o.setValueText(zdis.readUTF());
					} else if (dataType == Constants.TYPE_INT) {
						o.setValueInt(zdis.readInt());
					} else if (dataType == Constants.TYPE_FLOAT) {
						o.setValueNumeric(zdis.readFloat());
					} else if (dataType == Constants.TYPE_DATE) {
						o.setValueDate(new Date(zdis.readLong()));
					}

					o.setDataType(dataType);
					o.setEncounterDate(new Date(zdis.readLong()));
					mPatientDbAdapter.createObservation(o);
				}
			}

			publishProgress("history", Integer.valueOf(i + icount).toString(),
					Integer.valueOf(icount * 2).toString());
		}

	}
	
}
