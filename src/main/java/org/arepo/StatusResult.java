package org.arepo;

import com.google.gson.annotations.Expose;
import org.arepo.common.AppUtils;

import java.util.HashSet;
import java.util.Set;

public class StatusResult {
    @Expose
    public Long version = 0L;
    @Expose
    private Set<String> addedFiles = new HashSet<String>();
    @Expose
    private Set<String> modifiedFiles = new HashSet<String>();
    @Expose
    private Set<String> deletedFiles = new HashSet<String>();
    @Expose
    private Set<String> erasedFiles = new HashSet<String>();
    //@Expose
    private Set<String> wrongFiles = new HashSet<String>();
    //only new files on filesystem
    //@Expose
    //public Set<String> filesystem = new HashSet<String>();

    public StatusResult() {
    }

    public Set<String> getAddedFiles() {
        return addedFiles;
    }

    public void setAddedFiles(Set<String> addedFiles) {
        this.addedFiles = addedFiles;
    }

    public Set<String> getModifiedFiles() {
        return modifiedFiles;
    }

    public void setModifiedFiles(Set<String> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }

    public Set<String> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(Set<String> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

    public Set<String> getErasedFiles() {
        return erasedFiles;
    }

    public void setErasedFiles(Set<String> erasedFiles) {
        this.erasedFiles = erasedFiles;
    }

    public Set<String> getWrongFiles() {
        return wrongFiles;
    }

    public void setWrongFiles(Set<String> wrongFiles) {
        this.wrongFiles = wrongFiles;
    }

    @Override
    public String toString() {
        return "StatusResult{" +
                "version=" + version +
                ", addedFiles=" + addedFiles +
                ", modifiedFiles=" + modifiedFiles +
                ", deletedFiles=" + deletedFiles +
                ", erasedFiles=" + erasedFiles +
                '}';
    }

    public static void main(String[] args) {
        var email = "root@mail.net";
        var one = new StringBuilder(email).append(System.nanoTime()).toString();

        var two = new StringBuilder(email).append(System.nanoTime()).toString();
        System.out.println("one:"+one+";"+ AppUtils.getCRC32Checksum(one.getBytes()));
        System.out.println("two:"+two+";"+ AppUtils.getCRC32Checksum(two.getBytes()));
    }
}
