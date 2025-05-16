package org.arepo.entites;

import com.google.gson.annotations.Expose;
import org.arepo.common.AppUtils;
import org.arepo.common.GlobalValues;

import static org.arepo.common.GlobalValues.INIT_ID;

/**
 * Changes in local files.
 */
public class APatches {
    @Expose
    private String name;
    private byte[] content; // when file deleted content must be null
    @Expose
    private Long hash;
    @Expose
    private Long version = INIT_ID;
    @Expose
    private Integer charset = GlobalValues.BINARY_CHARSET; // -1 = for binary files
    @Expose
    private Long updated = 0L;
    @Expose
    private String comment = "";
    public APatches(String name, byte[] content, Long hash, Integer charset) {
        this.name = name;
        this.content = content;
        this.hash = hash;
        this.updated = AppUtils.getCurrentTimeInSeconds();
        this.charset = charset;
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

    public Long getHash() {
        return hash;
    }

    public void setHash(Long hash) {
        this.hash = hash;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getCharset() {
        return charset;
    }

    public void setCharset(Integer charset) {
        this.charset = charset;
    }

    public Long getUpdated() {
        return updated;
    }

    public void setUpdated(Long updated) {
        this.updated = updated;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "APatches{" +
                "name='" + name + '\'' +
                ", hash=" + hash +
                ", version=" + version +
                ", charset=" + charset +
                ", updated=" + updated +
                ", comment='" + comment + '\'' +
                '}';
    }
}
