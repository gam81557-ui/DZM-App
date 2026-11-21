package com.dzm.manager;

import com.dzm.manager.IFileSource;

interface IFileService {
    boolean fileExists(String targetDir, String fileName) = 1;
    String copyFile(IFileSource source, String targetDir, String fileName) = 5;
    boolean zipHasExistingFiles(IFileSource source, String targetDir) = 6;
    String extractZip(IFileSource source, String targetDir) = 7;
    void destroy() = 16777114;
}
