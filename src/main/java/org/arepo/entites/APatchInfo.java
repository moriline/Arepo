package org.arepo.entites;

import com.google.gson.annotations.Expose;

import java.util.List;

public class APatchInfo {
    @Expose
    public final List<APatchHistory> history;
    @Expose
    public final List<APatches> files;

    public APatchInfo(List<APatchHistory> history, List<APatches> files) {
        this.history = history;
        this.files = files;
    }

    @Override
    public String toString() {
        return "APatchInfo{" +
                "history=" + history +
                ", files=" + files +
                '}';
    }
}
