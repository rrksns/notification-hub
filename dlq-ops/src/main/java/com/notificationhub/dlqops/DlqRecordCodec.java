// DLQ 레코드를 JSON Lines 파일 형식으로 변환하는 codec
package com.notificationhub.dlqops;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class DlqRecordCodec {

    private final ObjectMapper objectMapper;

    DlqRecordCodec() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    String toLine(DlqRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to serialize DLQ record", exception);
        }
    }

    DlqRecord fromLine(String line) {
        try {
            return objectMapper.readValue(line, DlqRecord.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to deserialize DLQ record", exception);
        }
    }

    void write(Path output, List<DlqRecord> records) {
        List<String> lines = records.stream()
                .map(this::toLine)
                .toList();
        try {
            Files.write(output, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write DLQ records", exception);
        }
    }

    List<DlqRecord> read(Path input) {
        try {
            return Files.readAllLines(input, StandardCharsets.UTF_8)
                    .stream()
                    .filter(line -> !line.isBlank())
                    .map(this::fromLine)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read DLQ records", exception);
        }
    }
}
