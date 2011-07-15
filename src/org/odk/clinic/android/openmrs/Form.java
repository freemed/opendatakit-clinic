package org.odk.clinic.android.openmrs;

public class Form {

    private Integer formId = null;
    private String path = null;
    private String name = null;
    
    @Override
    public String toString() {
        return name;
    }
    
    public Integer getFormId() {
        return formId;
    }
    
    public void setFormId(Integer formId) {
        this.formId = formId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
}
