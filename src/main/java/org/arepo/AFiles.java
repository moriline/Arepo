package org.arepo;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.google.gson.reflect.TypeToken;
import org.arepo.common.*;
import org.arepo.entites.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AFiles {
    public final Path dirPath;
    public final Model model;

    private AFiles(Path dirPath) throws Exception {
        this.dirPath = dirPath.toAbsolutePath();
        if(Files.notExists(this.dirPath)) throw new Exception(GlobalValues.DNE+this.dirPath);
        model = new Model(this.dirPath);
    }
    public AFiles(String path)throws Exception{
        //if(path.isEmpty()) throw new Exception("empty");
        this(Path.of(path).toAbsolutePath());
    }

    public RepoSettings setup(String root, String email, Integer eol, boolean isShortLogs) throws Exception {
        RepoSettings result = null;
        if(!ASettings.eol.containsKey(eol)) throw new Exception("End of line is not exists:"+eol);
        if(ASettings.ROOT_PREFIX.equals(root)){
            //this is root repo like: setup root -> FROM ''
            model.create();
            result = new RepoSettings(dirPath, email, eol);
            result.useFS = false; //это репозиторий, не имеющий рабочей директории.
            model.save(result);
        }else {
            //this is clone repo like: setup clone -> FROM root
            var rootPath = Path.of(root).toAbsolutePath();
            if(Files.notExists(rootPath)) throw new Exception(GlobalValues.DNE+rootPath);
            //if(Files.exists(rootPath.resolve(ASettings.DIR_NAME))) throw new Exception(GlobalValues.CIE+rootPath);
            var rootModel = new Model(rootPath);
            var rootSettings = rootModel.get();

            model.create();
            result = new RepoSettings(dirPath, email, eol);
            result.remote = rootPath.toString();
            result.version = rootSettings.version;
            model.save(result);

            var logs = isShortLogs?rootModel.log.findLastLogs(1):rootModel.log.logFiles();
            var files = rootModel.text.all();
            if(!logs.isEmpty()) model.log.insertLogFiles(logs);
            if(!files.isEmpty()) {
                model.text.insertFiles(files);
                fillFileSystemByFiles(dirPath, result, files);
            }
        }
        return result;
    }
    public StatusResult status() throws IOException {
        var settings = model.get();
        if(settings.useFS){
            model.temp.dropTable();
            model.temp.createTable();
            model.temp.add(dirPath, settings.ignoreDirs, settings.ignoreFiles);
        }
        //return AppJson.gson.newBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(model.status.status2());
        var result = model.status.status2();
        result.version = settings.version;
        return result;
    }

    public void download() throws Exception {
        var settings = model.get();
        //Long lastLogModified = model.log.maxLogModified();
        var version = settings.version;
        if(settings.remote.isEmpty()) throw new Exception(GlobalValues.WAFR+dirPath);

        var remoteRootPath = Path.of(settings.remote);
        var rootModel = new Model(remoteRootPath);
        var rootSettings = rootModel.get();
        //var rootlastLogModified = rootModel.log.maxLogModified();
        System.out.println("local max:"+version+"; rem max:"+rootSettings.version);
        if(rootSettings.version > version){
            var remLogs = rootModel.log.getLogsAfterModified(version);
            System.out.println("get logs from remote:"+remLogs);
            model.log.insertLogFiles(remLogs);
            var files = rootModel.text.getFilesAfterVersion(version);
            model.text.deleteByIds(files.stream().map(TextFiles::getId).toList());
            model.text.insertFiles(files);
            //save new version
            settings.version = rootSettings.version;
            model.save(settings);
        }
    }
    public String upload(String patchFilename) throws Exception {
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(GlobalValues.WAFR+dirPath);
        var patchFiles = model.patch.getFilesFromPatch(patchFilename);
        if(patchFiles.isEmpty()) throw new Exception(GlobalValues.PIE+dirPath);
        var comment = model.patch.getComment(patchFilename);
        if(comment.isEmpty()) throw new Exception("Comment is empty!");
        var remotePatch = model.patch.repo.resolve(ASettings.DIR_NAME).resolve(settings.email+"_"+AppUtils.getTimeInSec()+ASettings.PATCH_EXTENSION);//model.patch.createPatchName(patchFilename, settings.email);
        String remotePatchname = "";
        try {
            model.patch.clearHistory(patchFilename); //clear history before sending to remote repo
            Files.move(model.patch.repo.resolve(ASettings.DIR_NAME).resolve(patchFilename+ASettings.PATCH_EXTENSION),
                    Path.of(settings.remote).resolve(ASettings.DIR_NAME).resolve(remotePatch.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            var finalRemotePatch = Path.of(settings.remote).resolve(ASettings.DIR_NAME).resolve(remotePatch.getFileName());
            remotePatchname = remotePatch.getFileName().toString().substring(0, remotePatch.getFileName().toString().lastIndexOf(ASettings.PATCH_EXTENSION));
            var result = remoteUpload(settings, remotePatchname, finalRemotePatch);
            model.patch.createPatchDB(patchFilename);
        }catch (IOException e){
            throw new Exception(e.getMessage());
        }
        return remotePatchname;
    }
    @Deprecated
    public String upload_old(String patchFilename) throws Exception {
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(GlobalValues.WAFR+dirPath);
        var patchFiles = model.patch.getFilesFromPatch(patchFilename);
        if(patchFiles.isEmpty()) throw new Exception(GlobalValues.PIE+dirPath);
        var comment = model.patch.getComment(patchFilename);
        if(comment.isEmpty()) throw new Exception("Comment is empty!");
        //System.out.println("upload clone repo");
        var orig = model.patch.getPatchPath(patchFilename);
        var remotePatch = model.patch.createPatchName(patchFilename, settings.email);
        //System.out.println("rem path:"+remotePatch);
        try{
            Files.copy(orig, remotePatch, StandardCopyOption.REPLACE_EXISTING);
            //System.out.println("list patches:"+model.patch.list());
            //merge strategy
            var finalRemotePatch = Path.of(settings.remote).resolve(ASettings.DIR_NAME).resolve(remotePatch.getFileName());
            Files.copy(remotePatch, finalRemotePatch, StandardCopyOption.REPLACE_EXISTING);
            var remotePatchname = remotePatch.getFileName().toString().substring(0, remotePatch.getFileName().toString().lastIndexOf(ASettings.PATCH_EXTENSION));
            var result = remoteUpload(settings, remotePatchname, finalRemotePatch);
            model.patch.deletePatch(remotePatchname);
            model.patch.clearHistory(patchFilename);
            model.patch.clearData(patchFilename);
        }catch (IOException e){
            //restore patch files...
            Files.deleteIfExists(remotePatch);
            throw new Exception(e.getMessage());
        }
        System.out.println("upload");
        return "";
    }
    private static int remoteUpload(RepoSettings settings, String remotePatchname, Path finalRemotePatch) throws IOException {
        if(Files.notExists(finalRemotePatch)) throw new IOException("Patch not exists:"+finalRemotePatch.toString());
        var rootModel = new Model(Path.of(settings.remote));
        var rootSettings = rootModel.get();
        if(!rootSettings.remote.isEmpty()) throw new IOException("Upload should be for the root repo only.");
        rootSettings.version = rootModel.patch.addPatchToLogs(remotePatchname, settings.email, rootModel.text, rootModel.log);
        rootModel.save(rootSettings);
        rootModel.patch.clearHistory(remotePatchname);
        //mark this patch as deleted
        finalRemotePatch.toFile().renameTo(new File(finalRemotePatch.toFile().toString()+ASettings.PATCH_DELETED));
        return 1;
    }
    /**
     * Save file only changed files.
     * @param patchname name of patch like "a".
     * @param filenames List of filenames for saving. If the list is empty, all previously saved files will be saved.
     * @return
     * @throws Exception
     */
    public List<ATempFiles> save(String patchname, Set<String> filenames) throws Exception {
        var time = AppUtils.getCurrentTimeInSeconds();
        List<ATempFiles> result = new ArrayList<ATempFiles>();
        var filenames2 = new ArrayList<String>();
        var named = new NamedParameterJdbcTemplate(model.patch.build2(patchname+ ASettings.PATCH_EXTENSION, true));
        var savedFiles = model.patch.getFilesFromPatch(named);

        var savedFilesList = savedFiles.stream().collect(Collectors.toMap(APatches::getName, Function.identity()));
        if(filenames.isEmpty()){
            filenames2.addAll(savedFilesList.keySet());
        }else {
            filenames2.addAll(filenames);
        }

        for (var filename : filenames2){
            filename = filename.replace("\\", ASettings.SEP);
            if(model.get().ignoreFiles.contains(filename)) continue;
            ATempFiles temp = new ATempFiles(dirPath, filename);
            if(temp.getContent() == null && !savedFilesList.containsKey(filename)) {
                if(model.text.findByName(filename).isEmpty()){
                    throw new Exception(GlobalValues.FNE+filename);
                }
            }

            if(temp.getContent() != null && savedFilesList.containsKey(filename)){
                var file = savedFilesList.get(filename);
                if(temp.getHash().equals(file.getHash())) continue;
            }
            var version = model.text.getVersionByName(filename);
            var res = (version == 0L && temp.getContent() == null)?model.patch.deleteFile(named, temp.getName(), time):model.patch.addFile(named, temp, version, time);
            if(res > 0){
                result.add(temp);
            }

        }
        return result;
    }

    public void remove(String patchname, Set<String> filenames) throws Exception {
        var named = new NamedParameterJdbcTemplate(model.patch.build2(patchname+ ASettings.PATCH_EXTENSION, true));
        var time = AppUtils.getCurrentTimeInSeconds();
        for(var filename : filenames){
            model.patch.deleteFile(named, filename, time);
        }
    }
    public String setComment(String patchname, String comment) throws Exception {
        if(comment.isEmpty()) throw new Exception("comment is empty");
        var result = model.patch.createComment(patchname, comment);
        return "";
    }
    public String getComment(String patchname) throws Exception {
        return model.patch.getComment(patchname);
    }

    public APatchInfo showPatch(String patchname) throws Exception {
        var result = new APatchInfo(model.patch.getHistory(patchname), model.patch.getFilesFromPatch(patchname));
        return result;
    }
    public String clearPatch(String patchname) throws Exception {
        model.patch.clearData(patchname);
        return "";
    }
    public List<String> patches() throws Exception {
        return model.patch.list();
    }

    /**
     * Get map of modified or deleted files for patch to File system.
     * If file modified and in the conflict - would be created new file with .temp extension by patch content.
     * Use any merge tool for resolve this conflict between bin/file.txt and bin/file.txt.temp
     * @param patchname
     * @return Map<String, String> - map of modified or deleted files.
     * @throws Exception
     */
    public Map<String, String> diff(String patchname) throws Exception {
        var result = new HashMap<String, String>();
        var files = model.patch.getFilesFromPatch(patchname);
        for(var file : files){
            var local = dirPath.resolve(file.getName()).toAbsolutePath();
            if(Files.exists(local)){
                var hash = AppUtils.getCRC32Checksum(Files.readAllBytes(local));
                //System.out.println("hash:"+hash);
                if(!file.getHash().equals(hash)){
                    Files.write(dirPath.resolve(file.getName()+ASettings.FILE_CONFLICT).toAbsolutePath(), file.getContent());
                    result.put(file.getName(), "Modified");
                }
            }else {
                result.put(file.getName(), "Deleted");
            }
        }
        return result;
    }
    public List<APatches> applyPatch2FS(String patchname) throws Exception {
        var files = model.patch.getFilesFromPatch(patchname);
        for(var file : files){
            var local = dirPath.resolve(file.getName()).toAbsolutePath();
            if(file.getContent() != null && file.getHash() != null){
                local.getParent().toFile().mkdirs();
                Files.write(local, file.getContent());
            }else {
                if(Files.exists(local)){
                    Files.delete(local);
                }
            }
            //Files.write(dirPath.resolve(file.getName()).toAbsolutePath(), file.getContent(), StandardOpenOption.WRITE);
        }
        return files;
    }
    public List<APatchHistory> applyHistory2FS(String patchname, Long time) throws Exception {
        var files = model.patch.getHistoryByTime(patchname, time);
        for(var file : files){
            var local = dirPath.resolve(file.getName()).toAbsolutePath();
            if(file.getContent() != null){
                local.getParent().toFile().mkdirs();
                Files.write(local, file.getContent());
            }else {
                if(Files.exists(local)){
                    Files.delete(local);
                }
            }
        }
        return files;
    }
    public void clearFS() throws Exception {
        var settings = model.get();
        FSUtils.clear(dirPath, settings.ignoreFiles, settings.ignoreDirs);
    }
    public void clearAndHead() throws Exception {
        var settings = model.get();
        //settings.ignoreFiles.add(ASettings.DIR_NAME);
        var files = model.text.all();
        fillFileSystemByFiles(dirPath, settings, files);
    }
    private static void fillFileSystemByFiles(Path dirPath, RepoSettings settings, List<TextFiles> files) throws IOException {
        if(!files.isEmpty()){
            FSUtils.clear(dirPath, settings.ignoreFiles, settings.ignoreDirs);
        }
        for(var file : files){
            if(file.getName().startsWith(ASettings.SIGN_ERASIED_FILE)) continue;
            var local = dirPath.resolve(file.getName()).toAbsolutePath();
            if(file.getContent() != null){
                local.getParent().toFile().mkdirs();
                Files.write(local, file.getContent());
            }else {
                if(Files.exists(local)){
                    Files.delete(local);
                }
            }
        }
    }
    public List<LogFiles> getLogs() throws Exception {
        var files = model.log.logFiles();
        return files;
    }
    public List<TextFiles> getFiles() throws Exception {
        return model.text.all();
    }
    public List<LogFiles> getLogsByFilename(String filename) throws Exception {
        var founded = model.text.findByName(filename);
        if(founded.isEmpty()){
            return List.of();
        }
        return model.log.findByFileId(founded.iterator().next().getId());
    }

    public List<LogFiles> applyFSByLogsByCountLimit(int countLogsLimit) throws Exception {
        var itemsArrType = new TypeToken<String[]>() {}.getType();
        var Jsontype = new TypeToken<ArrayList<String>>(){}.getType();
        var logs = model.log.findLastLogs(countLogsLimit);
        var textFilesPatchesMap = new HashMap<TextFiles, List<String>>();
        var binaryIds = new HashMap<Long, String>();
        Long lastModifiedLog = 0L;
        for(var log : logs){
            lastModifiedLog = log.getModified();
            String[] types = AppUtils.gson.fromJson(log.getTypes(), itemsArrType);
            String[] files = AppUtils.gson.fromJson(log.getFiles(), itemsArrType);
            List<byte[]> content = AppUtils.convert2ListOfBytes(log.getContent());
            var foundedMap = model.text.findAsMapByIds(Arrays.asList(files).stream().map(Long::valueOf).collect(Collectors.toList()));
            //System.out.println("past:"+files+";"+types+";"+parents);
            foundedMap.values().stream().forEach(it -> textFilesPatchesMap.putIfAbsent(it, new ArrayList<String>()));
            for (int i = 0; i <files.length; i++) {
                var fileId = Long.parseLong(files[i]);
                var textFile = foundedMap.get(fileId);
                var type = types[i];
                var contentUniPatch = content.get(i);
                if(ASettings.BINARY_FILE_TYPE.equals(type)){
                    System.out.println("BIN:"+fileId);
                    binaryIds.put(fileId, textFile.getName());

                    if(contentUniPatch != null){
                        Files.write(dirPath.resolve(textFile.getName()), contentUniPatch);
                    }else {
                        Files.delete(dirPath.resolve(textFile.getName()));
                    }
                }else {
                    textFilesPatchesMap.get(textFile).add(new String(contentUniPatch));
                }
            }
        }
        //text files
        for(var entry : textFilesPatchesMap.entrySet()){
            var textFile = entry.getKey();
            var orig = new String(textFile.getContent());
            var finalContent = applyPatches(Arrays.asList(orig.split("\n")), entry.getValue());
            var content = String.join("\n", finalContent).trim();
            //System.out.println("te:"+textFile.toString());
            //System.out.println("te 2:"+content);
            if(!content.isEmpty()){
                Files.write(dirPath.resolve(textFile.getName()), content.getBytes());
            }else {
                Files.delete(dirPath.resolve(textFile.getName()));
            }
        }
        //binary files
        for(var entry : binaryIds.entrySet()){
            var id = entry.getKey();
            var binaryLogs = model.log.findByFileIdLessModified(id, lastModifiedLog);
            for(var binLog : binaryLogs){
                String[] binfiles = AppUtils.gson.fromJson(binLog.getFiles(), itemsArrType);
                List<byte[]> bincontent = AppUtils.convert2ListOfBytes(binLog.getContent());
                for (int i = 0; i <binfiles.length; i++) {
                    var binfileId = Long.parseLong(binfiles[i]);
                    if(id.equals(binfileId)){
                        var bytes = bincontent.get(i);
                        if(bytes != null){
                            Files.write(dirPath.resolve(entry.getValue()), bytes);
                        }
                        break;
                    }
                }
            }
        }
        return logs;
    }
    private static List<String> applyPatches(List<String> orig, List<String> patches){
        List<String> result = new ArrayList<String>(orig);
        for(var patchValue : patches){
            List<String> unifiedDiff = Arrays.asList(patchValue.split("\n"));
            var patch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiff);
            result = DiffUtils.unpatch(result, patch);
        }
        return result;
    }
    public String sendPatch(String patchFilename) throws Exception{
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(GlobalValues.WAFR+dirPath);
        var patchFiles = model.patch.getFilesFromPatch(patchFilename);
        if(patchFiles.isEmpty()) throw new Exception(GlobalValues.PIE+dirPath);
        var comment = model.patch.getComment(patchFilename);
        if(comment.isEmpty()) throw new Exception("Comment is empty!");
        String remotePatchname = "";
        try{
            var remotePatch = model.patch.repo.resolve(ASettings.DIR_NAME).resolve(settings.email+"_"+AppUtils.getTimeInSec()+ASettings.PATCH_EXTENSION);//model.patch.createPatchName(patchFilename, settings.email);
            var finalRemotePatch = Path.of(settings.remote).resolve(ASettings.DIR_NAME).resolve(remotePatch.getFileName());
            Files.move(model.patch.repo.resolve(ASettings.DIR_NAME).resolve(patchFilename+ASettings.PATCH_EXTENSION),
                    finalRemotePatch, StandardCopyOption.REPLACE_EXISTING);
            remotePatchname = remotePatch.getFileName().toString().substring(0, remotePatch.getFileName().toString().lastIndexOf(ASettings.PATCH_EXTENSION));
            model.patch.createPatchDB(patchFilename);
        }catch (IOException e){
            throw new Exception(e.getMessage());
        }
        return remotePatchname;
    }
    @Deprecated
    public String sendPatch_old(String patchFilename)throws Exception{
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(GlobalValues.WAFR+dirPath);
        var patchFiles = model.patch.getFilesFromPatch(patchFilename);
        if(patchFiles.isEmpty()) throw new Exception(GlobalValues.PIE+dirPath);

        var orig = model.patch.getPatchPath(patchFilename);
        var remotePatch = model.patch.createPatchName(patchFilename, settings.email);
        System.out.println("rem path:"+remotePatch);
        String remotePatchname = "";
        try{
            var finalRemotePatch = Path.of(settings.remote).resolve(ASettings.DIR_NAME).resolve(remotePatch.getFileName());
            Files.copy(orig, finalRemotePatch, StandardCopyOption.REPLACE_EXISTING);
            remotePatchname = remotePatch.getFileName().toString().substring(0, remotePatch.getFileName().toString().lastIndexOf(ASettings.PATCH_EXTENSION));
            //var result = remoteUpload(settings, remotePatchname, finalRemotePatch);
            model.patch.deletePatch(remotePatchname);
            model.patch.clearHistory(patchFilename);
            model.patch.clearData(patchFilename);
        }catch (IOException e){
            Files.deleteIfExists(remotePatch);
            throw new Exception(e.getMessage());
        }
        return remotePatchname;
    }
    public String getPatch(String patchFilename) throws Exception {
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(GlobalValues.WAFR+dirPath);
        var remotePath = Path.of(settings.remote).resolve(ASettings.DIR_NAME).resolve(patchFilename+ASettings.PATCH_EXTENSION);
        Files.copy(remotePath, dirPath.resolve(ASettings.DIR_NAME).resolve(patchFilename+ASettings.PATCH_EXTENSION), StandardCopyOption.REPLACE_EXISTING);
        return patchFilename;
    }
    public String deleteRemotePatch(String patchFilename) throws Exception{
        if(ASettings.defindedPatches.contains(patchFilename)) throw new Exception("Invalid patch name:"+patchFilename);
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(GlobalValues.WAFR+dirPath);
        Files.deleteIfExists(Path.of(settings.remote).resolve(ASettings.DIR_NAME).resolve(patchFilename+ASettings.PATCH_EXTENSION));
        return patchFilename;
    }
    public String deleteLocalPatch(String patchFilename) throws Exception{
        if(ASettings.defindedPatches.contains(patchFilename)) throw new Exception("Invalid patch name:"+patchFilename);
        Files.deleteIfExists(dirPath.resolve(ASettings.DIR_NAME).resolve(patchFilename+ASettings.PATCH_EXTENSION));
        return patchFilename;
    }

    /**
     * Update repo settings for ignore dirs or files.
     * @param ignoreDirs
     * @param ignoreFiles
     * @return
     * @throws Exception
     */
    public RepoSettings updateSettings(Set<String> ignoreDirs, Set<String> ignoreFiles) throws Exception {
        //if(ignoreDirs.isEmpty()) throw new Exception("Dirs is empty!");
        //if(ignoreFiles.isEmpty()) throw new Exception("Files is empty!");
        var settings = model.get();
        settings.ignoreDirs = ignoreDirs;
        settings.ignoreFiles = ignoreFiles;
        model.save(settings);
        return settings;
    }

    public String accept(String patchFilename) throws Exception {
        if(ASettings.defindedPatches.contains(patchFilename)) throw new Exception("Invalid patch name:"+patchFilename);
        var settings = model.get();
        if(settings.remote.isEmpty()) throw new Exception(GlobalValues.WAFR+dirPath);

        var remotePatch = Path.of(settings.remote).resolve(ASettings.DIR_NAME).resolve(patchFilename+ASettings.PATCH_EXTENSION);
        var finalRemotePatch = Path.of(settings.remote).resolve(ASettings.DIR_NAME).resolve(remotePatch.getFileName());

        var remotePatchname = remotePatch.getFileName().toString().substring(0, remotePatch.getFileName().toString().lastIndexOf(ASettings.PATCH_EXTENSION));
        var result = remoteUpload(settings, remotePatchname, finalRemotePatch);
        //model.patch.deletePatch(remotePatchname);
        //model.patch.clearHistory(patchFilename);
        //model.patch.clearData(patchFilename);
        return "";
    }
}
