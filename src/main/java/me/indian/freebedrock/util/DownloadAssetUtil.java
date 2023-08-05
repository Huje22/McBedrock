package me.indian.freebedrock.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadAssetUtil {

    private final String saveFilePath;

    public DownloadAssetUtil() {
        this.saveFilePath = System.getProperty("user.dir") + File.separator + "assets.zip";
    }

    public void downloadAssets() {
        try {
            downloadFile();
            unzipFile(System.getProperty("user.dir"));
            Files.delete(Paths.get(this.saveFilePath));
            System.out.println("Pobrano i rozpakowano plik ZIP.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile() throws IOException {
        final URL url = new URL("https://github.com/Huje22/McBedrock/releases/download/Assets/asstes.zip");
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        final int fileSize = connection.getContentLength();
        final InputStream inputStream = new BufferedInputStream(connection.getInputStream());
        final FileOutputStream outputStream = new FileOutputStream(this.saveFilePath);

        System.out.println(connection.getResponseCode());

        final byte[] buffer = new byte[1024];
        int bytesRead;
        long totalBytesRead = 0;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
            final double progress = (double) totalBytesRead / fileSize * 100;
            System.out.println("Pobrano w: " + progress + "%");
        }

        inputStream.close();
        outputStream.close();

        System.out.println("Pobrano.");
    }

    private void unzipFile(final String targetDirectory) throws IOException {
        try (final ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(Paths.get(this.saveFilePath)))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                final String entryName = zipEntry.getName();
                final File outputFile = new File(targetDirectory, entryName);
                if (zipEntry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
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
