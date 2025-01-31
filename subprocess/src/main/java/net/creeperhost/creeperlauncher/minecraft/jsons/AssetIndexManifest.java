package net.creeperhost.creeperlauncher.minecraft.jsons;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static net.creeperhost.creeperlauncher.Constants.JSON_PROXY;

/**
 * Asset index json.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public class AssetIndexManifest {

    private static final Logger LOGGER = LogManager.getLogger();
    public Map<String, AssetObject> objects = new HashMap<>();
    public boolean virtual;
    @SerializedName ("map_to_resources")
    public boolean mapToResources;

    /**
     * Updates (if required) the asset index for the specified version.
     *
     * @param assetsDir  The path to the assets' directory on disk.
     * @param assetIndex The {@link VersionManifest.AssetIndex} to update for.
     * @return The {@link AssetIndexManifest} parsed from disk.
     * @throws IOException        Thrown when an error occurs whilst loading the manifest.
     * @throws JsonParseException Thrown when the Json cannot be parsed.
     */
    public static AssetIndexManifest update(Path assetsDir, VersionManifest.AssetIndex assetIndex) throws IOException {
        Path assetIndexFile = assetsDir.resolve("indexes/" + assetIndex.getId() + ".json");
        LOGGER.info("Updating AssetIndex Manifest for '{}' from '{}'.", assetIndex.getId(), assetIndex.getUrl());

        NewDownloadTask downloadTask = NewDownloadTask.builder()
                .url(assetIndex.getUrl())
                .withMirror(JSON_PROXY + assetIndex.getUrl())
                .dest(assetIndexFile)
                .withValidation(assetIndex.validation()
                        .withUseETag(true)
                        .withUseOnlyIfModified(true)
                )
                .build();

        if (!downloadTask.isRedundant()) {
            try {
                downloadTask.execute(null, null);
            } catch (Throwable e) {
                if (Files.exists(assetIndexFile)) {
                    LOGGER.warn("Failed to update AssetIndexManifest. Continuing with disk cache..", e);
                } else {
                    throw new IOException("Failed to update AssetIndexManifest. Disk cache does not exist.", e);
                }
            }
        }
        return GsonUtils.loadJson(assetIndexFile, AssetIndexManifest.class);
    }

    public static class AssetObject {

        @Nullable
        @JsonAdapter (HashCodeAdapter.class)
        private HashCode hash;
        private int size;

        public String getPath() {
            return getHash().toString().substring(0, 2) + "/" + getHash();
        }

        // @formatter:off
        public HashCode getHash() { return requireNonNull(hash); }
        public int getSize() { return size; }
        // @formatter:on
    }
}
