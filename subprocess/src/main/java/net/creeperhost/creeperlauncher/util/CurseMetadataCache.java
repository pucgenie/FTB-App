package net.creeperhost.creeperlauncher.util;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.data.mod.CurseMetadata;
import net.creeperhost.creeperlauncher.data.mod.ModManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionModsManifest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Created by covers1624 on 13/9/23.
 */
public class CurseMetadataCache {

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Type TYPE = new TypeToken<Map<String, FileMetadata>>() { }.getType();

    private final Path file;
    private final Map<String, FileMetadata> metadata;

    // Cache per-run of failed requests to make things a tiny bit snappier.
    private final Set<String> failedCache = ConcurrentHashMap.newKeySet();

    public CurseMetadataCache(Path file) {
        this.file = file;
        Map<String, FileMetadata> metadata = null;
        if (Files.exists(file)) {
            try {
                metadata = JsonUtils.parse(GSON, file, TYPE);
            } catch (IOException | JsonParseException ex) {
                LOGGER.error("Failed to load storage.", ex);
            }
        }

        if (metadata == null) {
            metadata = new HashMap<>();
        }

        this.metadata = metadata;
    }

    public @Nullable CurseMetadata getCurseMeta(@Nullable ModpackVersionModsManifest.Mod mod, String sha1) {
        if (mod != null) {
            long projectId = mod.getCurseProject();
            long fileId = mod.getCurseFile();
            String name = mod.getName();
            String synopsis = mod.getSynopsis();
            String icon = mod.getIcon();
            return CurseMetadata.full(projectId, fileId, name, synopsis, icon);
        }

        FileMetadata metadata = queryMetadata(sha1);
        if (metadata != null) {
            return metadata.toCurseInfo();
        }
        return null;
    }

    public @Nullable CurseMetadata getCurseMeta(long curseProject, long curseFile, String sha1) {
        ModManifest mod = null;
        try {
            mod = Constants.MOD_VERSION_CACHE.queryMod(curseProject).get();
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.warn("Failed to query mod version.", ex);
        }
        ModManifest.Version version = null;
        if (mod != null) {
            version = mod.findVersion(curseFile);
        }

        if (version == null) return CurseMetadata.full(curseProject, curseFile, null, null, null);

        return CurseMetadata.full(
                curseProject,
                curseFile,
                mod.getName(),
                mod.getSynopsis(),
                !mod.getArt().isEmpty() ? mod.getArt().get(0).getUrl() : null // TODO, this is so dumb.
        );
    }

    /**
     * Find or query metadata for the given file hash.
     *
     * @param sha1 The sha1 to lookup.
     * @return The metadata.
     */
    public @Nullable FileMetadata queryMetadata(String sha1) {
        FileMetadata metadata = findMetadata(sha1);
        if (metadata != null) return metadata;

        if (failedCache.contains(sha1)) return null;

        synchronized (sha1.intern()) {
            metadata = findMetadata(sha1);
            if (metadata != null) return metadata;

            metadata = query(sha1);
            if (metadata == null) {
                failedCache.add(sha1);
            }
            synchronized (this.metadata) {
                this.metadata.put(sha1, metadata);
                save();
            }
            return metadata;
        }
    }

    /**
     * Try and find existing metadata for the given file hash.
     *
     * @param sha1 The sha1.
     * @return The metadata. {@code null} if no metadata exists already.
     */
    public @Nullable FileMetadata findMetadata(String sha1) {
        return metadata.get(sha1);
    }

    private void save() {
        try {
            synchronized (metadata) {
                JsonUtils.write(GSON, file, metadata, TYPE);
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to save storage.", ex);
        }
    }

    private static @Nullable FileMetadata query(String sha1) {
        StringWriter sw = new StringWriter();
        try {
            LOGGER.info("Querying metadata for {}", sha1);
            DownloadAction action = new OkHttpDownloadAction()
                    .setClient(Constants.httpClient())
                    .setUrl(Constants.getModEndpoint() + "lookup/" + sha1)
                    .setDest(sw);
            action.execute();

            FileLookupResponse resp = JsonUtils.parse(GSON, sw.toString(), FileLookupResponse.class);
            if (resp == null) return null;

            return resp.meta;
        } catch (IOException | JsonParseException ex) {
            LOGGER.warn("Failed to query metadata for {}", sha1, ex);
            return null;
        }
    }

    // Only the useful things from /public/mod/lookup/<hash>
    public record FileMetadata(
            long fileId,
            String name,
            String synopsis,
            String icon,
            String curseSlug,
            long curseProject,
            long curseFile,
//            long stored,
            String filename
    ) {

        public CurseMetadata toCurseInfo() {
            return CurseMetadata.full(curseProject, curseFile, name, synopsis, icon);
        }
    }

    public record FileLookupResponse(String status, FileMetadata meta) {
    }
}
