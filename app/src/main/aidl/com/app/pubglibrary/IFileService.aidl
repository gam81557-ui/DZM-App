package com.app.pubglibrary;

interface IFileService {
    boolean fileExists(String targetDir, String fileName) = 1;
    String copyFile(String sourcePath, String targetDir, String fileName) = 2;
    boolean zipHasExistingFiles(String sourcePath, String targetDir) = 3;
    String extractZip(String sourcePath, String targetDir) = 4;
    void destroy() = 16777114;
}
