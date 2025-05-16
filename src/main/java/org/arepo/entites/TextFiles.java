package org.arepo.entites;

import com.google.gson.annotations.Expose;
import org.arepo.common.AppUtils;
import org.arepo.common.FileCharsets;

import java.util.Objects;

import static org.arepo.common.GlobalValues.INIT_ID;

public class TextFiles {
    @Expose
    private Long id;
    @Expose
    private String name;
    private int size = 0;
    private byte[] content; // when file deleted content must be null
    @Expose
    private Long hash;
    @Expose
    private Long parent = INIT_ID; // id of previous text file when it moved
    @Expose
    private Long version = INIT_ID;
    @Expose
    private Integer charset = FileCharsets.U8.ordinal();

    public TextFiles(Long id, String name, byte[] content) {
        this.id = id;
        this.name = name;
        this.content = content;
        update(content);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getParent() {
        return parent;
    }

    public void setParent(Long parent) {
        this.parent = parent;
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
    public void update(byte[] content){
        this.content = content;
        this.hash = content != null? AppUtils.getCRC32Checksum(content):null;
        this.size = content != null?content.length:0;
    }
    @Override
    public String toString() {
        return "TextFiles{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", hash='" + hash + '\'' +
                ", parent=" + parent +
                ", version=" + version +
                ", charset=" + charset +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextFiles textFiles = (TextFiles) o;
        return Objects.equals(id, textFiles.id) && Objects.equals(name, textFiles.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
