package net.creeperhost.creeperlauncher.api.handlers;

import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.PathTypeAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringJoiner;

public class ModFile {
    private final transient String realName;
    private final String name;
    private final String version;
    private final long size;
    private final String sha1;
    private boolean expected;
    private boolean exists;
    private boolean enabled;

    @JsonAdapter(PathTypeAdapter.class)
    private Path realPath;

    private final transient int hashCode;

    public ModFile(String name, String version, long size, String sha1) {
        this.realName = name;
        this.name = name.replace(".disabled", "");
        this.version = version;
        this.size = size;
        this.sha1 = sha1;
        this.enabled = !realName.endsWith(".disabled");
        this.hashCode = this.realName.toLowerCase().hashCode();
    }

    public ModFile setExpected(boolean expected)  {
        this.expected = expected;
        return this;
    }

    public ModFile setExists(boolean exists) {
        this.exists = exists;
        return this;
    }

    public ModFile setPath(Path path) {
        realPath = path;
        return this;
    }

    public boolean setEnabled(boolean state) {
        if (realPath == null) {
            return false;
        }
        if (enabled == state) {
            return true;
        }
        Path parent = realPath.getParent();
        String oldName = realPath.getFileName().toString();
        String newFileName = state ? oldName.replace(".disabled", "") : oldName + ".disabled";
        Path newPath = parent.resolve(newFileName);
        try {
            Files.move(realPath, newPath);
        } catch (IOException exception) {
            return false;
        }

        enabled = state;
        realPath = newPath;
        return true;
    }

    public String getRealName() {
        return realName;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o.getClass() == this.getClass() && o.hashCode() == this.hashCode();
    }

    public String getSha1() {
        return sha1;
    }

    public long getSize() {
        return size;
    }

    public String getVersion() {
        return version;
    }

    public static boolean isPotentialMod(String name) {
        String replace = name.replace(".disabled", "");
        return replace.endsWith(".jar") || replace.endsWith(".zip");
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ModFile.class.getSimpleName() + "[", "]")
                .add("realName='" + realName + "'")
                .add("name='" + name + "'")
                .add("version='" + version + "'")
                .add("size=" + size)
                .add("sha1='" + sha1 + "'")
                .add("expected=" + expected)
                .add("exists=" + exists)
                .add("enabled=" + enabled)
                .add("realPath=" + realPath)
                .add("hashCode=" + hashCode)
                .toString();
    }
}
