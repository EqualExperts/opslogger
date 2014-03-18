package uk.gov.gds.performance.collector.logging;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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

                final Holder<Boolean> logMessageImplementationFound = new Holder<>(false);

                final Path classFolder = Paths.get(it);
                Files.walkFileTree(classFolder, new SimpleFileVisitor<Path>() {
                    @SuppressWarnings("NullableProblems")
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!file.toString().endsWith(".class")) {
                            return super.visitFile(file, attrs);
                        }

                        Path relativePath = classFolder.relativize(file);
                        String className = toClassName(relativePath);

                        try {
                            Class<?> clazz = Class.forName(className);
                            if (isValidClass(clazz)) {
                                logMessageImplementationFound.set(true);
                                documentLogMessageEnum(out, clazz);
                            }
                        } catch (ClassNotFoundException e) {
                            //this is an inner class, or not a class, etc
                        }
                        return super.visitFile(file, attrs);
                    }
                });

                if (!logMessageImplementationFound.get()) {
                    throw new RuntimeException("No LogMessage implementations found in " + classFolder.toString());
                }
            }
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
        return clazz.isEnum() && LogMessage.class.isAssignableFrom(clazz);
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