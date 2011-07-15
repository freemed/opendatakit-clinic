package org.odk.clinic.android.openmrs;

import java.util.Date;

public class Observation {

	private Integer patientId = null;
	private Float valueNumeric;
	private Integer valueInt;
	private Date valueDate;
	private String valueText;
	private Date encounterDate;
	private String fieldName;
	private byte dataType;
	
	
	public Integer getPatientId() {
		return patientId;
	}
	
	public void setPatientId(Integer patientId) {
		this.patientId = patientId;
	}
	
	public Float getValueNumeric() {
		return valueNumeric;
	}
	
	public void setValueNumeric(Float valueNumeric) {
		this.valueNumeric = valueNumeric;
	}
	
	public Date getValueDate() {
		return valueDate;
	}
	
	public void setValueDate(Date valueDate) {
		this.valueDate = valueDate;
	}
	
	public String getValueText() {
		return valueText;
	}
	
	public void setValueText(String valueText) {
		this.valueText = valueText;
	}
	
	public Date getEncounterDate() {
		return encounterDate;
	}
	
	public void setEncounterDate(Date encounterDate) {
		this.encounterDate = encounterDate;
	}

	public Integer getValueInt() {
		return valueInt;
	}

	public void setValueInt(Integer valueInt) {
		this.valueInt = valueInt;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public byte getDataType() {
		return dataType;
	}

	public void setDataType(byte dataType) {
		this.dataType = dataType;
	}
}
