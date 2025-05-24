package pl.indianbartonka.freebedrock;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import pl.indianbartonka.util.FileUtil;
import pl.indianbartonka.util.MessageUtil;
import pl.indianbartonka.util.ThreadUtil;
import pl.indianbartonka.util.logger.Logger;
import pl.indianbartonka.util.logger.config.LoggerConfiguration;
import pl.indianbartonka.util.system.SystemArch;
import pl.indianbartonka.util.system.SystemOS;
import pl.indianbartonka.util.system.SystemUtil;

public class McBedrock {

    private final JFrame frame;
    private final ProgressPanel progressBar;
    private final Logger logger;
    private final DownloadAssetUtil downloadAssetUtil;
    private final String takeOnwershipProExe;
    private final String currentDir;
    private final SystemOS systemOS;
    private final SystemArch systemArch;
    private boolean supportedSystem;

    public McBedrock() {
        this.frame = new JFrame("Free MC Bedrock");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setSize(500, 250);
        this.frame.setLocationRelativeTo(null);
        this.frame.setResizable(false);
        this.frame.setAlwaysOnTop(true);

        this.progressBar = new ProgressPanel();

        this.logger = new Logger(LoggerConfiguration.builder().setOneLog(true).setLoggingToFile(true).build()) {
        };

        this.downloadAssetUtil = new DownloadAssetUtil(this.logger, this.progressBar);
        this.takeOnwershipProExe = "C:\\Program Files (x86)\\TakeOwnershipPro\\TakeOwnershipPro.exe";
        this.currentDir = System.getProperty("user.dir");
        this.systemOS = SystemUtil.getSystem();
        this.systemArch = SystemUtil.getCurrentArch();
        this.supportedSystem = true;

        SwingUtilities.invokeLater(this::runGui);
    }

    public static void main(final String[] args) {
        new McBedrock();
    }

    private void runGui() {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final JPanel jPanel = new JPanel();

        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        jPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel system = new JLabel("System: " + System.getProperty("os.name"));
        final JLabel arch = new JLabel("Architektura: " + System.getProperty("os.arch"));
        final JLabel processor = new JLabel("Procesor: *WYKRYWANIE*");
        final JLabel graphicCardsLabel = new JLabel("Karty Graficzne: *WYKRYWANIE*");
        final JLabel note = new JLabel("Uwagi: " + this.getSystemNote());

        final JButton jButton = new JButton("Uruchom");

        jButton.setEnabled(this.supportedSystem);
        jButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        jButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        jButton.addActionListener(listener -> {
            this.frame.setLocation(screenSize.width - this.frame.getWidth(), 0);
            this.run();
        });

        jPanel.add(system);
        jPanel.add(arch);
        jPanel.add(processor);
        jPanel.add(graphicCardsLabel);
        jPanel.add(note);
        jPanel.add(Box.createVerticalGlue());
        jPanel.add(this.progressBar);
        jPanel.add(jButton);

        this.frame.add(jPanel, BorderLayout.CENTER);
        this.frame.setVisible(true);

        new ThreadUtil("Detect").newThread(() -> {
            final String processorName = SystemUtil.getProcesorName();
            final String graphicCards = MessageUtil.stringListToString(SystemUtil.getGraphicCardsName(), " | ");

            this.logger.info("&aProcesor:&b " + processorName);
            this.logger.info("&aKarty Graficzne:&b " + graphicCards);

            SwingUtilities.invokeLater(() -> {
                processor.setText("Procesor: " + processorName);
                graphicCardsLabel.setText("Karty Graficzne: " + graphicCards);
            });
        }).start();

    }

    private String getSystemNote() {
        String note = "*BRAK*";

        if (this.systemOS != SystemOS.WINDOWS) {
            this.logger.critical("Twój system to nie windows!");
            note = "Twój system to nie windows!\n";
            this.supportedSystem = false;

            JOptionPane.showMessageDialog(null, "Twój system to nie windows", "Niewspierany system", JOptionPane.ERROR_MESSAGE);
        }

        if (this.systemArch != SystemArch.AMD_X64) {
            this.logger.critical("Twoja architektura to nie 64 bitowy AMD!");
            note += "Twoja architektura to nie 64 bitowy AMD!";
            this.supportedSystem = false;

            JOptionPane.showMessageDialog(null, "Twoja architektura to nie 64 bitowy AMD!", "Niewspierany system", JOptionPane.ERROR_MESSAGE);
        }

        return note;
    }

    private void run() {
        this.downloadAssets();

        this.checkTakeOwnerShip();

        this.killAps();
        this.replaceFiles("System32");
        this.replaceFiles("SysWOW64");

        try {
            Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "start minecraft:"}).waitFor();
        } catch (final IOException | InterruptedException exception) {
            this.logger.error("Nie udało się uruchomić minecraft!", exception);
        }

        JOptionPane.showMessageDialog(this.frame, "Udało się!", "Gotowe", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
    }

    private void downloadAssets() {
        final List<String> assetList = List.of(this.currentDir + "\\System32\\Windows.ApplicationModel.Store.dll",
                this.currentDir + "\\SysWOW64\\Windows.ApplicationModel.Store.dll");

        for (final String asset : assetList) {
            if (!Files.exists(Paths.get(asset))) {
                this.logger.critical("Nie znaleziono pliku  \"Windows.ApplicationModel.Store.dll\" w aktualnym katalogu z " + asset + " !.");
                try {
                    this.downloadAssetUtil.downloadAssets();
                } catch (final IOException | TimeoutException exception) {
                    this.logger.critical("Nie udało się pobrać assetów!", exception);
                    System.exit(0);
                }
                return;
            }
        }
        this.progressBar.setValue(100);
        this.progressBar.setText("Wszytko pobrane!");
    }

    private void checkTakeOwnerShip() {
        final Path sourcePath = Paths.get(this.takeOnwershipProExe);
        if (!Files.exists(sourcePath)) {
            this.logger.critical("Nie zainstalowałeś TakeOwnershipPro.");
            JOptionPane.showMessageDialog(null, "Musisz pierw zainstalować TakeOwnership Pro!", "Brak TakeOwnership Pro", JOptionPane.ERROR_MESSAGE);

            try {
                Runtime.getRuntime().exec(this.currentDir + File.separator + "TakeOwnershipPro.exe").waitFor();
            } catch (final IOException | InterruptedException exception) {
                this.logger.critical("Nie udało się zainstalować TakeOwnershipPro");
            }

            if (!Files.exists(sourcePath)) {
                this.logger.critical("Nadal nie zainstalowałeś TakeOwnershipPro.");
                System.exit(0);
            }
        }
    }

    private void killAps() {
        final List<String> appsToKill = List.of("XboxPcApp.exe", "WinStore.App.exe", "Minecraft.Windows.exe", "GameBar.exe");

        for (final String app : appsToKill) {
            try {
                this.killApp(app);
            } catch (final IOException | InterruptedException exception) {
                this.logger.critical("Nie udało się zamknąć aplikacji: '" + app + "'", exception);
            }
        }
    }

    private void killApp(final String processName) throws IOException, InterruptedException {
        final ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/F", "/IM", processName);
        final Process process = processBuilder.start();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                this.logger.info(line);
            }
        }

        process.waitFor(10, TimeUnit.SECONDS);
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
            Runtime.getRuntime().exec(this.takeOnwershipProExe + " " + destinationFile).waitFor();
        } catch (final InterruptedException | IOException exception) {
            this.logger.critical("Nie udało się operować z TakeOwnershipPro w folderze '" + folderName + "'", exception);
            JOptionPane.showMessageDialog(null, "Nie udało się operować z TakeOwnershipPro w folderze '" + folderName + "'", "Krytyczny błąd", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return;
        }

        if (Files.exists(destinationPath)) {
            try {
                FileUtil.deleteFile(destinationPath.toFile());
            } catch (final IOException | UncheckedIOException ioException) {
                this.logger.critical("Nie można usunać pliku! ", ioException);
                JOptionPane.showMessageDialog(null, "Nie można usunać pliku: " + destinationPath, "Krytyczny błąd", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }

        try {
            Files.copy(replacementPatch, destinationPath);
            this.logger.info("Plik w folderze " + folderName + " został skopiowany i podmieniony.");
        } catch (final IOException exception) {
            this.logger.critical("Nie udało się podmienić pliku w folderze '" + folderName + "'!");
            JOptionPane.showMessageDialog(null, "Nie udało się podmienić pliku w folderze '" + folderName + "'!", "Krytyczny błąd", JOptionPane.ERROR_MESSAGE);
            System.exit(0);

            throw new RuntimeException(exception);
        }
    }
}