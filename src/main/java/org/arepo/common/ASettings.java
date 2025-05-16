package org.arepo.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ASettings {
    String DIR_NAME = ".arepo"; // palindrome for OPERA !
    String SEP = "/";
    String FILE_SETTINGS = "settings.json";
    String ROOT_DB_NAME = "_root.db";
    String PATCH_EXTENSION = ".db";
    String UPLOAD_EXTENSION = ".db3";
    String DB_NAME = "main.db";
    String SIGN_ERASIED_FILE = "*";

    String TEXT_FILE_TYPE = "t";
    String BINARY_FILE_TYPE = "b";
    String DEFAULT_GROUP_NAME = "master";
    String DEFAULT_FOLDER_ROOT_NAME = "";
    String SCHEMA_PREFIX = "s_";
    String DEFAULT_SCHEMA = "public";
    String IGNORE_FOLDERS_NAME = ".ignored_folders";
    String IGNORE_FILES_NAME = ".ignored_files";
    String APP_HISTORY_FILE = "history.json";

    String DIR_PATCH = "patch";
    String DIR_UPLOAD = "upload";
    String FILE_CONFLICT = ".temp";
    String PATCH_DELETED = ".deleted";
    String STATE_FILENAME = "state.json";
    String CONFIG_FILE = ".config";
    String ROOT_PREFIX = "";
    Integer LINUX = 1;
    Integer WINDOWS = 2;
    Integer MAC = 3;
    Map<Integer, String> eol = Map.of(LINUX, "\n", //LF
            WINDOWS, "\r\n", //CR+LF
            MAC, "\r");//CR
    List<String> defindedPatches = List.of("a", "b", "c");
    Set<String> charsets = Set.of("UTF-8","UTF-16","Windows-1251");
}
