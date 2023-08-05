package me.indian.freebedrock;

import me.indian.freebedrock.util.DownloadAssetUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RunFreeBedrock {

    private final String sourceFile, currentDir;

    public RunFreeBedrock() {
        this.downloadAssets();
        if (!this.isWindows()) {
            System.out.println("Twój system to nie windows!");
            System.exit(0);
        }
        if (!this.is64Bit()) {
            System.out.println("Twój system nie jest 64 bitowy!");
            System.exit(0);
        }

        this.sourceFile = "C:\\Program Files (x86)\\TakeOwnershipPro\\TakeOwnershipPro.exe";
        this.currentDir = System.getProperty("user.dir");

        final Path sourcePath = Paths.get(this.sourceFile);
        if (!Files.exists(sourcePath)) {
            System.out.println("Nie zainstalowałeś TakeOwnershipPro.");
            this.installOwnership();
            if (!Files.exists(sourcePath)) {
                System.out.println("Nadal nie zainstalowałeś TakeOwnershipPro.");
                System.exit(0);
            }
        }
        this.replaceFiles("System32");
        this.replaceFiles("SysWOW64");

    }


    public static void main(String[] args) {
        new RunFreeBedrock();
    }

    private void downloadAssets() {
        final List<String> assetList = new ArrayList<>();

        assetList.add(this.currentDir + "\\System32\\Windows.ApplicationModel.Store.dll");
        assetList.add(this.currentDir + "\\SysWOW64\\Windows.ApplicationModel.Store.dll");
        for (final String asset : assetList) {
            if (!Files.exists(Paths.get(asset))) {
                System.out.println("Nie znaleziono pliku  \"Windows.ApplicationModel.Store.dll\" w aktualnym katalogu z " + asset + " !.");
                new DownloadAssetUtil().downloadAssets();
                return;
            }
        }
    }

    public void replaceFiles(final String folderName) {
        final String destinationFile = "C:\\Windows\\" + folderName + "\\Windows.ApplicationModel.Store.dll";
        final String replacementFile = this.currentDir + "\\" + folderName + "\\Windows.ApplicationModel.Store.dll";

        try {
            final Path destinationPath = Paths.get(destinationFile);
            final Path replacementPatch = Paths.get(replacementFile);

            if (!Files.exists(replacementPatch)) {
                System.out.println("Nie znaleziono pliku  \"Windows.ApplicationModel.Store.dll\" w aktualnym katalogu z \"" + this.currentDir + File.separator + folderName + "\" !.");
                return;
            }

            final Process process = Runtime.getRuntime().exec(this.sourceFile + " " + destinationFile);
            process.waitFor();

            if (Files.exists(destinationPath)) {
                try {
                    Files.delete(destinationPath);
                } catch (AccessDeniedException accessDeniedException) {
                    accessDeniedException.printStackTrace();
                    System.out.println("Nie można usunać pliku! ");
                    System.out.println("Zamknij programy typu:!");
                    System.out.println("Minecraft");
                    System.out.println("Xbox app");
                    System.out.println("Xbox game bar");
                    System.out.println("Microsoft store");
                    System.out.println("I inne takie a następne spróbuj ponownie");
                    System.exit(0);
                }
            }

            Files.copy(replacementPatch, destinationPath);

            System.out.println("Plik w folderze " + folderName + " został skopiowany i podmieniony.");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void installOwnership() {
        try {
            final Process process = Runtime.getRuntime().exec(this.currentDir + File.separator + "TakeOwnershipPro.exe");
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean is64Bit() {
        return System.getProperty("os.arch").contains("64");
    }

    public boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}