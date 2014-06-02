package com.equalexperts.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class GenerateLogMessageDocumentation {

    public static void main(String... args) throws Exception {
        String head = args[0];
        String[] tail = Arrays.copyOfRange(args, 1, args.length);
        new GenerateLogMessageDocumentation(head, tail).generate();
    }

    private final File outputFile;
    private final List<String> classFoldersToDocument;

    GenerateLogMessageDocumentation(String outputFile, String... classFoldersToDocument) {
        this.outputFile = new File(outputFile);
        this.classFoldersToDocument = Arrays.asList(classFoldersToDocument);
    }

    public void generate() throws Exception {
        try (
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))
        ) {
            for (String it : classFoldersToDocument) {
                Path classFolder = Paths.get(it);
                boolean logMessageImplementationFound = walkDir(classFolder, classFolder.toFile(), out);
                if (!logMessageImplementationFound) {
                    throw new RuntimeException("No LogMessage implementations found in " + classFolder.toString());
                }
            }
        }
    }

    private boolean walkDir(Path classFolder, File file, PrintWriter out) {
        boolean logMessageImplementationFound = false;
        if (file.isDirectory()) {
            for (File sub : file.listFiles()) {
                logMessageImplementationFound = walkDir(classFolder, sub, out) || logMessageImplementationFound;
            }
        } else if (file.toString().endsWith(".class")) {
            Class<?> cls = loadClass(classFolder, file.toPath());
            if (isValidClass(cls)) {
                logMessageImplementationFound = true;
                documentLogMessageEnum(out, cls);
            }
        }
        return logMessageImplementationFound;
    }

    private Class<?> loadClass(Path classFolder, Path classFile) {
        Path relativePath = classFolder.relativize(classFile);
        String className = toClassName(relativePath);

        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            //this is an inner class, or not a class, etc
            return null;
        }
    }

    private String toClassName(Path relativePath) {
        String classNameWithDotClassExtension = relativePath.toString().replace('/', '.').replace('$', '.');
        return classNameWithDotClassExtension.substring(0, (classNameWithDotClassExtension.length() - ".class".length()));
    }

    private void documentLogMessageEnum(PrintWriter out, Class<?> clazz) {
        out.print(clazz.getName());
        out.println(":");
        out.println("Code\t\tMessage");
        out.println("==========\t==========");
        for (Object o : clazz.getEnumConstants()) {
            LogMessage message = (LogMessage) o;
            out.printf("%s\t%s\n", message.getMessageCode(), message.getMessagePattern());
        }
        out.println();
    }

    private boolean isValidClass(Class<?> clazz) {
        return clazz != null && clazz.isEnum() && LogMessage.class.isAssignableFrom(clazz);
    }

    private static class Holder<T> {
        private T instance;

        private Holder(T instance) {
            this.instance = instance;
        }

        public void set(T value) {
            instance = value;
        }

        public T get() {
            return instance;
        }
    }
}
