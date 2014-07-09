package com.equalexperts.logging;

import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

class RefreshableFileChannelProvider {
    private final Path path;
    private final Duration maximumResultLifetime;

    private Result result;

    RefreshableFileChannelProvider(Path path, Duration maximumResultLifetime) {
        this.path = path;
        this.maximumResultLifetime = maximumResultLifetime;
    }

    public Result getChannel(Instant now) throws IOException {
        if (result != null) {
            Duration resultAge = Duration.between(result.created, now);
            if (resultAge.compareTo(maximumResultLifetime) > 0) {
                result.channel.close();
                result = null;
            }
        }
        if (result == null) {
            FileChannel fileChannel = FileChannel.open(path, CREATE, APPEND);
            Writer writer = Channels.newWriter(fileChannel, "UTF-8");
            result = new Result(fileChannel, writer, now);
        }
        return result;
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
