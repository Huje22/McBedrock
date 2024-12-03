package pl.indianbartonka.freebedrock.util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import pl.indianbartonka.util.ZipUtil;
import pl.indianbartonka.util.download.DownloadListener;
import pl.indianbartonka.util.download.DownloadTask;
import pl.indianbartonka.util.file.FileUtil;
import pl.indianbartonka.util.http.HttpStatusCode;
import pl.indianbartonka.util.http.connection.Connection;
import pl.indianbartonka.util.http.connection.request.Request;
import pl.indianbartonka.util.http.connection.request.RequestBuilder;
import pl.indianbartonka.util.logger.Logger;

public final class DownloadAssetUtil {

    private static Logger logger;

    public static void setLogger(final Logger logger) {
        DownloadAssetUtil.logger = logger;
    }

    public static void downloadAssets() throws IOException, TimeoutException {
        final File saveFile = new File(System.getProperty("user.dir") + File.separator + "assets.zip");

        downloadFile(saveFile);
        ZipUtil.unzipFile(saveFile.getPath(), System.getProperty("user.dir"), false);
        FileUtil.deleteFile(saveFile);

        logger.info("Pobrano i rozpakowano plik ZIP.");
    }

    private static void downloadFile(final File saveFile) throws IOException, TimeoutException {
        final DownloadListener downloadListener = new DownloadListener() {
            @Override
            public void onStart(final int definedBuffer, final File outputFile) {
                logger.info("Zaczęto pobieranie");
            }

            @Override
            public void onSecond(final int progress, final double formatedSpeed, final String remainingTimeString) {

            }

            @Override
            public void onProgress(final int progress, final double formatedSpeed, final String remainingTimeString) {
                logger.info(progress + "% " + formatedSpeed + " MB/s Pozostało " + remainingTimeString);
            }

            @Override
            public void onTimeout(final int timeOutSeconds) {

            }

            @Override
            public void onEnd(final File outputFile) {
                logger.info("Pobrano: " + outputFile.getPath());
            }

            @Override
            public void onDownloadStop() {
            }
        };

        final Request request = new RequestBuilder()
                .setUrl("https://raw.githubusercontent.com/Huje22/McBedrock/master/assets/asstes.zip")
                .GET()
                .build();

        try (final Connection connection = new Connection(request)) {
            final HttpStatusCode statusCode = connection.getHttpStatusCode();

            logger.info("Kod odpowiedzi: " + statusCode + " (" + statusCode.getCode() + ")");

            if (statusCode.isSuccess()) {
                final DownloadTask downloadTask = new DownloadTask(connection.getInputStream(), saveFile, connection.getContentLength(), 130, downloadListener);

                downloadTask.downloadFile();
            } else {
                logger.info(connection.getResponseMessage());
            }
        }
    }
}
