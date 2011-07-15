package org.odk.clinic.android.openmrs;


// TODO add description
public class Cohort {

	private Integer cohortId = null;
	private String name = null;
	
    @Override
    public String toString() {
    	return name;
    }
	
	public Integer getCohortId() {
		return cohortId;
	}
	
	public void setCohortId(Integer cohortId) {
		this.cohortId = cohortId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
