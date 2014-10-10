package com.equalexperts.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
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

        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))) {
            for(String it : classFoldersToDocument) {
                Holder<Boolean> logMessageImplementationFound = new Holder<>(false);
                Path classFolder = Paths.get(it);
                Files.walk(classFolder)
                        .filter(p -> !Files.isDirectory(p))
                        .filter(p -> p.toString().endsWith(".class"))
                        .map(p -> loadClass(classFolder, p))
                        .filter(this::isValidClass)
                        .peek(c -> logMessageImplementationFound.set(true))
                        .onClose(() -> {
                            if (!logMessageImplementationFound.get()) {
                                throw new RuntimeException("No LogMessage implementations found in " + classFolder.toString());
                            }
                        })
                        .forEach(c -> documentLogMessageEnum(out, c));
            }
        }
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
        for(Object o : clazz.getEnumConstants()) {
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