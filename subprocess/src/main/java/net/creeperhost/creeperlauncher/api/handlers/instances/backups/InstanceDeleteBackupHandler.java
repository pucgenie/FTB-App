package net.creeperhost.creeperlauncher.api.handlers.instances.backups;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class InstanceDeleteBackupHandler implements IMessageHandler<InstanceDeleteBackupHandler.Request> {
    public static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    public void handle(Request data) {
        var backupFileLocation = Path.of(data.backupLocation);
        if (Files.notExists(backupFileLocation)) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Unable to locate the backup that was requested to be deleted"));
            return;
        }

        try {
            Files.deleteIfExists(backupFileLocation);
            
            // Update the json file
            var jsonStore = backupFileLocation.getParent().resolve("backups.json");
            if (Files.exists(jsonStore)) {
                InstanceGetBackupsHandler.BackupsJson jsonData = GsonUtils.loadJson(jsonStore, InstanceGetBackupsHandler.BackupsJson.class);
                jsonData.backups = jsonData.backups.stream().filter(e -> !e.backupLocation.equals(data.backupLocation)).toList();
                
                GsonUtils.saveJson(jsonStore, jsonData);
            }
            
            Settings.webSocketAPI.sendMessage(new Reply(data, true, "Deleted %s".formatted(backupFileLocation.getFileName())));
        } catch (IOException e) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Unable to delete %s".formatted(backupFileLocation.getFileName())));
        }
    }
    
    public static class Request extends BaseData {
        public String backupLocation;
    }
    
    private static class Reply extends Request {
        public String message;
        public boolean success;
        
        public Reply(Request data, boolean success, String message) {
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.message = message;
            this.success = success;
        }
    }
}
