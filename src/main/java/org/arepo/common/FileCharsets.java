package org.arepo.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public enum FileCharsets {
    U8(StandardCharsets.UTF_8),
    U16(StandardCharsets.UTF_16),
    ASCII(StandardCharsets.US_ASCII);

    private Charset charset;

    FileCharsets(Charset charset) {
        this.charset = charset;
    }

    public static Charset findByIndex(Integer index){
        for (var ch : values()){
            if(ch.ordinal() == index){
                return ch.getCharset();
            }
        }
        return null;
    }
    public Charset getCharset() {
        return charset;
    }

    public String getAll() {
        return Arrays.toString(values());
    }

    public static void main(String[] args) {
        Integer index = -1;
        System.out.println("f:"+FileCharsets.findByIndex(index));
        var utf8 = FileCharsets.U16;
        System.out.println("index:"+utf8);
    }
}
