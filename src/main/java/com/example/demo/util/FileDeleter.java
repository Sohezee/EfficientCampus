package com.example.demo.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileDeleter {
    private final EventLogger eventLogger = new EventLogger(FileDeleter.class);
    Path directoryPath;

    public FileDeleter(String path) {
        this.directoryPath = Paths.get(path);
    }

    // Method to delete all files in the specified directory
    public void deleteAllFilesInDirectory() {
        // Use Files.walk to recursively iterate through all files and subdirectories
        try {
            Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Delete the file
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    // Continue after visiting the directory
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            eventLogger.logException(e);
        }
    }
}
