package me.indian.freebedrock;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.indian.freebedrock.util.DownloadAssetUtil;

public class RunFreeBedrock {

    private static final Logger LOGGER = Logger.getLogger(RunFreeBedrock.class.getName());
    private final String takeOwnershipProFile, currentDir;

    public RunFreeBedrock() {
        this.takeOwnershipProFile = "C:\\Program Files (x86)\\TakeOwnershipPro\\TakeOwnershipPro.exe";
        this.currentDir = System.getProperty("user.dir");

        if (!this.isWindows()) {
            LOGGER.log(Level.SEVERE, "Twój system to nie windows!");
            System.exit(0);
            return;
        }
        if (!this.is64Bit()) {
            LOGGER.log(Level.SEVERE, "Twój system nie jest 64 bitowy!");
            System.exit(0);
            return;
        }

        this.downloadAssets();

        final Path sourcePath = Paths.get(this.takeOwnershipProFile);
        if (!Files.exists(sourcePath)) {
            LOGGER.log(Level.SEVERE, "Nie zainstalowałeś TakeOwnershipPro.");

            try {
                this.installOwnership();
            } catch (final IOException | InterruptedException exception) {
                LOGGER.log(Level.SEVERE, "Nie udało się zainstalować TakeOwnershipPro");
            }

            if (!Files.exists(sourcePath)) {
                LOGGER.log(Level.SEVERE, "Nadal nie zainstalowałeś TakeOwnershipPro.");
                System.exit(0);
            }
        }

        this.killAps();
        this.replaceFiles("System32");
        this.replaceFiles("SysWOW64");
    }

    public static void main(final String[] args) {
        new RunFreeBedrock();
    }

    private void downloadAssets() {
        final List<String> assetList = List.of(this.currentDir + "\\System32\\Windows.ApplicationModel.Store.dll",
                this.currentDir + "\\SysWOW64\\Windows.ApplicationModel.Store.dll");

        for (final String asset : assetList) {
            if (!Files.exists(Paths.get(asset))) {
                LOGGER.log(Level.SEVERE, "Nie znaleziono pliku  \"Windows.ApplicationModel.Store.dll\" w aktualnym katalogu z " + asset + " !.");
                try {
                    DownloadAssetUtil.downloadAssets();
                } catch (final IOException exception) {
                    LOGGER.log(Level.SEVERE, "Nie udało się pobrać assetów!", exception);
                }
                return;
            }
        }
    }

    public void replaceFiles(final String folderName) {
        final String destinationFile = "C:\\Windows\\" + folderName + "\\Windows.ApplicationModel.Store.dll";
        final String replacementFile = this.currentDir + "\\" + folderName + "\\Windows.ApplicationModel.Store.dll";

        final Path destinationPath = Paths.get(destinationFile);
        final Path replacementPatch = Paths.get(replacementFile);

        if (!Files.exists(replacementPatch)) {
            LOGGER.log(Level.SEVERE, "Nie znaleziono pliku  \"Windows.ApplicationModel.Store.dll\" w aktualnym katalogu z \"" + this.currentDir + File.separator + folderName + "\" !.");
            return;
        }

        try {
            Runtime.getRuntime().exec(this.takeOwnershipProFile + " " + destinationFile).waitFor();
        } catch (final InterruptedException | IOException exception) {
            LOGGER.log(Level.SEVERE, "Nie udało się operować z TakeOwnershipPro w folderze '" + folderName + ";", exception);
            return;
        }

        if (Files.exists(destinationPath)) {
            try {
                Files.delete(destinationPath);
            } catch (final IOException ioException) {
                LOGGER.log(Level.SEVERE, "Nie można usunać pliku! ", ioException);
                System.exit(0);
            }
        }

        try {
            Files.copy(replacementPatch, destinationPath);
            LOGGER.log(Level.INFO, "Plik w folderze " + folderName + " został skopiowany i podmieniony.");
        } catch (final IOException exception) {
            LOGGER.log(Level.SEVERE, "Nie udało się podmienić pliku w folderze '" + folderName + "'!");
            throw new RuntimeException(exception);
        }
    }

    public void installOwnership() throws InterruptedException, IOException {
        Runtime.getRuntime().exec(this.currentDir + File.separator + "TakeOwnershipPro.exe").waitFor();
    }

    public void killAps() {
        final List<String> appsToKill = List.of("XboxPcApp.exe", "WinStore.App.exe", "Minecraft.Windows.exe");

        for (final String app : appsToKill) {
            try {
                this.killApp(app);
            } catch (final IOException exception) {
                LOGGER.log(Level.SEVERE, "Nie udało się zamknąć aplikacji: '" + app + "'", exception);
            }
        }
    }

    public void killApp(final String processName) throws IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/F", "/IM", processName);
        final Process process = processBuilder.start();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.log(Level.INFO, line);
            }
        }
    }

    public boolean is64Bit() {
        return System.getProperty("os.arch").contains("64");
    }

    public boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}