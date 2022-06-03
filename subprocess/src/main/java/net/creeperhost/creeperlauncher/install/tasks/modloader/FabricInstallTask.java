package net.creeperhost.creeperlauncher.install.tasks.modloader;

import net.covers1624.quack.gson.JsonUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by covers1624 on 28/1/22.
 */
public class FabricInstallTask extends ModLoaderInstallTask {

    private static final String META_URL = "https://meta.fabricmc.net/";
    private static final String LOADER_NAME = "fabric-loader";

    private final String mcVersion;
    private final String loaderVersion;

    private final String versionName;

    public FabricInstallTask(String mcVersion, String loaderVersion) {
        this.mcVersion = mcVersion;
        this.loaderVersion = loaderVersion;

        versionName = LOADER_NAME + "-" + mcVersion + "-" + loaderVersion;
    }

    @Override
    public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {
        Path versionsDir = Constants.BIN_LOCATION.resolve("versions");
        Path librariesDir = Constants.BIN_LOCATION.resolve("libraries");

        Path versionJson = versionsDir.resolve(versionName).resolve(versionName + ".json");

        VersionManifest loaderManifest = downloadLoaderManifest(versionJson, cancelToken);
        downloadVanilla(versionsDir, mcVersion);

        for (VersionManifest.Library library : loaderManifest.libraries) {
            if (cancelToken != null) cancelToken.throwIfCancelled();
            processLibrary(cancelToken, librariesDir, library);
        }
    }

    @Nullable
    @Override
    public String getResult() {
        return versionName;
    }

    private void processLibrary(@Nullable CancellationToken token, Path librariesDir, VersionManifest.Library library) throws IOException {
        NewDownloadTask task = library.createDownloadTask(librariesDir, true);
        if (task == null) throw new IOException("Unable to download or locate library: " + library.name);

        if (!task.isRedundant()) {
            task.execute(token, null);
        }
    }

    private VersionManifest downloadLoaderManifest(Path dest, @Nullable CancellationToken cancellationToken) throws IOException {
        NewDownloadTask task = NewDownloadTask.builder()
                .url(META_URL + "v2/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json")
                .dest(dest)
                .build();
        if (!task.isRedundant()) {
            task.execute(cancellationToken, null);
        }

        return JsonUtils.parse(VersionManifest.GSON, dest, VersionManifest.class);
    }
}
