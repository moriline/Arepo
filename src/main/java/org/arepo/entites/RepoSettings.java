package org.arepo.entites;

import com.google.gson.annotations.Expose;
import org.arepo.common.AppUtils;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class RepoSettings {
    @Expose
    public String remote = "";// If remote is empty - this is root repo!
    @Expose
    public boolean useFS = true;
    @Expose
    public Integer eol = 0;
    @Expose
    public String email = "";
    @Expose
    public Long hash = 0L; //unique hash for every repo!
    @Expose
    public Set<String> ignoreDirs = new HashSet<String>(); // ignore all files in directory like this: Set.of(".gradle", "build", ".idea");
    @Expose
    public Set<String> ignoreFiles = new HashSet<String>();// ignore files like this: Set.of("readme.md", "src/One.java");
    @Expose
    public Long version = 0L;
    public RepoSettings(Path path, String email, Integer eol) {
        //this.hash = AppCheckSum.getCRC32Checksum(new StringBuilder(email).append(path.toAbsolutePath().toString()).toString().getBytes(StandardCharsets.UTF_8));
        this.hash = AppUtils.getCRC32Checksum(new StringBuilder(email).append(System.nanoTime()).toString().getBytes());
        this.eol = eol;
        this.email = email;
    }

    public static void main(String[] args) throws MalformedURLException {
        var repo = new RepoSettings(Path.of("bin/repi1"), "root@mail.net", 1);
        var repo2 = new RepoSettings(Path.of("bin/repi2"), "root@mail.net", 1);
        System.out.println("repo 1:"+repo.toString());
        System.out.println("repo 2:"+repo2.toString());

    }

    @Override
    public String toString() {
        return "RepoSettings{" +
                "remote='" + remote + '\'' +
                ", useFS=" + useFS +
                ", eol=" + eol +
                ", email='" + email + '\'' +
                ", hash=" + hash +
                ", ignoreDirs=" + ignoreDirs +
                ", ignoreFiles=" + ignoreFiles +
                '}';
    }
}
