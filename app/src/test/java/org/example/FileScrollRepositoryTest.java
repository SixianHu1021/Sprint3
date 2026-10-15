package org.example;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import org.example.model.DigitalScroll;
import org.example.repository.file.FileScrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies scroll storage independently of login, upload and console services.
 * Adapted from the scroll persistence scenarios in SprintOneTest.
 */
class FileScrollRepositoryTest {
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 10, 10, 9, 30);

    @TempDir Path directory;
    private Path snapshot;
    private FileScrollRepository scrolls;

    /** Creates a separate temporary repository for each test. */
    @BeforeEach
    void setup() {
        snapshot = directory.resolve("scrolls.properties");
        scrolls = new FileScrollRepository(snapshot);
    }

    /** Supplies a scroll whose content covers every byte value, including zero and negative bytes. */
    private DigitalScroll binary() {
        byte[] content = new byte[256];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) i;
        }
        return new DigitalScroll("s1", "Binary", content, "u1", DATE);
    }

    /** An absent snapshot starts an empty repository and lookups report absence. */
    @Test
    void missingSnapshotStartsEmpty() {
        assertTrue(scrolls.findAll().isEmpty());
        assertTrue(scrolls.findById("missing").isEmpty());
        assertFalse(scrolls.existsById("missing"));
        assertFalse(scrolls.existsByName("missing"));
    }

    /** Bytes, metadata and counters survive reopening; returned lists cannot be modified. */
    @Test
    void savedScrollSurvivesRestart() {
        scrolls.save(binary());
        scrolls.save(new DigitalScroll("s2", "Counted", new byte[]{7}, "u2", DATE, 3, 5));
        FileScrollRepository reopened = new FileScrollRepository(snapshot);
        DigitalScroll restored = reopened.findById("s1").orElseThrow();
        assertArrayEquals(binary().getContent(), restored.getContent());
        assertEquals("Binary", restored.getName());
        assertEquals("u1", restored.getUploaderId());
        assertEquals(DATE, restored.getUploadDate());
        assertEquals(1, restored.getUploadCount());
        assertEquals(0, restored.getDownloadCount());
        DigitalScroll counted = reopened.findById("s2").orElseThrow();
        assertEquals(3, counted.getUploadCount());
        assertEquals(5, counted.getDownloadCount());
        assertEquals(2, reopened.findAll().size());
        assertTrue(reopened.existsByName("Counted"));
        assertFalse(reopened.existsByName("counted"));
        assertThrows(UnsupportedOperationException.class, () -> reopened.findAll().clear());
    }

    /** Empty content is stored and restored as zero bytes. */
    @Test
    void emptyContentSurvivesRestart() {
        scrolls.save(new DigitalScroll("empty", "Empty", new byte[0], "u1", DATE));
        assertEquals(0, new FileScrollRepository(snapshot).findById("empty").orElseThrow().getContent().length);
    }

    /** Content is copied in and out, so callers cannot change a stored scroll. */
    @Test
    void contentIsCopiedDefensively() {
        byte[] content = {1, 2, 3};
        DigitalScroll scroll = new DigitalScroll("s", "S", content, "u", DATE);
        content[0] = 9;
        scroll.getContent()[1] = 9;
        assertArrayEquals(new byte[]{1, 2, 3}, scroll.getContent());
    }

    /** Saving the same ID replaces it; a name used by another scroll is rejected. */
    @Test
    void upsertsAndRejectsDuplicateNames() {
        scrolls.save(binary());
        scrolls.save(new DigitalScroll("s1", "Renamed", new byte[]{1}, "u1", DATE, 1, 2));
        assertEquals(1, scrolls.findAll().size());
        assertFalse(scrolls.existsByName("Binary"));
        assertThrows(IllegalArgumentException.class,
                () -> scrolls.save(new DigitalScroll("other", "Renamed", new byte[0], "u2", DATE)));
        FileScrollRepository reopened = new FileScrollRepository(snapshot);
        assertEquals(1, reopened.findAll().size());
        assertEquals(2, reopened.findById("s1").orElseThrow().getDownloadCount());
    }

    /** A file in place of the parent directory prevents saving without adding a scroll. */
    @Test
    void failedSaveLeavesMemoryUnchanged() throws IOException {
        Path blocker = directory.resolve("blocker");
        FileScrollRepository blocked = new FileScrollRepository(blocker.resolve("scrolls.properties"));
        Files.writeString(blocker, "regular file");
        assertThrows(IllegalStateException.class, () -> blocked.save(binary()));
        assertTrue(blocked.findAll().isEmpty());
    }

    /** Snapshots written before counters existed load with 1 upload and 0 downloads. */
    @Test
    void missingCountersUseDefaults() throws IOException {
        scrolls.save(binary());
        Properties data = load(snapshot);
        data.remove("0.uploads");
        data.remove("0.downloads");
        store(snapshot, data);
        DigitalScroll legacy = new FileScrollRepository(snapshot).findById("s1").orElseThrow();
        assertEquals(1, legacy.getUploadCount());
        assertEquals(0, legacy.getDownloadCount());
    }

    /** Invalid counts, counters, fields and Base64 content fail explicitly. */
    @Test
    void corruptedSnapshotsAreRejected() throws IOException {
        Path bad = directory.resolve("bad.properties");
        for (String content : List.of("count=no", "count=-1", "unknown=value", "count=1\n0.id=broken")) {
            Files.writeString(bad, content);
            assertThrows(IllegalStateException.class, () -> new FileScrollRepository(bad));
        }
        scrolls.save(binary());
        for (String[] change : List.of(new String[]{"0.downloads", "-1"}, new String[]{"0.downloads", "x"},
                new String[]{"0.uploads", "0"}, new String[]{"0.content", "%%%"}, new String[]{"0.date", "today"})) {
            Properties data = load(snapshot);
            data.setProperty(change[0], change[1]);
            store(bad, data);
            assertThrows(IllegalStateException.class, () -> new FileScrollRepository(bad));
        }
        assertThrows(IllegalStateException.class, () -> new FileScrollRepository(directory));
    }

    /** Duplicate IDs or names in stored records are rejected at startup. */
    @Test
    void duplicateSnapshotRecordsAreRejected() throws IOException {
        scrolls.save(binary());
        String original = Files.readString(snapshot);
        Path bad = directory.resolve("duplicate.properties");
        String second = original.replace("0.", "1.");
        Files.writeString(bad, original.replace("count=1", "count=2") + "\n" + second.replace("count=1", "count=2"));
        assertThrows(IllegalStateException.class, () -> new FileScrollRepository(bad));
        Files.writeString(bad, original.replace("count=1", "count=2") + "\n"
                + second.replace("count=1", "count=2").replace("1.id=s1", "1.id=other"));
        assertThrows(IllegalStateException.class, () -> new FileScrollRepository(bad));
    }

    /** Missing required fields and invalid counters are rejected by the model. */
    @Test
    void invalidScrollsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DigitalScroll("", "n", new byte[0], "u", DATE));
        assertThrows(IllegalArgumentException.class, () -> new DigitalScroll("s", null, new byte[0], "u", DATE));
        assertThrows(IllegalArgumentException.class, () -> new DigitalScroll("s", "n", new byte[0], " ", DATE));
        assertThrows(NullPointerException.class, () -> new DigitalScroll("s", "n", new byte[0], "u", null));
        assertThrows(IllegalArgumentException.class, () -> new DigitalScroll("s", "n", new byte[0], "u", DATE, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new DigitalScroll("s", "n", new byte[0], "u", DATE, 1, -1));
    }

    /** Reads a snapshot so a test can alter individual properties. */
    private static Properties load(Path file) throws IOException {
        Properties data = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            data.load(reader);
        }
        return data;
    }

    /** Writes altered properties back to a snapshot file. */
    private static void store(Path file, Properties data) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file)) {
            data.store(writer, "test snapshot");
        }
    }
}
