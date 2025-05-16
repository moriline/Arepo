package org.arepo.common;


import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public interface GlobalValues {
    String DELIMITER = "\n";
    Integer BINARY_CHARSET = -1;
    String ERROR = "ERROR:";
    Integer DEFAULT_INT = 0;
    Long INIT_ID = 0L;
    Long DEFAULT_ID = 1L;
    Map<String, Long> foldersNameIndex = new HashMap<String, Long>();
    static Charset[] charsets = {StandardCharsets.UTF_8,StandardCharsets.UTF_16, StandardCharsets.US_ASCII};//StandardCharsets.ISO_8859_1
    String CNE = "Clone not exists.";
    String CIE = "Clone is exists:";
    String DNE = "Directory not exists:";
    String FNE = "File not exists:";
    String FNF = "File not founded:";
    String WAFR = "Wrong action for root repo:"; // Only clone repo should to use file system with file. Root repo - only for centralized point for storage from clones.
    String PIE = "Patch is empty:";


    public static String makeError(Exception e){
        return GlobalValues.ERROR+e.getMessage();
    }

    static String commandNotFounded() {
        return makeError(new SQLException("command not founded"));
    }
}
