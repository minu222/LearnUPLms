package com.lms.mainpages.entity;

public class instructorProfile {
    private int instructorId;   // = users.user_id (FK)
    private String affiliation;
    private String bio;

    public instructorProfile() {}

    public instructorProfile(int instructorId, String affiliation, String bio) {
        this.instructorId = instructorId;
        this.affiliation = affiliation;
        this.bio = bio;
    }

    public int getInstructorId() { return instructorId; }
    public void setInstructorId(int instructorId) { this.instructorId = instructorId; }
    public String getAffiliation() { return affiliation; }
    public void setAffiliation(String affiliation) { this.affiliation = affiliation; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}

