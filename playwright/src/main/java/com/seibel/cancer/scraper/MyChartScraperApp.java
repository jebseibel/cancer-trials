package com.seibel.cancer.scraper;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.seibel.cancer.scraper.common.ConfigLoader;
import com.seibel.cancer.scraper.common.LoginService;
import com.seibel.cancer.scraper.common.SessionManager;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

/**
 * Step 1 of PLAYWRIGHT_SCRAPE_PLAN.md: log in, persist the session, prove a second run
 * reuses it. Deliberately does NOT scrape anything yet.
 *
 * This step exists on its own because it is the step most likely to fail, so it should be
 * the cheapest to find out about. It answers one question: does session reuse survive on
 * this account, or does MFA force a human into every run? Everything downstream in the
 * plan assumes the former.
 *
 * Run it twice. The first run logs in; the second should print SESSION REUSED.
 */
@Slf4j
public class MyChartScraperApp {

    public static void main(String[] args) {
        ConfigLoader config = new ConfigLoader();
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String username = dotenv.get("MYCHART_USERNAME");
        String password = dotenv.get("MYCHART_PASSWORD");
        if (isBlank(username) || isBlank(password)) {
            log.error("MYCHART_USERNAME / MYCHART_PASSWORD not set. Copy .env.example to .env and fill them in.");
            return;
        }

        String url = config.require("app.url");
        String successSelector = config.require("app.successSelector");
        int formTimeout = config.getInt("app.formTimeout", 15000);
        int mfaWaitMs = config.getInt("app.mfaWaitMs", 0);
        boolean headless = config.getBoolean("app.headless", false);

        SessionManager sessions = new SessionManager(config.require("app.storageStatePath"));
        boolean hadSavedSession = sessions.hasSavedSession();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(headless));

            BrowserContext context = sessions.newContext(browser);
            Page page = context.newPage();

            LoginService loginService = new LoginService(page);

            // Only meaningful when a state file was actually restored. Without this guard the
            // check also passes when Chromium simply still holds a live login from an earlier
            // run, which reports "session reused" while proving nothing about reuse - exactly
            // what happened on the 13:55 run, where it claimed a valid session three seconds
            // after logging that no session file existed.
            boolean reused = hadSavedSession
                    && loginService.alreadyLoggedIn(url, successSelector, formTimeout);

            try {
                if (!reused) {
                    loginService.login(new LoginService.LoginParams(
                            url,
                            config.require("app.usernameSelector"),
                            config.require("app.passwordSelector"),
                            config.require("app.buttonSelector"),
                            successSelector,
                            username,
                            password,
                            formTimeout,
                            mfaWaitMs));
                }
                sessions.save(context);
                report(hadSavedSession, reused);

            } catch (Exception loginFailure) {
                // A wrong successSelector must not throw away a login that actually
                // worked - clearing MFA costs a real interruption for the patient. If the
                // browser looks authenticated, keep the session and report what was on
                // screen so the selector can be corrected without another MFA round.
                log.warn("Post-login marker not found: {}", loginFailure.getMessage());
                loginService.dumpPageDiagnostics();

                if (loginService.looksAuthenticated(url)) {
                    log.warn("Browser appears authenticated despite the marker not matching - "
                            + "saving the session anyway so this login is not wasted.");
                    sessions.save(context);
                } else {
                    log.error("Browser does not appear authenticated - session NOT saved.");
                }
            }

            context.close();
            browser.close();
        } catch (Exception e) {
            log.error("Session check failed: {}", e.getMessage(), e);
        }
    }

    /** The actual result of step 1 - stated plainly, since this run exists to answer one question. */
    private static void report(boolean hadSavedSession, boolean reused) {
        if (!hadSavedSession) {
            log.info("FIRST RUN - logged in and saved the session. Run again to test reuse.");
        } else if (reused) {
            log.info("SESSION REUSED - no login needed. Repeatable scraping is viable.");
        } else {
            log.warn("SESSION EXPIRED - a saved session existed but was no longer valid, "
                    + "so a fresh login was required. If this happens every run, scraping is "
                    + "semi-automated rather than repeatable. Note how long reuse lasted.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
