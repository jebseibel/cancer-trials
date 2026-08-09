package com.seibel.cancer.scraper.common;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves and restores the authenticated browser session so a scrape run does not need a
 * fresh interactive login every time.
 *
 * This is the piece viro-playwright does not have - it logs in fresh on every run. Here it
 * matters more: if My Health Connection enforces MFA per login, then without session reuse
 * this pipeline is not "repeatable", it is "a human completes every run". Whether reuse
 * actually survives is the open question step 1 of the plan exists to answer.
 *
 * SECURITY: the state file is a live authenticated session to a real medical record.
 * It is functionally a credential - gitignored, and written owner-only where the OS
 * supports it. Never log its contents, never commit it, delete it when done testing.
 */
@Slf4j
public class SessionManager {

    private final Path stateFile;

    public SessionManager(String stateFilePath) {
        this.stateFile = Path.of(stateFilePath);
    }

    /** True when a previously saved session exists on disk. Says nothing about whether it is still valid. */
    public boolean hasSavedSession() {
        return Files.isRegularFile(stateFile);
    }

    /**
     * Opens a context, restoring a saved session when one exists. A restored session may
     * still be expired - the caller must verify with an actual page load, not by trusting
     * this method.
     */
    public BrowserContext newContext(Browser browser) {
        Browser.NewContextOptions options = new Browser.NewContextOptions();

        if (hasSavedSession()) {
            log.info("Restoring saved session from {}", stateFile);
            options.setStorageStatePath(stateFile);
        } else {
            log.info("No saved session at {} - a fresh login will be required", stateFile);
        }
        return browser.newContext(options);
    }

    /** Persists cookies and local storage for the next run. */
    public void save(BrowserContext context) {
        try {
            Path parent = stateFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            context.storageState(new BrowserContext.StorageStateOptions().setPath(stateFile));
            restrictPermissions();
            log.info("Session saved to {}", stateFile);
        } catch (Exception e) {
            // Not fatal - the scrape already succeeded, the next run just pays for a fresh login.
            log.warn("Could not save session state to {}: {}", stateFile, e.getMessage());
        }
    }

    /** Removes the stored session. Use when it has expired or when finished testing. */
    public void clear() {
        try {
            if (Files.deleteIfExists(stateFile)) {
                log.info("Cleared saved session at {}", stateFile);
            }
        } catch (Exception e) {
            log.warn("Could not clear session state at {}: {}", stateFile, e.getMessage());
        }
    }

    /** Best-effort owner-only permissions. Silently skipped on filesystems that do not support POSIX. */
    private void restrictPermissions() {
        try {
            Files.setPosixFilePermissions(stateFile, java.util.Set.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
        } catch (Exception e) {
            log.debug("Could not restrict permissions on {}: {}", stateFile, e.getMessage());
        }
    }
}
