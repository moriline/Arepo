package org.arepo.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitResult.CONTINUE;

public class FSUtils {

    private static final String SEP = "/";
    public static Map<String, File> findFiles(String sdir, Set<String> skipFiles, Set<String> skipFolders) throws IOException {
        var result = new LinkedHashMap<String, File>();
        Files.walk(Paths.get(sdir))
                .filter(Files::isRegularFile)
                .forEach(it->{
                    //System.out.println("f:"+AppCommon.modifyPath(replace(it.toFile().getPath(), sdir)));
                    var key = modifyPath(replace(it.toFile().getPath(), sdir));
                    result.put(key, it.toFile());
                    for(String dir : skipFolders) {
                        if(!key.startsWith(dir)) {
                            //result.put(key, it.toFile());
                        }
                    }

                });
        //System.out.println("size:"+result.size());
        return result;
    }
    public static String replace(String path, String dir) {
        return path.substring(dir.length()+1);
    }
    public static String modifyPath(String filename) {
        return isWindows()?filename.replace("\\", "/"):filename;
    }
    public static boolean isWindows() {
        return getOS().startsWith("Windows")?true:false;
    }
    public static String getOS() {
        return System.getProperty("os.name");
    }
    public static Map<String, Map<String, Path>> getAllFiles3(String currentPath, Set<String> ignoreList) throws IOException {
        var result = new LinkedHashMap<String, Map<String, Path>>();
        result.put(ASettings.DEFAULT_FOLDER_ROOT_NAME, new LinkedHashMap<String, Path>());
        var basePath = Path.of(currentPath);
        FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(attrs.isRegularFile()){

                    var relPath = basePath.relativize(file).toString().replace("\\", SEP);
                    var founded = false;
                    for(var pat : ignoreList){
                        if(relPath.indexOf(pat) == 0){
                            founded = true;
                        }
                    }
                    if(relPath.indexOf(ASettings.DIR_NAME) == 0){
                        founded = true;
                    }

                    if(!founded){
                        var index = relPath.indexOf(SEP);
                        if(index == -1){
                            result.get(ASettings.DEFAULT_FOLDER_ROOT_NAME).put(relPath, file);
                            //System.out.println("root:"+relPath);
                        }else {
                            var top = relPath.substring(0, index);
                            //System.out.println("f:"+"sub:"+top);
                            result.computeIfAbsent(top, k ->new LinkedHashMap<String, Path>()).put(relPath, file);
                        }
                    }
                }
                return CONTINUE;
            }
        };
        Files.walkFileTree(basePath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, matcherVisitor);
        return result;
    }
    public static void clear(final Path currentPath, Set<String> skipFiles, Set<String> igroreDirs) throws IOException {
        Files.walkFileTree(currentPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                var relPath = currentPath.relativize(file).toString().replace("\\", SEP);
                if(!skipFiles.contains(relPath) && !relPath.startsWith(ASettings.DIR_NAME)){
                    for(var dir : igroreDirs){
                        if(relPath.startsWith(dir)){
                            return FileVisitResult.CONTINUE;
                        }
                    }
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                var relPath = currentPath.relativize(dir).toString().replace("\\", SEP);
                System.out.println("del dir:"+relPath);
                if(!relPath.isEmpty() && !ASettings.DIR_NAME.equals(relPath)){
                    for(var dir2 : igroreDirs){
                        if(relPath.startsWith(dir2)){
                            return FileVisitResult.CONTINUE;
                        }
                    }
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    public static void clearAll(final Path currentPath, Set<String> skipFiles, Set<String> igroreDirs) throws IOException {
        FileVisitor<Path> rootVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(attrs.isRegularFile()){
                    var relPath = currentPath.relativize(file).toString().replace("\\", SEP);
                    //System.out.println("rel:"+relPath);
                    if(!skipFiles.contains(relPath)){
                        var founded = false;
                        for(var pat : igroreDirs){
                            if(relPath.indexOf(pat) == 0){
                                founded = true;
                            }
                        }
                        if(relPath.indexOf(ASettings.DIR_NAME) == 0){
                            founded = true;
                        }
                        if(!founded){
                            //result.put(relPath, Files.readAllBytes(file));
                        }
                    }
                }
                return CONTINUE;
            }
        };
        Files.walkFileTree(currentPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, rootVisitor);
    }
    public static Map<String, byte[]> getAllFiles6(final Path currentPath, Set<String> skipFiles, Set<String> igroreDirs) throws IOException {
        var result = new HashMap<String, byte[]>();

        FileVisitor<Path> rootVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(attrs.isRegularFile()){
                    var relPath = currentPath.relativize(file).toString().replace("\\", SEP);
                    //System.out.println("rel:"+relPath);
                    if(!skipFiles.contains(relPath)){
                        var founded = false;
                        for(var pat : igroreDirs){
                            if(relPath.indexOf(pat) == 0){
                                founded = true;
                            }
                        }
                        if(relPath.indexOf(ASettings.DIR_NAME) == 0){
                            founded = true;
                        }
                        if(!founded){
                            result.put(relPath, Files.readAllBytes(file));
                        }
                    }
                }
                return CONTINUE;
            }
        };
        Files.walkFileTree(currentPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, rootVisitor);
        return result;
    }
    public static List<Path> getAllFiles5(final Path currentPath, Set<String> skipFiles, Set<String> igroreDirs) throws IOException {
        var result = new ArrayList<Path>();
        FileVisitor<Path> rootVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                if(attrs.isRegularFile()){
                    var top = file.subpath(currentPath.getNameCount(), currentPath.getNameCount()+1);
                    System.out.println("i:"+top+";f:"+file);
                    if(Files.isDirectory(currentPath.resolve(top))){
                        if(!igroreDirs.contains(top.toString())){
                            result.add(file);
                        }
                    }else {
                        result.add(file);
                    }
                }
                return CONTINUE;
            }
        };
        //Files.walkFileTree(currentPath, EnumSet.noneOf(FileVisitOption.class), 1, rootVisitor);
        Files.walkFileTree(currentPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, rootVisitor);
        return result;
    }
    public static Map<Integer, Map<String, Path>> getAllFiles4(final Path currentPath, Set<String> igroreDirs) throws IOException {
        var result = new LinkedHashMap<Integer, Map<String, Path>>();
        FileVisitor<Path> rootVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(attrs.isRegularFile()){
                    var relPath = currentPath.relativize(file).toString().replace("\\", SEP);
                    System.out.println("i:"+relPath.indexOf(ASettings.SEP));
                    var index = relPath.indexOf(SEP);
                    if(index == -1){
                        result.get(1).put(relPath, file);
                        System.out.println("root:"+relPath);
                    }
                }
                return CONTINUE;
            }
        };
        var alldirs = listDirectoriesStream(currentPath, igroreDirs);
        for(var dir : alldirs.entrySet()){
            var currentDir = dir.getValue();
            result.put(dir.getKey(), new LinkedHashMap<String, Path>());
            if(currentDir.isEmpty()){

                Files.walkFileTree(currentPath, EnumSet.noneOf(FileVisitOption.class), 1, rootVisitor);
            }else {
                if(!igroreDirs.contains(currentDir)){
                    System.out.println("other: "+currentPath.resolve(currentDir));

                    Files.walkFileTree(currentPath.resolve(currentDir), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if(attrs.isRegularFile()){
                                var relPath = currentPath.relativize(file).toString().replace("\\", SEP);
                                //System.out.println("i:"+relPath.indexOf(AppSystem.SEP));
                                //var index = relPath.indexOf(SEP);
                                //var top = relPath.substring(0, index);
                                //System.out.println("f:"+"sub:"+top);
                                //result.computeIfAbsent(top, k ->new LinkedHashMap<String, Path>()).put(relPath, file);
                                result.get(dir.getKey()).put(relPath, file);
                            }
                            return CONTINUE;
                        }
                    });
                }
            }
        }
        return result;
    }
    public static Set<String> listFilesUsingDirectoryStream(String dir) throws IOException {
        Set<String> fileSet = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileSet.add(path.getFileName()
                            .toString());
                }
            }
        }
        return fileSet;
    }
    public static Map<Integer, String> listDirectoriesStream(Path dir, Set<String> igroreDirs) throws IOException {

        Map<Integer, String> result = new HashMap<Integer, String>();
        var count = Integer.valueOf(1);
        result.put(count++, ASettings.DEFAULT_FOLDER_ROOT_NAME);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    var dir2 = path.getFileName().toString();
                    if(!igroreDirs.contains(dir2)){
                        result.put(count++, dir2);
                    }
                }
            }
        }
        return result;
    }
    public static Integer getTopFolderIndexForFile(Path base, Path file, Map<Integer, String> topFolders) throws Exception {
        var sub1 = file.subpath(base.getNameCount() , base.getNameCount()+1);
        if(Files.isDirectory(sub1)){
            var subStr = sub1.toString();
            for(var dirEntry : topFolders.entrySet()){
                if(dirEntry.getValue().equals(subStr)){
                    return dirEntry.getKey();
                }
            }
            throw new Exception("Top Folder id not founded for:"+file);
        }else {
            return 1;
        }

    }
    public static void clearDirRecursive(Path dirPath) throws IOException {
        if(Files.exists(dirPath)){
            Files.walkFileTree(dirPath.toAbsolutePath(),
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult postVisitDirectory(
                                Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(
                                Path file, BasicFileAttributes attrs)
                                throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }
                    });
        }
    }
    /*
    public static Set<String> listDirsUsingFileWalk(String dir, int depth, Set<String> igroreDirs) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(dir), depth)) {
            return stream
                    .filter(file -> Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }


    public static Set<String> listFilesUsingFileWalk(String dir, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(dir), depth)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }*/
}
