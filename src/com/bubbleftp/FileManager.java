package com.bubbleftp;

import java.io.File;
import java.io.IOException;

public class FileManager {

    private File currCwd;

    public FileManager() {
        currCwd = new File(".");
    }


    public String switchDirectory(String newDirectory) throws IOException {
        currCwd = new File(newDirectory);
        return currCwd.getCanonicalPath();
    }

    public String getPwd() throws IOException {
        return currCwd.getCanonicalPath();
    }

    public String[] listFiles() {
        return currCwd.list();
    }
}
