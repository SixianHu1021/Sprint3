package org.example.repository.file;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
 *
 * Reads and writes UTF-8 properties snapshots for user and scroll repositories.
 * Intended for a single application process without concurrent writers.
 */
public final class AtomicPropertiesStore {
    private final Path file;

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Creates a store with an absolute snapshot path.
     * @param file snapshot file path
     */
    public AtomicPropertiesStore(Path file) {
        this.file = file.toAbsolutePath();
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Loads properties, returning an empty snapshot if the file does not exist.
     * @return loaded properties
     * @throws IllegalStateException if reading fails or properties syntax is invalid
     */
    public Properties read() {
        Properties data = new Properties();
        if (Files.notExists(file)) {
            return data;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            data.load(reader);
            return data;
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot read data file: " + file, e);
        }
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Writes a temporary snapshot and replaces the destination after closing the writer.
     * Creates missing parent directories and attempts an atomic move. If unsupported,
     * falls back to regular replacement without the same atomicity guarantee.
     * @param data properties to persist
     * @throws IllegalStateException if writing or replacing the snapshot fails
     */
    public void write(Properties data) {
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "vsas-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                data.store(writer, "VSAS Sprint 1 snapshot");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save data file: " + file, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    // Cleanup is best-effort and must not mask the write result.
                }
            }
        }
    }
}
