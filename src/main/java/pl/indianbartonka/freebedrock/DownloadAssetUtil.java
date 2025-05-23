package pl.indianbartonka.freebedrock;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import pl.indianbartonka.util.FileUtil;
import pl.indianbartonka.util.ZipUtil;
import pl.indianbartonka.util.download.DownloadListener;
import pl.indianbartonka.util.download.DownloadTask;
import pl.indianbartonka.util.http.HttpStatusCode;
import pl.indianbartonka.util.http.connection.Connection;
import pl.indianbartonka.util.http.connection.request.Request;
import pl.indianbartonka.util.http.connection.request.RequestBuilder;
import pl.indianbartonka.util.logger.Logger;

public class DownloadAssetUtil {

    private final Logger logger;
    private final ProgressPanel progressBar;

    public DownloadAssetUtil(final Logger logger, final ProgressPanel progressBar) {
        this.logger = logger;
        this.progressBar = progressBar;
        progressBar.setValue(0);
        progressBar.setText("Status Pobierania");
    }

    public void downloadAssets() throws IOException, TimeoutException {
        final File saveFile = new File(System.getProperty("user.dir") + File.separator + "assets.zip");

        this.downloadFile(saveFile);
        ZipUtil.unzipFile(saveFile.getPath(), System.getProperty("user.dir"), false);
        FileUtil.deleteFile(saveFile);

        this.logger.info("Pobrano i rozpakowano plik ZIP.");
    }

    private void downloadFile(final File saveFile) throws IOException, TimeoutException {
        final DownloadListener downloadListener = new DownloadListener() {
            @Override
            public void onStart(final int definedBuffer, final File outputFile) {
                DownloadAssetUtil.this.logger.info("Rozpoczęto pobieranie!");
                DownloadAssetUtil.this.progressBar.setText("Rozpoczęto pobieranie!");
            }

            @Override
            public void onSecond(final int progress, final double formatedSpeed, final String remainingTimeString) {
                DownloadAssetUtil.this.progressBar.setValue(progress);
                DownloadAssetUtil.this.progressBar.setText(progress + "% " + formatedSpeed + " MB/s Pozostało " + remainingTimeString);
            }

            @Override
            public void onProgress(final int progress, final double formatedSpeed, final String remainingTimeString) {
                DownloadAssetUtil.this.logger.info(progress + "% " + formatedSpeed + " MB/s Pozostało " + remainingTimeString);
            }

            @Override
            public void onTimeout(final int timeOutSeconds) {
                DownloadAssetUtil.this.progressBar.setValue(-1);
                DownloadAssetUtil.this.progressBar.setText("Przekroczono limit czas!");
            }

            @Override
            public void onEnd(final File outputFile) {
                DownloadAssetUtil.this.progressBar.setValue(100);
                DownloadAssetUtil.this.progressBar.setText("Wszytko pobrane!");
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

            this.logger.info("Kod odpowiedzi: " + statusCode + " (" + statusCode.getCode() + ")");

            if (statusCode.isSuccess()) {
                final DownloadTask downloadTask = new DownloadTask(connection.getInputStream(), saveFile, connection.getContentLength(), 130, downloadListener);

                downloadTask.downloadFile();
            } else {
                this.logger.info(connection.getResponseMessage());
                this.progressBar.setText("Nie udało się pobrać assetów: " + connection.getResponseMessage());
            }
        }
    }
}
