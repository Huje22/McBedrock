package pl.indianbartonka.freebedrock;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeoutException;
import pl.indianbartonka.freebedrock.util.DownloadAssetUtil;
import pl.indianbartonka.util.logger.Logger;
import pl.indianbartonka.util.logger.config.LoggerConfiguration;

public class RunFreeBedrock {

    //TODO: Zrób rewrite kodu nie zapomnij o metodach usuwania z FileUtil
    private final LoggerConfiguration configuration;
    private final Logger logger;
    private final String takeOwnershipProFile, currentDir;

    public RunFreeBedrock() {
        this.takeOwnershipProFile = "C:\\Program Files (x86)\\TakeOwnershipPro\\TakeOwnershipPro.exe";
        this.currentDir = System.getProperty("user.dir");
        this.configuration = LoggerConfiguration.builder().build();
        this.logger = new Logger(this.configuration) {
        };

        if (!this.isWindows()) {
            this.logger.critical("Twój system to nie windows!");
            System.exit(0);
            return;
        }
        if (!this.is64Bit()) {
            this.logger.critical("Twój system nie jest 64 bitowy!");
            System.exit(0);
            return;
        }

        DownloadAssetUtil.setLogger(this.logger);
        this.downloadAssets();

        final Path sourcePath = Paths.get(this.takeOwnershipProFile);
        if (!Files.exists(sourcePath)) {
            this.logger.critical("Nie zainstalowałeś TakeOwnershipPro.");

            try {
                this.installOwnership();
            } catch (final IOException | InterruptedException exception) {
                this.logger.critical("Nie udało się zainstalować TakeOwnershipPro");
            }

            if (!Files.exists(sourcePath)) {
                this.logger.critical("Nadal nie zainstalowałeś TakeOwnershipPro.");
                System.exit(0);
            }
        }

        this.killAps();
        this.replaceFiles("System32");
        this.replaceFiles("SysWOW64");

        try {
            this.runMinecraft();
        } catch (final IOException | InterruptedException exception) {
            this.logger.error("Nie udało się uruchomić minecraft!", exception);
        }
    }

    public static void main(final String[] args) {
        new RunFreeBedrock();
    }

    private void downloadAssets() {
        final List<String> assetList = List.of(this.currentDir + "\\System32\\Windows.ApplicationModel.Store.dll",
                this.currentDir + "\\SysWOW64\\Windows.ApplicationModel.Store.dll");

        for (final String asset : assetList) {
            if (!Files.exists(Paths.get(asset))) {
                this.logger.critical("Nie znaleziono pliku  \"Windows.ApplicationModel.Store.dll\" w aktualnym katalogu z " + asset + " !.");
                try {
                    DownloadAssetUtil.downloadAssets();
                } catch (final IOException | TimeoutException exception) {
                    this.logger.critical("Nie udało się pobrać assetów!", exception);
                    System.exit(0);
                }
                return;
            }
        }
    }

    private void replaceFiles(final String folderName) {
        final String destinationFile = "C:\\Windows\\" + folderName + "\\Windows.ApplicationModel.Store.dll";
        final String replacementFile = this.currentDir + "\\" + folderName + "\\Windows.ApplicationModel.Store.dll";

        final Path destinationPath = Paths.get(destinationFile);
        final Path replacementPatch = Paths.get(replacementFile);

        if (!Files.exists(replacementPatch)) {
            this.logger.critical("Nie znaleziono pliku  \"Windows.ApplicationModel.Store.dll\" w aktualnym katalogu z \"" + this.currentDir + File.separator + folderName + "\" !.");
            return;
        }

        try {
            Runtime.getRuntime().exec(this.takeOwnershipProFile + " " + destinationFile).waitFor();
        } catch (final InterruptedException | IOException exception) {
            this.logger.critical("Nie udało się operować z TakeOwnershipPro w folderze '" + folderName + ";", exception);
            return;
        }

        if (Files.exists(destinationPath)) {
            try {
                Files.delete(destinationPath);
            } catch (final IOException ioException) {
                this.logger.critical("Nie można usunać pliku! ", ioException);
                System.exit(0);
            }
        }

        try {
            Files.copy(replacementPatch, destinationPath);
            this.logger.info("Plik w folderze " + folderName + " został skopiowany i podmieniony.");
        } catch (final IOException exception) {
            this.logger.critical("Nie udało się podmienić pliku w folderze '" + folderName + "'!");
            throw new RuntimeException(exception);
        }
    }

    private void installOwnership() throws InterruptedException, IOException {
        Runtime.getRuntime().exec(this.currentDir + File.separator + "TakeOwnershipPro.exe").waitFor();
    }

    private void runMinecraft() throws InterruptedException, IOException {
        Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "start minecraft:"}).waitFor();
    }

    private void killAps() {
        final List<String> appsToKill = List.of("XboxPcApp.exe", "WinStore.App.exe", "Minecraft.Windows.exe");

        for (final String app : appsToKill) {
            try {
                this.killApp(app);
            } catch (final IOException exception) {
                this.logger.critical("Nie udało się zamknąć aplikacji: '" + app + "'", exception);
            }
        }
    }

    private void killApp(final String processName) throws IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/F", "/IM", processName);
        final Process process = processBuilder.start();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                this.logger.info(line);
            }
        }
    }

    private boolean is64Bit() {
        return System.getProperty("os.arch").contains("64");
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
