package com.bubbleftp;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

public class FileManager {

    private File currCwd;

    public FileManager() {
        currCwd = new File(".");
    }


    public String switchDirectory(String newDirectory) throws IOException {
        String newPath = newDirectory.startsWith("/") ? newDirectory : currCwd.getAbsoluteFile() + File.separator + newDirectory;
        File newCwd = new File(newPath);

        if (!newCwd.exists() || !newCwd.isDirectory()) {
            throw new NoSuchFileException(newPath, "", "Unknown directory: " + newPath);
        }

        currCwd = newCwd;
        return currCwd.getCanonicalPath();
    }

    public String getPwd() throws IOException {
        return currCwd.getCanonicalPath();
    }

    public String[] listFiles() {
        return currCwd.list();
    }
}
