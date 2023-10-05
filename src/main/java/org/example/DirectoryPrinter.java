package org.example;

import java.io.File;
import java.util.Objects;

/*
    for checking path
 */
public class DirectoryPrinter {

    public static void main(String[] args) {
        // your ClassPath
        String path = "target\\classes\\org\\example";
        File directory = new File(path);
        printFiles(directory, "");
    }

    public static void printFiles(File directory, String indent) {
        if (directory.isDirectory()) {
            System.out.println(indent + "Directory: " + directory.getName());
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                printFiles(file, indent + "  ");
            }
        } else {
            System.out.println(indent + "File: " + directory.getName());
        }
    }
}
