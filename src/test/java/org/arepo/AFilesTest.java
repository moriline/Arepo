package org.arepo;

import org.arepo.common.ASettings;
import org.arepo.common.FSUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AFilesTest {
    String rootRepo = "root_repo";
    String cloneRepo = "clone_repo";
    String otherCloneRepo = "other_clone_repo";
    static String A = ASettings.defindedPatches.get(0);
    static String B = ASettings.defindedPatches.get(1);
    String email = "test@mail.net";
    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() throws IOException {
        FSUtils.clearDirRecursive(Path.of(rootRepo));
        FSUtils.clearDirRecursive(Path.of(cloneRepo));
        FSUtils.clearDirRecursive(Path.of(otherCloneRepo));
    }
    @Test
    void clearFiles() throws Exception {
        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);

        Files.createDirectories(current);
        var afiles = new AFiles(rootRepo);
        var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        Files.createDirectories(rootPath.resolve(cloneRepo));

        afiles = new AFiles(cloneRepo);
        settings = afiles.setup(rootRepo, email, eol, false);
        assertTrue(settings.useFS);
        var status = afiles.status();
        assertTrue(status.getAddedFiles().isEmpty());
        var one = "index.php";
        var two = "sub/folder/somefile.txt";
        var three = "main.java";
        var four = "subdir/Files.java";
        rootPath.resolve(cloneRepo).resolve(two).toFile().getParentFile().mkdirs();
        rootPath.resolve(cloneRepo).resolve(four).toFile().getParentFile().mkdirs();

        Files.write(rootPath.resolve(cloneRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(cloneRepo).resolve(two), "2 clone".getBytes());
        Files.write(rootPath.resolve(cloneRepo).resolve(three), "3 clone".getBytes());
        Files.write(rootPath.resolve(cloneRepo).resolve(four), "4 clone".getBytes());

        settings = afiles.updateSettings(Set.of("sub"), Set.of());
        assertFalse(settings.ignoreDirs.isEmpty());
        assertTrue(settings.ignoreFiles.isEmpty());

        settings = afiles.updateSettings(Set.of(), Set.of(one));
        assertTrue(settings.ignoreDirs.isEmpty());
        assertFalse(settings.ignoreFiles.isEmpty());

        settings = afiles.updateSettings(Set.of("sub"), Set.of(one));
        assertFalse(settings.ignoreDirs.isEmpty());
        assertFalse(settings.ignoreFiles.isEmpty());

        afiles.clearFS();
        assertTrue(Files.notExists(rootPath.resolve(cloneRepo).resolve(three)));
        assertTrue(Files.exists(rootPath.resolve(cloneRepo).resolve(two)));
        assertTrue(Files.exists(rootPath.resolve(cloneRepo).resolve(one)));
        assertTrue(Files.exists(rootPath.resolve(cloneRepo).resolve(four)));
    }

    @Test
    void binaryFilesLogsRestore() throws Exception {
        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);
        Files.createDirectories(current);
        var afiles = new AFiles(rootRepo);
        var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        Files.createDirectories(rootPath.resolve(cloneRepo));
        Files.createDirectories(rootPath.resolve(otherCloneRepo));

        afiles = new AFiles(cloneRepo);
        settings = afiles.setup(rootRepo, email, eol, false);
        assertTrue(settings.useFS);
        var status = afiles.status();
        assertTrue(status.getAddedFiles().isEmpty());

        Path resourceDirectory = Paths.get("src","test","resources");
        var one = "brand.jpg";
        var two = "grails-icon.png";
        var three = "site.ico";

        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(cloneRepo).resolve(one));
        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(cloneRepo).resolve(two));

        var savedFiles = afiles.save(A, Set.of(one, two));
        System.out.println("saved 1:"+savedFiles);
        afiles.setComment(A, "one comment for clone A");
        var diff = afiles.diff(A);
        assertTrue(diff.isEmpty());

        afiles.clearFS();
        diff = afiles.diff(A);
        assertEquals(2, diff.size());
        assertTrue(diff.containsKey(one));
        assertTrue(diff.containsKey(two));

        var upload = afiles.upload(A);
        System.out.println("upload 1:"+upload);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();

        Files.copy(resourceDirectory.resolve(three), rootPath.resolve(cloneRepo).resolve(three));
        status = afiles.status();
        System.out.println("st 2:"+status);
        savedFiles = afiles.save(A, Set.of(three));
        System.out.println("saved 2:"+savedFiles);
        afiles.setComment(A, "two comment for clone A");
        upload = afiles.upload(A);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();

        status = afiles.status();
        System.out.println("st 3:"+status);

        Files.copy(resourceDirectory.resolve(three), rootPath.resolve(cloneRepo).resolve(one), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(cloneRepo).resolve(three), StandardCopyOption.REPLACE_EXISTING);

        status = afiles.status();
        System.out.println("st 4:"+status);
        savedFiles = afiles.save(A, Set.of(three, one));
        System.out.println("saved 3:"+savedFiles);
        afiles.setComment(A, "three comment for clone A");
        upload = afiles.upload(A);
        System.out.println("upload 3:"+upload);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();
        status = afiles.status();
        System.out.println("st 5:"+status);
        var logs = afiles.getLogs();
        System.out.println("logs:"+logs);
        assertEquals(3, logs.size());
        var files = afiles.getFiles();
        System.out.println("files:"+files);
        assertEquals(3, files.size());

        var pastLogs = afiles.applyFSByLogsByCountLimit(1);
        System.out.println("past history:"+pastLogs);
        assertEquals(1, pastLogs.size());

    }
    @Test
    void binaryFiles() throws Exception {

        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);
        Files.createDirectories(current);
        var afiles = new AFiles(rootRepo);
        var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        Files.createDirectories(rootPath.resolve(cloneRepo));
        Files.createDirectories(rootPath.resolve(otherCloneRepo));

        afiles = new AFiles(cloneRepo);
        settings = afiles.setup(rootRepo, email, eol, false);
        assertTrue(settings.useFS);
        var status = afiles.status();
        assertTrue(status.getAddedFiles().isEmpty());

        Path resourceDirectory = Paths.get("src","test","resources");
        String absolutePath = resourceDirectory.toFile().getAbsolutePath();
        var sub = "images";
        var one = "brand.jpg";
        var two = "grails.svg";
        Files.createDirectory(rootPath.resolve(cloneRepo).resolve(sub));
        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(cloneRepo).resolve(one));
        Files.copy(resourceDirectory.resolve(two), rootPath.resolve(cloneRepo).resolve(sub).resolve(two));

        status = afiles.status();
        System.out.println("st 1:"+status);
        var savedFiles = afiles.save(A, Set.of(one, sub+"/"+two));
        System.out.println("saved 1:"+savedFiles);
        afiles.setComment(A, "first comment for clone A");
        var history = afiles.showPatch(A);
        System.out.println("history 1:"+history);
        assertEquals(2, history.history.size());

        var upload = afiles.upload(A);
        System.out.println("upload:"+upload);
        TimeUnit.SECONDS.sleep(1);
        afiles.clearFS();
        afiles.download();
        afiles.clearFS();
        status = afiles.status();
        System.out.println("st 2:"+status);
        var logs = afiles.getLogs();
        assertEquals(1, logs.size());

        var three = "grails-icon.png";
        Files.copy(resourceDirectory.resolve(three), rootPath.resolve(cloneRepo).resolve(one), StandardCopyOption.REPLACE_EXISTING);
        status = afiles.status();
        System.out.println("st 3:"+status);
        savedFiles = afiles.save(A, Set.of(one));
        System.out.println("saved 2:"+savedFiles);

        afiles.setComment(A, "second comment for clone A");
        upload = afiles.upload(A);
        System.out.println("upload 2:"+upload);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();
        status = afiles.status();
        System.out.println("st 4:"+status);
        Files.deleteIfExists(rootPath.resolve(cloneRepo).resolve(one));

        status = afiles.status();
        System.out.println("st 5:"+status);
        savedFiles = afiles.save(A, Set.of(one));
        System.out.println("saved 3:"+savedFiles);
        afiles.setComment(A, "three comment for clone A");
        TimeUnit.SECONDS.sleep(1);
        upload = afiles.upload(A);
        System.out.println("upload 3:"+upload);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();
        status = afiles.status();
        System.out.println("st 6:"+status);

        logs = afiles.getLogs();
        System.out.println("logs:"+logs);
        assertEquals(3, logs.size());
        var files = afiles.getFiles();
        System.out.println("files:"+files);

        Files.copy(resourceDirectory.resolve(one), rootPath.resolve(cloneRepo).resolve(one));
        savedFiles = afiles.save(A, Set.of(one));
        System.out.println("saved 4:"+savedFiles);
        afiles.setComment(A, "restore file for A");
        upload = afiles.upload(A);
        System.out.println("upload 4:"+upload);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();

        logs = afiles.getLogs();
        System.out.println("logs:"+logs);
        assertEquals(4, logs.size());
        files = afiles.getFiles();
        System.out.println("files:"+files);
        assertEquals(3, files.size());

        var foundFiles = afiles.getLogsByFilename("somefile.txt");
        assertTrue(foundFiles.isEmpty());
        foundFiles = afiles.getLogsByFilename(one);
        assertEquals(1, foundFiles.size());

    }
    @Test
    void sendPatch() throws Exception {
        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);
        Files.createDirectories(current);
        var afiles = new AFiles(rootRepo);
        var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        Files.createDirectories(rootPath.resolve(cloneRepo));
        Files.createDirectories(rootPath.resolve(otherCloneRepo));

        afiles = new AFiles(cloneRepo);
        settings = afiles.setup(rootRepo, email, eol, false);
        assertTrue(settings.useFS);
        var status = afiles.status();
        assertTrue(status.getAddedFiles().isEmpty());
        var one = "One.java";
        var two = "Two.txt";
        var three = "Three.java";
        Files.write(rootPath.resolve(cloneRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(cloneRepo).resolve(two), "2 clone".getBytes());
        Files.write(rootPath.resolve(cloneRepo).resolve(three), "3 clone".getBytes());
        status = afiles.status();
        assertFalse(status.getAddedFiles().isEmpty());

        afiles.save(A, Set.of(one, two, three));
        afiles.setComment(A, "comment for clone");
        var patchname = afiles.sendPatch(A);

        afiles = new AFiles(otherCloneRepo);
        settings = afiles.setup(rootRepo, email, eol, false);
        assertTrue(settings.useFS);
        afiles.getPatch(patchname);
        assertEquals(4, afiles.patches().size());
        var patchfiles = afiles.showPatch(patchname);
        assertEquals(3, patchfiles.files.size());
        afiles.applyPatch2FS(patchname);
        afiles.clearPatch(patchname);
        afiles.deleteLocalPatch(patchname);
        afiles.deleteRemotePatch(patchname);

        Files.write(rootPath.resolve(cloneRepo).resolve(one), "1111 clone".getBytes());
        Files.write(rootPath.resolve(cloneRepo).resolve(two), "2222 clone".getBytes());
        afiles.save(A, Set.of(one, two));
        afiles.setComment(A, "comment for clone 222");

        var upload = afiles.upload(A);
        System.out.println("upload:"+upload);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();
        TimeUnit.SECONDS.sleep(1);
        var logs = afiles.getLogs();
        assertEquals(1, logs.size());
        var textFiles = afiles.getFiles();
        assertEquals(2, textFiles.size());

    }
    @Test
    void remotePatchAccept() throws Exception {
        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);
        Files.createDirectories(current);
        var afiles = new AFiles(rootRepo);
        var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        Files.createDirectories(rootPath.resolve(cloneRepo));
        Files.createDirectories(rootPath.resolve(otherCloneRepo));

        afiles = new AFiles(cloneRepo);
        settings = afiles.setup(rootRepo, email, eol, false);
        assertTrue(settings.useFS);
        var status = afiles.status();
        assertTrue(status.getAddedFiles().isEmpty());

        var one = "One.java";
        var two = "Two.txt";
        Files.write(rootPath.resolve(cloneRepo).resolve(one), "1 clone".getBytes());
        Files.write(rootPath.resolve(cloneRepo).resolve(two), "2 clone".getBytes());
        status = afiles.status();
        assertFalse(status.getAddedFiles().isEmpty());

        afiles.save(A, Set.of(one, two));
        afiles.setComment(A, "comment for clone");
        var patchname = afiles.sendPatch(A);

        afiles = new AFiles(otherCloneRepo);
        settings = afiles.setup(rootRepo, email, eol, false);
        assertTrue(settings.useFS);
        afiles.download();
        TimeUnit.SECONDS.sleep(1);
        var logs = afiles.getLogs();
        assertEquals(0, logs.size());
        var textFiles = afiles.getFiles();
        assertEquals(0, textFiles.size());

        afiles.getPatch(patchname);
        assertEquals(4, afiles.patches().size());
        var patchfiles = afiles.showPatch(patchname);
        assertEquals(2, patchfiles.files.size());
        TimeUnit.SECONDS.sleep(1);

        assertThrows(Exception.class, ()-> {
            var afiles2 = new AFiles(otherCloneRepo);
            afiles2.accept(email+"_121212");
        });
        afiles.accept(patchname);

        TimeUnit.SECONDS.sleep(1);
        afiles.download();
        TimeUnit.SECONDS.sleep(1);
        logs = afiles.getLogs();
        assertEquals(1, logs.size());
        textFiles = afiles.getFiles();
        assertEquals(2, textFiles.size());

        Files.write(rootPath.resolve(otherCloneRepo).resolve(one), "111 clone".getBytes());
        status = afiles.status();
        assertTrue(status.getAddedFiles().isEmpty());
        assertFalse(status.getModifiedFiles().isEmpty());
        afiles.save(A, Set.of(one));
        afiles.setComment(A, "comment for 2 patch");
        patchname = afiles.sendPatch(A);
        afiles.accept(patchname);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();
        TimeUnit.SECONDS.sleep(1);
        logs = afiles.getLogs();
        assertEquals(2, logs.size());
        textFiles = afiles.getFiles();
        assertEquals(2, textFiles.size());
    }
    @Test
    void twoUsers() throws Exception {
        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);
        var email2 = "user@mail.net";
        Files.createDirectories(current);
        var afiles = new AFiles(rootRepo);
        var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
        assertFalse(settings.useFS);
        Files.createDirectories(rootPath.resolve(cloneRepo));

        afiles = new AFiles(cloneRepo);
        settings = afiles.setup(rootRepo, email2, eol, false);
        assertTrue(settings.useFS);
        var status = afiles.status();
        assertTrue(status.getAddedFiles().isEmpty());
        settings = afiles.updateSettings(Set.of(".somefolder"), Set.of("bin/readme.md", ".settings"));
        assertEquals(1, settings.ignoreDirs.size());
        assertEquals(2, settings.ignoreFiles.size());


        var one = "One.java";
        var two = "Two.txt";
        var three = "Three.java";
        Files.write(rootPath.resolve(cloneRepo).resolve(one), "one clone".getBytes());
        Files.write(rootPath.resolve(cloneRepo).resolve(two), "two clone".getBytes());
        status = afiles.status();
        assertFalse(status.getAddedFiles().isEmpty());

        afiles.save(A, Set.of(one, two));
        afiles.setComment(A, "first comment for clone");
        var comment = afiles.getComment(A);
        System.out.println("comm 1:"+comment);

        var content3 = "three clone";
        Files.write(rootPath.resolve(cloneRepo).resolve(three), content3.getBytes());
        afiles.save(B, Set.of(three));
        afiles.setComment(B, "second comment for clone");

        afiles.clearFS();

        afiles.upload(B);
        TimeUnit.SECONDS.sleep(1);
        afiles.upload(A);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();

        afiles.clearAndHead();

        status = afiles.status();

        System.out.println("st 2:"+status);
        var content4 = "222";
        Files.write(rootPath.resolve(cloneRepo).resolve(three), content4.getBytes());
        status = afiles.status();
        System.out.println("st 3:"+status);
        assertFalse(status.getModifiedFiles().isEmpty());

        var savedFiles = afiles.save(B, Set.of(three));
        afiles.setComment(B, "three comment for B");
        System.out.println("saved 3:"+savedFiles);
        afiles.clearAndHead();
        assertEquals(content3, Files.readString(rootPath.resolve(cloneRepo).resolve(three)));

        var diff = afiles.diff(B);
        assertFalse(diff.isEmpty());
        assertTrue(diff.containsKey(three));

        var patchFiles = afiles.applyPatch2FS(B);
        assertEquals(content4, Files.readString(rootPath.resolve(cloneRepo).resolve(three)));
        var upload = afiles.upload(B);
        System.out.println("upload 3:"+upload);
        TimeUnit.SECONDS.sleep(1);
        afiles.download();
        status = afiles.status();
        System.out.println("st 4:"+status);
        var content5 = "333";
        Files.write(rootPath.resolve(cloneRepo).resolve(two), content5.getBytes());
        status = afiles.status();
        System.out.println("st 5:"+status);
        assertFalse(status.getModifiedFiles().isEmpty());

        savedFiles = afiles.save(B, Set.of(two));
        System.out.println("saved 4:"+savedFiles);
        TimeUnit.SECONDS.sleep(1);
        Files.deleteIfExists(rootPath.resolve(cloneRepo).resolve(two));
        status = afiles.status();
        System.out.println("st 6:"+status);

        assertFalse(status.getDeletedFiles().isEmpty());
        diff = afiles.diff(B);
        assertTrue(diff.containsKey(two));
        savedFiles = afiles.save(B, Set.of(two));

        System.out.println("saved 5:"+savedFiles);
        var history = afiles.showPatch(B);
        assertEquals(2, history.history.size());
        assertFalse(Files.exists(rootPath.resolve(cloneRepo).resolve(two)));
        System.out.println("history:"+history);

        var historyItem = history.history.iterator().next();
        afiles.applyHistory2FS(B, historyItem.updated);

        assertTrue(Files.exists(rootPath.resolve(cloneRepo).resolve(two)));

        var logs = afiles.getLogs();
        var textFiles = afiles.getFiles();
        assertFalse(logs.isEmpty());
        assertFalse(textFiles.isEmpty());
    }
    @Test
    void errors() throws Exception {
        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);
        Files.createDirectories(current);

        assertThrows(Exception.class, ()->{
            var afiles = new AFiles(rootRepo);
            var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
            var clonePath = rootPath.resolve(cloneRepo);
            //Files.createDirectories(clonePath);
            afiles = new AFiles(cloneRepo);
            settings = afiles.setup(rootRepo, email, eol, false);

        });
        assertThrows(Exception.class, ()->{
            var afiles = new AFiles(rootRepo);
            var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
            var clonePath = rootPath.resolve(cloneRepo);
            Files.createDirectories(clonePath);
            afiles = new AFiles(cloneRepo);
            settings = afiles.setup(rootRepo, email, eol, false);
            System.out.println("clone settings:"+settings);
            afiles.save("Wrong name", Set.of());
        });
        assertThrows(Exception.class, ()->{
            var afiles = new AFiles(rootRepo);
            var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
            var clonePath = rootPath.resolve(cloneRepo);
            Files.createDirectories(clonePath);
            afiles = new AFiles(cloneRepo);
            settings = afiles.setup(rootRepo, email, eol, false);
            System.out.println("clone settings:"+settings);
            afiles.save(A, Set.of());

            var comment = afiles.getComment("Wrong name");
            System.out.println("comm 1:"+comment);
        });
        assertThrows(Exception.class, ()->{
            var afiles = new AFiles(rootRepo);
            var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);
            var clonePath = rootPath.resolve(cloneRepo);
            Files.createDirectories(clonePath);
            afiles = new AFiles(cloneRepo);
            settings = afiles.setup(rootRepo, email, eol, false);
            System.out.println("clone settings:"+settings);
            afiles.setComment("Wrong name", "first comment for clone");
        });
        assertThrows(Exception.class, ()->{
            var afiles = new AFiles(cloneRepo);
            var history = afiles.showPatch("Wrong name");
        });
    }
    @Test
    void one() throws Exception {
        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);
        Files.createDirectories(current);
        var afiles = new AFiles(rootRepo);
        var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);

        var clonePath = rootPath.resolve(cloneRepo);

        Files.createDirectories(clonePath);
        afiles = new AFiles(cloneRepo);
        settings = afiles.setup(rootRepo, email, eol, false);
        System.out.println("clone settings:"+settings);
        var one = "One.java";
        Files.write(clonePath.resolve(one), "one clone".getBytes());


        var status = afiles.status();
        System.out.println("st 1:"+status);

        afiles.save(A, Set.of(one));
        afiles.setComment(A, "first comment for clone");
        var comment = afiles.getComment(A);
        System.out.println("comm 1:"+comment);

        var history = afiles.showPatch(A);
        System.out.println("history:"+history);
        assertEquals(1, history.history.size());
        status = afiles.status();
        System.out.println("st 2:"+status);
        var two = "Two.java";
        Files.write(clonePath.resolve(two), "2 clone".getBytes());
        var three = "Three.java";
        Files.write(clonePath.resolve(three), "33 clone".getBytes());
        afiles.save(A, Set.of(two, three));
        status = afiles.status();
        System.out.println("st 3:"+status);

        assertThrows(Exception.class, () -> {
            var afiles2 = new AFiles(cloneRepo);
            var patchFiles = afiles2.showPatch(A);
            System.out.println("show 1:"+patchFiles);
            afiles2.remove(A, Set.of(one));
            patchFiles = afiles2.showPatch(A);
            assertEquals(2, patchFiles.files.size());
            afiles2.remove(A, Set.of(two, three));
            patchFiles = afiles2.showPatch(A);
            assertEquals(0, patchFiles.files.size());
            var comment2 = afiles2.getComment(A);
            assertTrue(comment2.isEmpty());
            afiles2.upload(A);
        });

        afiles.save(A, Set.of(one, two));
        afiles.setComment(A, "first comment for clone");
        status = afiles.status();
        System.out.println("st 4:"+status);
        var upload = afiles.upload(A);
        System.out.println("upload:"+upload);
        afiles.download();
        status = afiles.status();
        System.out.println("st 5:"+status);

        Files.write(clonePath.resolve(one), "new content for one".getBytes());
        status = afiles.status();
        System.out.println("st 6:"+status);
        afiles.save(B, Set.of(one, three));
        afiles.setComment(B, "second comment for clone");
        TimeUnit.SECONDS.sleep(1);
        upload = afiles.upload(B);
        afiles.download();
        status = afiles.status();
        System.out.println("st 7:"+status);
        // other clone repo
        var otherClone = rootPath.resolve(otherCloneRepo);
        Files.createDirectories(otherClone);
        afiles = new AFiles(otherCloneRepo);
        settings = afiles.setup(rootRepo, email, eol, false);
        System.out.println("clone settings 2:"+settings);

        afiles.download();
        afiles.clearAndHead();
        status = afiles.status();
        System.out.println("st 8:"+status);

        System.out.println("logs:"+afiles.getLogs());
        var logFiles = afiles.getFiles();
        System.out.println("files:"+logFiles);
        Long time = 0L;
        for(var it : logFiles) {
            time = it.getVersion();break;
        }
        var pastLogs = afiles.applyFSByLogsByCountLimit(1);
        System.out.println("past history:"+time+";"+pastLogs);
        assertEquals(1, pastLogs.size());

    }
    @Test
    void cloneFromClone() throws Exception {
        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);
        Files.createDirectories(current);
        var afiles = new AFiles(rootRepo);
        var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);

        var one = "One.java";
        var clonePath = rootPath.resolve(cloneRepo);

        Files.createDirectories(clonePath);
        var afiles2 = new AFiles(cloneRepo);
        settings = afiles2.setup(rootRepo, email, eol, false);
        System.out.println("clone settings:"+settings);

        Files.write(clonePath.resolve(one), "222".getBytes());
        var status = afiles2.status();
        assertEquals(1, status.getAddedFiles().size());

        var otherClone = rootPath.resolve(otherCloneRepo);
        Files.createDirectories(rootPath.resolve(otherCloneRepo));
        assertThrows(Exception.class, ()->{
            var afiles3 = new AFiles(otherCloneRepo);
            var settings3 = afiles3.setup(cloneRepo, email, eol, false);
            System.out.println("clone settings 2:"+settings3);

            Files.write(otherClone.resolve(one), "333".getBytes());
            var status3 = afiles3.status();
            assertEquals(1, status3.getAddedFiles().size());
            afiles3.save(A, Set.of(one));
            afiles3.setComment(A, "first comment for other clone");
            afiles3.upload(A);
            TimeUnit.SECONDS.sleep(1);
            afiles3.download();
        });
        TimeUnit.SECONDS.sleep(1);
        afiles2.download();
    }
    @Test
    void shortLogs() throws Exception {
        var rootPath = Paths.get("").toAbsolutePath();
        var eol = 1;
        var current = rootPath.resolve(rootRepo);
        Files.createDirectories(current);
        var afiles = new AFiles(rootRepo);
        var settings = afiles.setup(ASettings.ROOT_PREFIX, email, eol, false);

        var one = "One.java";
        var clonePath = rootPath.resolve(cloneRepo);

        Files.createDirectories(clonePath);
        var afiles2 = new AFiles(cloneRepo);
        settings = afiles2.setup(rootRepo, email, eol, false);
        System.out.println("clone settings:"+settings);

        Files.write(clonePath.resolve(one), "111".getBytes());
        var status = afiles2.status();
        assertEquals(1, status.getAddedFiles().size());

        afiles2.save(A, Set.of(one));
        afiles2.setComment(A, "1 comment");
        afiles2.upload(A);
        TimeUnit.SECONDS.sleep(1);
        afiles2.download();
        TimeUnit.SECONDS.sleep(1);
        Files.write(clonePath.resolve(one), "222".getBytes());
        status = afiles2.status();
        assertEquals(1, status.getModifiedFiles().size());

        afiles2.save(B, Set.of(one));
        afiles2.setComment(B, "2 comment");
        afiles2.upload(B);
        TimeUnit.SECONDS.sleep(1);
        afiles2.download();
        TimeUnit.SECONDS.sleep(1);

        var otherClone = rootPath.resolve(otherCloneRepo);
        Files.createDirectories(rootPath.resolve(otherCloneRepo));

        var afiles3 = new AFiles(otherCloneRepo);
        var settings3 = afiles3.setup(rootRepo, email, eol, true);
        System.out.println("clone settings 2:"+settings3);
        //afiles3.download();
        var logs = afiles3.getLogs();
        var files = afiles3.getFiles();
        assertEquals(1, logs.size());
        assertEquals(1, files.size());
    }
}