package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies user storage independently of authentication and console services.
 * Adapted from the persistence scenarios in SprintOneTest. Password hashes are
 * fixture values; these tests do not verify password hashing or registration rules.
 */
class FileUserRepositoryTest {
    @TempDir Path directory;
    private Path snapshot;
    private FileUserRepository users;

    /** Creates a separate temporary repository for each test. */
    @BeforeEach
    void setup() {
        snapshot = directory.resolve("users.properties");
        users = new FileUserRepository(snapshot);
    }

    /** Supplies a complete profile with a placeholder hash for storage tests. */
    private User member() {
        return new User("m", "member", "fixture-hash", "Member", "member@example.com", "456", UserRole.MEMBER);
    }

    /** An absent snapshot starts an empty repository and lookups report absence. */
    @Test
    void missingSnapshotStartsEmpty() {
        assertTrue(users.findAll().isEmpty());
        assertTrue(users.findByIdKey("missing").isEmpty());
        assertTrue(users.findByUsername("missing").isEmpty());
        assertFalse(users.existsByIdKey("missing"));
        assertFalse(users.existsByUsername("missing"));
    }

    /** Every profile field survives reopening; returned lists cannot be modified. */
    @Test
    void savedProfileSurvivesRestart() {
        users.save(member());
        FileUserRepository reopened = new FileUserRepository(snapshot);
        User restored = reopened.findByUsername("member").orElseThrow();
        assertEquals("m", restored.getIdKey());
        assertEquals("member", restored.getUsername());
        assertEquals("Member", restored.getFullName());
        assertEquals("member@example.com", restored.getEmail());
        assertEquals("456", restored.getPhone());
        assertEquals("fixture-hash", restored.getPasswordHash());
        assertEquals(UserRole.MEMBER, restored.getRole());
        assertTrue(reopened.existsByIdKey("m"));
        assertFalse(reopened.existsByUsername("MEMBER"));
        assertThrows(UnsupportedOperationException.class, () -> reopened.findAll().clear());
    }

    /** Saving the same ID updates its profile; a conflicting username is rejected. */
    @Test
    void upsertsAndRejectsNameCollisions() {
        users.save(member());
        users.save(new User("m", "renamed", "new-hash", "New Name", "new@example.com", "789", UserRole.ADMIN));
        assertEquals(1, users.findAll().size());
        assertFalse(users.existsByUsername("member"));
        assertThrows(IllegalArgumentException.class,
                () -> users.save(new User("other", "renamed", "hash", "N", "e", "p", UserRole.MEMBER)));
        FileUserRepository reopened = new FileUserRepository(snapshot);
        assertEquals(1, reopened.findAll().size());
        User restored = reopened.findByIdKey("m").orElseThrow();
        assertEquals("renamed", restored.getUsername());
        assertEquals("new-hash", restored.getPasswordHash());
        assertEquals("New Name", restored.getFullName());
        assertEquals("new@example.com", restored.getEmail());
        assertEquals("789", restored.getPhone());
        assertEquals(UserRole.ADMIN, restored.getRole());
    }

    /** Deletion survives reopening and an absent ID leaves existing users intact. */
    @Test
    void deletionPersistsAndMissingIdIsHarmless() {
        users.save(member());
        users.deleteByIdKey("absent");
        assertTrue(new FileUserRepository(snapshot).existsByIdKey("m"));
        users.deleteByIdKey("m");
        assertTrue(users.findAll().isEmpty());
        assertFalse(new FileUserRepository(snapshot).existsByIdKey("m"));
    }

    /** A file in place of the parent directory prevents saving without adding a user. */
    @Test
    void failedInsertLeavesMemoryUnchanged() throws IOException {
        Path blocker = directory.resolve("blocker");
        FileUserRepository blocked = new FileUserRepository(blocker.resolve("users.properties"));
        Files.writeString(blocker, "regular file");
        assertThrows(IllegalStateException.class, () -> blocked.save(member()));
        assertTrue(blocked.findAll().isEmpty());
    }

    /** Failed updates and deletions preserve both memory and the original disk snapshot. */
    @Test
    void failedMutationsPreserveExistingUsers() throws IOException {
        users.save(member());
        Path backup = directory.resolve("backup.properties");
        Files.move(snapshot, backup);
        Files.createDirectory(snapshot);
        Files.writeString(snapshot.resolve("blocker"), "prevent replacement");
        assertThrows(IllegalStateException.class,
                () -> users.save(new User("m", "renamed", "h", "N", "e", "p", UserRole.MEMBER)));
        assertThrows(IllegalStateException.class, () -> users.deleteByIdKey("m"));
        assertEquals("member", users.findByIdKey("m").orElseThrow().getUsername());
        assertEquals("member", new FileUserRepository(backup).findByIdKey("m").orElseThrow().getUsername());
    }

    /** Invalid counts, missing fields and invalid property escapes fail explicitly. */
    @Test
    void corruptedSnapshotsAreRejected() throws IOException {
        Path bad = directory.resolve("bad.properties");
        for (String content : List.of("count=no", "count=-1", "unknown=value", "count=1\n0.role=INVALID",
                "count=1\n0.id=broken", "value=" + (char) 92 + "uZZZZ")) {
            Files.writeString(bad, content);
            assertThrows(IllegalStateException.class, () -> new FileUserRepository(bad));
        }
        assertThrows(IllegalStateException.class, () -> new FileUserRepository(directory));
    }

    /** Duplicate IDs or usernames in stored records are rejected at startup. */
    @Test
    void duplicateSnapshotRecordsAreRejected() throws IOException {
        users.save(member());
        String original = Files.readString(snapshot);
        Path bad = directory.resolve("duplicate.properties");
        String second = original.replace("0.", "1.");
        Files.writeString(bad, original.replace("count=1", "count=2") + "\n" + second.replace("count=1", "count=2"));
        assertThrows(IllegalStateException.class, () -> new FileUserRepository(bad));
        Files.writeString(bad, original.replace("count=1", "count=2") + "\n"
                + second.replace("count=1", "count=2").replace("1.id=m", "1.id=other"));
        assertThrows(IllegalStateException.class, () -> new FileUserRepository(bad));
    }
}
