package org.odk.clinic.android.openmrs;

public class FormInstance {

    private Integer patientId = null;
    private Integer formId = null;
    private String path = null;
    private String status = null;
    
    @Override
    public String toString() {
        return path;
    }
    
    public Integer getPatientId() {
        return patientId;
    }
    
    public void setPatientId(Integer patientId) {
        this.patientId = patientId;
    }
    
    public Integer getFormId() {
        return formId;
    }
    
    public void setFormId(Integer formId) {
        this.formId = formId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
}
