package org.arepo.entites;

import com.google.gson.annotations.Expose;

public class LogFiles {
    @Expose
    private Long id;
    @Expose
    private String comment;
    @Expose
    private String email;
    @Expose
    private String files; //JSON like this with textfileid : ["22", "31"]
    @Expose
    private String types; // JSON like with ["t", "b"] where t -text, b - binary files
    private byte[] content; // ArrayList<byte[]> content = new ArrayList<>();
    @Expose
    private Long modified;

    public LogFiles(Long id, String comment, String email, String files, String types, byte[] content, Long modified) {
        this.id = id;
        this.comment = comment;
        this.email = email;
        this.files = files;
        this.types = types;
        this.content = content;
        this.modified = modified;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFiles() {
        return files;
    }

    public void setFiles(String files) {
        this.files = files;
    }

    public String getTypes() {
        return types;
    }

    public void setTypes(String types) {
        this.types = types;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Long getModified() {
        return modified;
    }

    public void setModified(Long modified) {
        this.modified = modified;
    }

    @Override
    public String toString() {
        return "LogFiles{" +
                "id=" + id +
                ", comment='" + comment + '\'' +
                ", email='" + email + '\'' +
                ", files='" + files + '\'' +
                ", types='" + types + '\'' +
                ", modified=" + modified +
                '}';
    }
}
