package com.example.demo.util;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.twocaptcha.captcha.Normal;

import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;


public class BrowserManager implements AutoCloseable  {
    private final EventLogger eventLogger = new EventLogger(BrowserManager.class);
    private final Playwright playwright ;
    private final Browser browser;
    private final BrowserContext browserContext; // Add a field to manage the browser context
    private final Solver solver = new Solver();
    FileDeleter fileDeleter = new FileDeleter("src/main/screenshots");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static int num = 0; // Appended to name of screenshot file to ensure distinction

    // Define the User-Agent string to spoof as a Windows browser
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36";

    public BrowserManager() {
        this(true);  // Default to headless
    }

    public BrowserManager(boolean isHeadless) {
        fileDeleter.deleteAllFilesInDirectory();
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(isHeadless));
        this.browserContext = browser.newContext(new Browser.NewContextOptions().setUserAgent(USER_AGENT));
    }

    public synchronized Page pageSetup(String userEmail, String userPassword, int attempts) {
        for (int i = 0; i < attempts; i++) {
            Page page = pageSetup(userEmail, userPassword);
            if (page != null) return page;
        }
        return null;
    }

    public synchronized Page pageSetup(String userEmail, String userPassword) {
        Normal captcha = null;
        Page page = null;
        try {
            page = browserContext.newPage();
            page.navigate("https://rockwoodmo.infinitecampus.org/campus/portal/students/rockwood.jsp");

            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Login with RSD Google Account")).click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.getByRole(AriaRole.TEXTBOX).fill(userEmail);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();

            ZonedDateTime nowCST = ZonedDateTime.now(ZoneId.of("America/Chicago"));
            String file = nowCST.format(formatter) + "-" + num + ".png";
            num++;

            page.locator("#captchaimg").screenshot(new Locator.ScreenshotOptions().setPath(Paths.get("src/main/screenshots/" + file)));
            captcha = solver.solveCaptcha(file);
            String solution = captcha.getCode();

            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Type the text you hear or see")).fill(solution);

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();

            // Possibly risky
            //page.waitForURL(Pattern.compile("https:\\/\\/accounts\\.google\\.com\\/v\\d+\\/signin\\/challenge\\/(pwd|identifier)\\?.*"), new Page.WaitForURLOptions().setTimeout(30000));
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.waitForSelector("input[aria-label='Enter your password']", new Page.WaitForSelectorOptions().setTimeout(12000));
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Enter your password")).fill(userPassword);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("Sign in|Next"))).click();
            page.waitForURL(Pattern.compile("https://rockwoodmo\\.infinitecampus\\.org/campus/nav-wrapper/student/portal/student/home.*"), new Page.WaitForURLOptions().setTimeout(25000));
            fileDeleter.deleteAllFilesInDirectory();
            solver.solver.report(captcha.getId(), true);
            return page;
        }
        catch (Exception e) {
            if  (page != null) {
                ZonedDateTime nowCST = ZonedDateTime.now(ZoneId.of("America/Chicago"));
                String errorScreenshot = "Error-" + nowCST.format(formatter)  + ".png";
                page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(("/root/Pictures/" + errorScreenshot))));
                page.close();
            }

            if (captcha != null && !captcha.getCode().isEmpty()){
                try {
                    solver.solver.report(captcha.getId(), false);
                } catch (Exception ex) {
                    eventLogger.logException(ex);
                }
            }
            eventLogger.logException(e);
            return null;
        }
    }


    @Override
    public void close() {
        if (browserContext != null) {
            browserContext.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
