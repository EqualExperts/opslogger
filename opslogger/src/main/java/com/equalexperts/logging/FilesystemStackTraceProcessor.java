package com.equalexperts.logging;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * A stack trace processor that stores the stack trace in a uniquely fingerprinted file in a given destination.
 * The URI of the file (whether new or existing) is included in the log message.
 */
class FilesystemStackTraceProcessor implements StackTraceProcessor {
    private final Path destination;
    private final ThrowableFingerprintCalculator fingerprintCalculator;

    FilesystemStackTraceProcessor(Path destination, ThrowableFingerprintCalculator fingerprintCalculator) {
        this.destination = destination;
        this.fingerprintCalculator = fingerprintCalculator;
    }

    @Override
    public void process(Throwable throwable, StringBuilder output) throws Exception {
        Path stackTraceFile = calculateFilenameForException(throwable);
        writeStacktraceToPathIfNecessary(throwable, stackTraceFile);
        printSubstituteMessage(output, throwable, stackTraceFile);
    }

    Path getDestination() {
        return destination;
    }

    private void writeStacktraceToPathIfNecessary(Throwable throwable, Path stackTraceFile) throws IOException {
        if (Files.notExists(stackTraceFile)) {
            try(PrintStream out = new PrintStream(Files.newOutputStream(stackTraceFile, CREATE_NEW, WRITE))) {
                throwable.printStackTrace(out);
            } catch (FileAlreadyExistsException ignore) {
                //the exception is being written to (probably right now)
            }
        }
    }

    private void printSubstituteMessage(StringBuilder output, Throwable throwable, Path stackTraceFile) {
        output.append(throwable.toString());
        output.append(" (");
        output.append(stackTraceFile.toUri().toString());
        output.append(")");
    }

    private Path calculateFilenameForException(Throwable throwable) {
        String fingerprint = fingerprintCalculator.calculateFingerprint(throwable);
        String filePath = "stacktrace_" + fingerprint + ".txt";
        return destination.resolve(filePath);
    }
}
