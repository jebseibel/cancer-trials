package com.seibel.cancer.scraper.common;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;

/**
 * Selector-driven login, adapted from viro-playwright's LoginService.
 *
 * Kept from that version: everything is selector-driven from config, and the login form is
 * looked for inside an iframe before falling back to the main page. That fallback is not
 * incidental here - Epic MyChart deployments do not consistently put the login form at the
 * top level.
 *
 * Added for MyChart:
 *  - alreadyLoggedIn(), so a restored session skips the login entirely
 *  - a pause for MFA, which the green-e portal never needed
 *  - no credential logging, ever (viro's version had commented-out lines printing the
 *    password; those are deliberately not carried over)
 */
@Slf4j
public class LoginService {

    private final Page page;

    public LoginService(Page page) {
        this.page = page;
    }

    /**
     * Navigates to the portal and reports whether a restored session is still authenticated.
     * Verifies by loading a page and looking for the post-login marker - a saved state file
     * on disk proves nothing about whether the session is still alive.
     */
    public boolean alreadyLoggedIn(String url, String successSelector, int timeoutMs) {
        try {
            page.navigate(url);
            page.waitForSelector(successSelector,
                    new Page.WaitForSelectorOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(timeoutMs));
            log.info("Existing session is still valid - skipping login");
            return true;
        } catch (TimeoutError e) {
            log.info("No valid existing session - login required");
            return false;
        }
    }

    /**
     * Fills and submits the login form, then waits for the post-login marker.
     *
     * When mfaWaitMs > 0 the wait is extended to leave room for a code to be entered by
     * hand in the visible browser. That is the difference between "repeatable" and
     * "semi-automated" and it is a property of the account, not of this code.
     */
    public void login(LoginParams params) {
        log.info("Navigating to portal login");
        page.navigate(params.url());

        Frame loginFrame = findLoginFrame(params.usernameSelector());

        if (loginFrame != null) {
            log.info("Login form found inside iframe: {}", loginFrame.url());
            fillAndSubmitInFrame(loginFrame, params);
        } else {
            log.info("Login form found on the main page");
            fillAndSubmitOnPage(params);
        }

        if (params.mfaWaitMs() > 0) {
            log.warn("Waiting up to {}s for MFA - complete any verification in the browser window",
                    params.mfaWaitMs() / 1000);
        }

        int successTimeout = Math.max(params.formTimeout(), params.mfaWaitMs());
        page.waitForSelector(params.successSelector(),
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(successTimeout));

        log.info("Login complete");
    }

    /**
     * Heuristic check that the browser left the login screen, used only to decide whether a
     * session is worth saving after the configured marker failed to match. Deliberately
     * loose - the precise check is successSelector; this exists so a wrong selector does
     * not discard a login that cost the patient an MFA interruption.
     */
    public boolean looksAuthenticated(String loginUrl) {
        try {
            String current = page.url();
            // Epic bounces to an authenticated landing path; still sitting on the login
            // URL means the credentials or MFA never went through.
            boolean movedOffLogin = !current.equalsIgnoreCase(loginUrl)
                    && !current.toLowerCase().contains("/authentication/login");

            boolean noPasswordField = page.locator("input[type='password']").count() == 0;

            log.info("looksAuthenticated: url={}, movedOffLogin={}, noPasswordField={}",
                    current, movedOffLogin, noPasswordField);
            return movedOffLogin && noPasswordField;
        } catch (Exception e) {
            log.warn("Could not evaluate authentication state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Logs what is actually on the page so a failed selector produces the information
     * needed to fix it, rather than a bare timeout. Prints structure only - headings and
     * link text - never field values, to keep clinical content out of the log file.
     */
    public void dumpPageDiagnostics() {
        try {
            log.info("--- page diagnostics ---");
            log.info("url:   {}", page.url());
            log.info("title: {}", page.title());

            Locator headings = page.locator("h1, h2");
            int headingCount = Math.min(headings.count(), 10);
            for (int i = 0; i < headingCount; i++) {
                String text = headings.nth(i).textContent();
                if (text != null && !text.isBlank()) {
                    log.info("heading: {}", text.strip());
                }
            }

            Locator links = page.locator("a:visible, button:visible");
            int linkCount = Math.min(links.count(), 25);
            for (int i = 0; i < linkCount; i++) {
                String text = links.nth(i).textContent();
                if (text != null && !text.isBlank()) {
                    log.info("clickable: {}", text.strip().replaceAll("\\s+", " "));
                }
            }
            log.info("--- end diagnostics ---");
        } catch (Exception e) {
            log.warn("Could not dump page diagnostics: {}", e.getMessage());
        }
    }

    /** Returns the frame containing the login form, or null when it is on the main page. */
    private Frame findLoginFrame(String usernameSelector) {
        for (Frame frame : page.frames()) {
            if (frame == page.mainFrame()) continue;
            try {
                if (frame.locator(usernameSelector).count() > 0) {
                    return frame;
                }
            } catch (Exception e) {
                // A frame can detach mid-scan - not a failure, just skip it.
                log.debug("Skipped frame {}: {}", frame.url(), e.getMessage());
            }
        }
        return null;
    }

    private void fillAndSubmitInFrame(Frame frame, LoginParams params) {
        waitVisible(frame.locator(params.usernameSelector()), params.formTimeout())
                .fill(params.username());
        waitVisible(frame.locator(params.passwordSelector()), params.formTimeout())
                .fill(params.password());
        waitVisible(frame.locator(params.buttonSelector()), params.formTimeout())
                .click();
    }

    private void fillAndSubmitOnPage(LoginParams params) {
        waitVisible(page.locator(params.usernameSelector()), params.formTimeout())
                .fill(params.username());
        waitVisible(page.locator(params.passwordSelector()), params.formTimeout())
                .fill(params.password());
        waitVisible(page.locator(params.buttonSelector()), params.formTimeout())
                .click();
    }

    private Locator waitVisible(Locator locator, int timeoutMs) {
        locator.first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(timeoutMs));
        return locator.first();
    }

    /** Credentials travel in this record but are never logged - do not add a toString(). */
    public record LoginParams(
            String url,
            String usernameSelector,
            String passwordSelector,
            String buttonSelector,
            String successSelector,
            String username,
            String password,
            int formTimeout,
            int mfaWaitMs) {
    }
}
