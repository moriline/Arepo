package org.arepo;

import com.google.gson.annotations.Expose;
import org.arepo.common.ASettings;
import org.arepo.common.AppUtils;
import org.arepo.common.GlobalValues;
import org.arepo.entites.LogFiles;
import org.arepo.entites.RepoSettings;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Usage example: java -jar JackrabbitRepo-1.0-SNAPSHOT.jar store git
 */
@CommandLine.Command(name = "arepo", showEndOfOptionsDelimiterInUsageHelp = true, mixinStandardHelpOptions = true, version = "1.0", description = "Displays the @|bold ARepo|@ information.")
public class Main implements Runnable{
    public static String path;
    public static Path currentRelativePath;
    @CommandLine.Parameters(index = "0",  description = "Action for commands")
    MainAction action = MainAction.status;
    @CommandLine.Option(names = {"-a"}, description = "access token")
    private String token = "";
    @CommandLine.Option(names = {"-u"}, description = "username")
    private String username = "";
    @CommandLine.Option(names = {"-pw"}, description = "password")
    private String password = "";
    @CommandLine.Option(names = {"-t"}, description = "user type")
    Integer type = 0;
    @CommandLine.Option(names = {"-s"}, description = "user status")
    Integer status = 0;
    @CommandLine.Option(names = {"-ui"}, description = "user id")
    private Long userId = 0L;
    @CommandLine.Option(names = {"-eol"}, description = "End Of Line in text file")
    Integer eol = ASettings.LINUX;
    @CommandLine.Option(names = {"-e"}, description = "email")
    private String email = "";
    @CommandLine.Option(names = {"-c"}, description = "comment")
    private String comment = "";
    @CommandLine.Option(names = {"-p"}, description = "patchname")
    private String patchname = ASettings.defindedPatches.get(0);
    @CommandLine.Option(names = {"-fn"}, split = ",", description = "filenames")
    private Set<String> filenames = new HashSet<String>();
    @CommandLine.Option(names = {"-id"}, split = ",", description = "ignore directories")
    private Set<String> ignoreDirs = new HashSet<String>();
    @CommandLine.Option(names = {"-if"}, split = ",", description = "ignore files")
    private Set<String> ignoreFiles = new HashSet<String>();
    @CommandLine.Option(names = {"-sl"}, description = "short of logs. If true - loads from remote repo only 1 last logs.")
    boolean shortlogs = false; // For example: -f root_1 -sl -e root@mail.net
    @CommandLine.Option(names = {"-count"}, description = "undo logs by specified count and write remaining files to the file system")
    Integer count = 0;
    @CommandLine.Option(names = {"-tm"}, description = "time of updated")
    Long time = 0L;
    // init -f ../../.gitignore -f deps/gson-2.8.9.jar
    //@CommandLine.Option(names = {"-f"}, description = "files")
    //private Set<Path> files = new HashSet<Path>();
    @CommandLine.Option(names = {"-f"}, split = ",", description = "files")
    private LinkedHashSet<String> files = new LinkedHashSet<>(2);

    public static void main(String[] args) throws Exception {
        currentRelativePath = Paths.get("").toAbsolutePath();
        //CommandLine.run(new Main(), args);
        CommandLine commandLine = new CommandLine(new Main());
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
    @Override
    public void run() {
        //System.out.println("main:"+action);
        try {
            var settings = getSettings(currentRelativePath);
            if(email.isEmpty()){
                email = settings.email;
            }else {
                settings.email = email;
            }
            //Path dirPath = files.stream().findFirst().orElse(Path.of(settings.repo)).toAbsolutePath();
            var alist = files.stream().toList();
            var dir = alist.size() > 0?alist.get(0):settings.repo;
            var root = alist.size() > 1?alist.get(1):ASettings.ROOT_PREFIX;
            Object result = null;
            var afiles = new AFiles(dir);
            switch (action){
                case set -> result = set(afiles, settings);
                case setup -> result = setup(afiles, settings, root, shortlogs, eol);
                case status -> result = afiles.status();
                case save -> result = afiles.save(patchname, filenames);
                case remove -> afiles.remove(patchname, filenames);
                case comment -> result = afiles.setComment(patchname, comment);
                case upload -> afiles.upload(patchname);
                case download -> afiles.download();
                case send -> result = afiles.sendPatch(patchname);
                case get -> result = afiles.getPatch(patchname);
                case history -> result = afiles.applyHistory2FS(patchname, time);
                case clear -> afiles.clearFS();
                case diff -> result = afiles.diff(patchname);
                case apply -> result = afiles.applyPatch2FS(patchname);
                case erase -> result = afiles.clearPatch(patchname);
                case head -> afiles.clearAndHead();
                case logs -> result = afiles.getLogs();
                case logsbyname -> result = logsByName(afiles);
                case undo -> result = afiles.applyFSByLogsByCountLimit(count);//
                case files -> result = afiles.getFiles();
                case patches -> result = afiles.patches();
                case show -> result = afiles.showPatch(patchname);
                case delete -> result = afiles.deleteLocalPatch(patchname);
                case delete_remote_patch -> result = afiles.deleteRemotePatch(patchname);
                case settings -> result = updateRepoSettings(afiles);
                case accept -> result = afiles.accept(patchname);
                case eol -> result = ASettings.eol;
                case commands -> result = MainAction.values();
                default -> System.out.println("Unexpected value: " + action);
            }
            if(result != null)  System.out.println(AppUtils.gson2.toJson(result));
        } catch (Exception e) {
            e.printStackTrace();
            //System.err.println("Error:"+e.getMessage());
        }
    }

    /**
     * Update repo settings for ignore dirs or files or get settings if -id and -if arrays are empty.
     * <p>
     *     Usage for update ignore directories: <code>settings -id sub/directory,.project </code>
     *     <p>or update ignore files: <p>
     *     <code>settings -if dir/file.txt,readme.md</code>
     *     <p>
     *     or get settings: <p>
     *         <code>settings</code>
     *
     * @param afiles
     * @return
     * @throws Exception
     */
    private RepoSettings updateRepoSettings(AFiles afiles) throws Exception {
        return afiles.model.get();
    }

    /**
     * Get logs by filenames.
     * <p>
     * Usage: <code>logsbyname -fn file.txt, img.png</code>
     * @param afiles
     * @return Map of filenames
     * @throws Exception
     */
    private Map<String, List<LogFiles>> logsByName(AFiles afiles) throws Exception {
        var map = new HashMap<String, List<LogFiles>>();
        for(var filename : filenames){
            map.put(filename, afiles.getLogsByFilename(filename));
        }
        return map;
    }

    /**
     * Setup root or clone repository files in the directory.
     *<p>
     * Usage example for root:<code> setup -f root/dir -e user@mail.net</code>
     *<p>
     * Usage example for clone: <code>setup -f clone/dir,root/dir -e user@mail.net</code>
     * <p>or
     * <p>
     * <code>setup -f clone/dir -f root/dir -e user@mail.net</code>
     * @param afiles
     * @param settings
     * @param root
     * @return RepoSettings
     * @throws Exception
     */
    private RepoSettings setup(AFiles afiles, UserSettings settings, String root, boolean shortLogs, Integer eol) throws Exception {
        //System.out.println("setup:"+dir+";"+root);
        var result = afiles.setup(root, email, eol, shortLogs);
        if(!email.isEmpty()) {
            set(afiles, settings);
        }
        return result;
    }
    public UserSettings set(AFiles afiles, UserSettings settings) throws IOException {
        settings.repo = afiles.dirPath.toString();
        saveSettings(currentRelativePath, settings);
        return settings;
    }
    public UserSettings getSettings(Path dirPath) throws IOException {
        if(Files.notExists(dirPath.toAbsolutePath().resolve(ASettings.STATE_FILENAME))){
            saveSettings(dirPath, new UserSettings());
        }
        return AppUtils.gson.fromJson(Files.readString(dirPath.toAbsolutePath().resolve(ASettings.STATE_FILENAME)), UserSettings.class);
    }
    public void saveSettings(Path dirPath, UserSettings settings) throws IOException {
        Files.write(dirPath.toAbsolutePath().resolve(ASettings.STATE_FILENAME), AppUtils.toJson(settings).getBytes());
    }
}

enum MainAction{
    init, clone, set, setup, upload, download, save, remove, send, get, status, comment, history, clear, diff,
    apply, erase, head, logs, logsbyname, undo, files, patches, show, delete, delete_remote_patch, accept, settings, eol, commands;
}
class UserSettings {
    @Expose
    String email = "";
    @Expose
    String repo = "";

}