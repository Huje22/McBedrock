package me.indian.freebedrock.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.net.ssl.HttpsURLConnection;

public final class DownloadAssetUtil {

    private static final Logger LOGGER = Logger.getLogger(DownloadAssetUtil.class.getName());

    public static void downloadAssets() throws IOException {
        final String saveFilePath = System.getProperty("user.dir") + File.separator + "assets.zip";

        downloadFile(saveFilePath);
        unzipFile(System.getProperty("user.dir"), saveFilePath);
        Files.delete(Paths.get(saveFilePath));
        LOGGER.log(Level.INFO, "Pobrano i rozpakowano plik ZIP.");
    }

    private static void downloadFile(final String saveFilePath) throws IOException {
        //TODO: Zmień link do pobrania pliku, i włącz odrazu Minecraft po zakończeniu wszystkiego 
        final URL url = new URL("https://github.com/Huje22/McBedrock/releases/download/Assets/asstes.zip");
        final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        final int fileSize = connection.getContentLength();
        try (final InputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
            try (final FileOutputStream outputStream = new FileOutputStream(saveFilePath)) {
                LOGGER.log(Level.INFO, "Kod połączenia: " + connection.getResponseCode());

                final byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    final double progress = (double) totalBytesRead / fileSize * 100;
                    LOGGER.log(Level.INFO, "Pobrano w: " + progress + "%");
                }
            }
        }

        LOGGER.log(Level.INFO, "Pobrano.");
    }

    private static void unzipFile(final String targetDirectory, final String saveFilePath) throws IOException {
        try (final ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(Paths.get(saveFilePath)))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                final String entryName = zipEntry.getName();
                final File outputFile = new File(targetDirectory, entryName);
                if (zipEntry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    try (final FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                        final byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                }
            }
        }
    }
}
