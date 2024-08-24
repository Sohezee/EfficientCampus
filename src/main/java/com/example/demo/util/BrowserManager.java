package com.example.demo.util;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;

import java.util.regex.Pattern;


public class BrowserManager implements AutoCloseable  {
    private final EventLogger eventLogger = new EventLogger(BrowserManager.class);
    private final Playwright playwright ;
    private final Browser browser;

    public BrowserManager() {
        this(true);  // Default to headless
    }

    public BrowserManager(boolean isHeadless) {
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(isHeadless));
    }

    public synchronized Page pageSetup(String userEmail, String userPassword, int attempts) {
        for (int i = 0; i < attempts; i++) {
            Page page = pageSetup(userEmail, userPassword);
            if (page != null) return page;
        }
        return null;
    }

    public synchronized Page pageSetup(String userEmail, String userPassword) {
        try {
            Page page = browser.newPage();
            page.navigate("https://rockwoodmo.infinitecampus.org/campus/portal/students/rockwood.jsp");

            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Login with RSD Google Account")).click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.getByRole(AriaRole.TEXTBOX).fill(userEmail);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();

            // Possibly risky
            //page.waitForURL(Pattern.compile("https:\\/\\/accounts\\.google\\.com\\/v\\d+\\/signin\\/challenge\\/(pwd|identifier)\\?.*"), new Page.WaitForURLOptions().setTimeout(30000));
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Enter your password")).fill(userPassword);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("Sign in|Next"))).click();
            page.waitForURL(Pattern.compile("https://rockwoodmo\\.infinitecampus\\.org/campus/nav-wrapper/student/portal/student/home.*"), new Page.WaitForURLOptions().setTimeout(30000));
            return page;

        }
        catch (TimeoutError error) {
            return null;
        }
        catch (Exception e) {
            eventLogger.logException(e);
            return null;
        }
    }


    @Override
    public void close() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
