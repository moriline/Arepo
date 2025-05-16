package org.arepo.entites;

import com.google.gson.annotations.Expose;
import org.arepo.common.AppUtils;
import org.arepo.common.FileCharsets;
import org.arepo.common.GlobalValues;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ATempFiles {
    @Expose
    private Long id;
    @Expose
    private String name;
    @Expose
    private Long hash;
    private byte[] content;
    @Expose
    private Integer charset= GlobalValues.BINARY_CHARSET; // if charset is -1 - this is binary file! 0 - for text file with UTF-8.

    public ATempFiles(Path dirPath, String filename) throws IOException {
        this.id = 0L;
        var local = dirPath.resolve(filename).toAbsolutePath();
        this.name = filename;//.replace("\\", AppSystem.SEP);
        if(Files.exists(local)){
            try{
                var lines = Files.readAllLines(local, FileCharsets.U8.getCharset());
                this.charset = FileCharsets.U8.ordinal();
            }catch (IOException e){
                System.out.println("binary!");
            }finally {
                this.content = Files.readAllBytes(local);
                this.hash = AppUtils.getCRC32Checksum(content);
            }
        }else {
            this.hash = null;
            this.content = null;
        }

    }

    public ATempFiles(Long id, String name, byte[] content) {
        this.id = id;
        this.name = name;
        this.hash = content != null? AppUtils.getCRC32Checksum(content):null;
        this.content = content;
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

    public Long getHash() {
        return hash;
    }

    public void setHash(Long hash) {
        this.hash = hash;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Integer getCharset() {
        return charset;
    }

    public void setCharset(Integer charset) {
        this.charset = charset;
    }

    @Override
    public String toString() {
        return "ATempFiles{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", hash=" + hash +
                ", charset=" + charset +
                '}';
    }
}
