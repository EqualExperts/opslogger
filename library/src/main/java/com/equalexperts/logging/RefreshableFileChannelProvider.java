package com.equalexperts.logging;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

class RefreshableFileChannelProvider implements Closeable {
    private final Path path;
    private final Duration maximumResultLifetime;

    private Result result;

    RefreshableFileChannelProvider(Path path, Duration maximumResultLifetime) {
        this.path = path;
        this.maximumResultLifetime = maximumResultLifetime;
    }

    public Result getChannel(Instant now) throws IOException {
        closeChannelIfItIsTooOld(now);
        createChannelIfNecessary(now);
        return result;
    }

    @Override
    public void close() throws IOException {
        if (result != null) {
            result.writer.close();
        }
    }

    private void createChannelIfNecessary(Instant now) throws IOException {
        if (result == null) {
            FileChannel fileChannel = FileChannel.open(path, CREATE, APPEND);
            Writer writer = Channels.newWriter(fileChannel, "UTF-8");
            result = new Result(fileChannel, writer, now);
        }
    }

    private void closeChannelIfItIsTooOld(Instant now) throws IOException {
        if (result != null) {
            Duration resultAge = Duration.between(result.created, now);
            if (resultAge.compareTo(maximumResultLifetime) > 0) {
                result.writer.close();
                result = null;
            }
        }
    }

    Path getPath() {
        return path;
    }

    public Duration getMaximumResultLifetime() {
        return maximumResultLifetime;
    }

    static class Result {
        final FileChannel channel;
        final Writer writer;
        private final Instant created;

        Result(FileChannel channel, Writer writer, Instant created) {
            this.channel = channel;
            this.writer = writer;
            this.created = created;
        }
    }
}