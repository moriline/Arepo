package org.arepo.common;

import java.util.Arrays;

public enum EOL {
    Unix("\n"), //LF
    MacOS("\r"), //CR
    Windows("\r\n");//CRLF
    private String separator;

    EOL(String value) {
        this.separator = value;
    }

    public static String findByIndex(int index){
        for (var ch : values()){
            if(ch.ordinal() == index){
                return ch.getSeparator();
            }
        }
        return "";
    }
    public String getSeparator() {
        return separator;
    }

    public String getAll() {
        return Arrays.toString(values());
    }

    public void printSelf() {
        for (var it : values()) {
            System.out.println("e:" + it.ordinal());
        }
    }
}
