package org.odk.clinic.android.openmrs;

import java.util.Date;

public class Patient {

	private Integer patientId = null;
	private String identifier = null;
	
	private String givenName = null;
	private String familyName = null;
	private String middleName = null;
	
	private Date birthDate = null;
	private String gender = null;
	
	public Patient() {
		
	}
	

    @Override
    public String toString() {
    	return givenName + " " + middleName + " " + familyName + " " + identifier;
    }
	
	public Integer getPatientId() {
		return patientId;
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public String getGivenName() {
		return givenName;
	}
	
	public String getFamilyName() {
		return familyName;
	}
	
	public String getMiddleName() {
		return middleName;
	}
	
	public Date getBirthdate() {
		return birthDate;
	}
	
	public String getGender() {
		return gender;
	}

	public void setPatientId(Integer pid) {
		patientId = pid;
	}

	public void setIdentifier(String id) {
		identifier = id;
	}

	public void setFamilyName(String n) {
		familyName = n;
	}

	public void setGivenName(String n) {
		givenName = n;
	}

	public void setMiddleName(String n) {
		middleName = n;
	}

	public void setBirthDate(Date b) {
		birthDate = b;
	}

	public void setGender(String g) {
		gender = g;
	}

	public String getName(){
		String name = "";
		
		if(givenName != null)
			name = givenName;
		
		if(middleName != null){
			if(name.length() > 0)
				name += " ";
			
			name += middleName;
		}
		
		if(familyName != null){
			if(name.length() > 0)
				name += " ";
			
			name += familyName;
		}
		
		return name;
	}
}
