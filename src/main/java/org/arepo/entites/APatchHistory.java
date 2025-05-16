package org.arepo.entites;

import com.google.gson.annotations.Expose;

public class APatchHistory {
    @Expose
    public String name;
    //@Expose
    public byte[] content;
    @Expose
    public Long updated = 0L;
    public APatchHistory(String name, byte[] content, Long updated) {
        this.name = name;
        this.content = content;
        this.updated = updated;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(Long updated) {
        this.updated = updated;
    }

    @Override
    public String toString() {
        return "APatchHistory{" +
                "name='" + name + '\'' +
                ", updated=" + updated +
                '}';
    }
}
