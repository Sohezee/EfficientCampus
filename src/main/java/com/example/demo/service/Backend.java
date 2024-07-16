package com.example.demo.service;

import com.example.demo.dao.UserRepository;
import com.example.demo.entity.Selection;
import com.example.demo.entity.User;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableScheduling
@Service
public class Backend {
    boolean isHeadless = true;
    Playwright playwright = Playwright.create();
    Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(isHeadless));
    Logger logger = LoggerFactory.getLogger(Backend.class);
    private static final StringWriter stringWriter = new StringWriter();
    private static final PrintWriter printWriter = new PrintWriter(stringWriter);

    private UserRepository userRepository;

    @Autowired
    public Backend(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 1 0 * * ?", zone = "America/Chicago")
    public void signUpUsers() {
        List<User> users = userRepository.findAll();
        for(User user: users) {
            signUpAll(user);
        }
    }

    public void signUpAll(User user) {
        Page page = pageSetup(user.getEmail(), user.getPassword());
        ArrayList<Cookie> cookies = new ArrayList<>(page.context().cookies());

        StringBuilder cookiesStr = new StringBuilder();

        for (Cookie cookie : cookies) {
            if (cookie.name.equals("JSESSIONID") || cookie.name.equals("sis-cookie")) cookiesStr.append(cookie.name).append("=").append(cookie.value).append("; ");
            else if (cookie.name.equals("appName")) {
                cookiesStr.append(cookie.name).append("=").append(cookie.value).append(";");
                break;
            }
        }
        page.close();

        OkHttpClient client = new OkHttpClient();

        Request getResponsiveSchedule = new Request.Builder()
                .url("https://rockwoodmo.infinitecampus.org/campus/resources/prism/portal/responsiveSchedule?calendarID="
                        + getCalendarID(user.getEmail(), user.getPassword(), cookiesStr.toString()) + "&personID="
                        + getPersonID(user.getEmail(), user.getPassword(), cookiesStr.toString())+ "&structureID="
                        + getStructureID(user.getEmail(), user.getPassword(), cookiesStr.toString()))
                .addHeader("Cookie", cookiesStr.toString())
                .get()
                .build();

        try {
            okhttp3.Response response = client.newCall(getResponsiveSchedule).execute();
            JSONArray responsiveSessions = new JSONArray(Objects.requireNonNull(response.body(),
                    "Null return from responsiveSchedule endpoint for " + user.getEmail()).string());
            outer:
            for (int i = 0; i < responsiveSessions.length(); i++) {
                Selection selection = responsiveSessions.getJSONObject(i).getString("sessionName").charAt(6) == '1' ?
                        new Selection(user.getOfferingNameOne(), user.getTeacherDisplayOne()) : new Selection(user.getOfferingNameTwo(), user.getTeacherDisplayTwo());
                JSONArray offeringsArray = responsiveSessions.getJSONObject(i).getJSONArray("offerings");
                if (!responsiveSessions.getJSONObject(i).getBoolean("sessionOpen")) continue;
                int offeringID = -1; //Will hold ID of session chosen by user

                for (int j = 0; j < offeringsArray.length(); j++){

                    //Non 0 roster ID indicates the user has signed up for the ac-lab
                    if (offeringsArray.getJSONObject(j).getInt("rosterID") != 0) continue outer;

                    String offeringName = offeringsArray.getJSONObject(j).getString("responsiveOfferingName");
                    String teacherDisplay = offeringsArray.getJSONObject(j).getString("teacherDisplay");

                    //Can't just break when below conditional is met, the loop has to finish to check each rosterID equals 0
                    if (offeringName.equalsIgnoreCase(selection.getOfferingName()) && teacherDisplay.equalsIgnoreCase(selection.getTeacherDisplay()))
                        offeringID = offeringsArray.getJSONObject(j).getInt("responsiveOfferingID");

                }
                if (offeringID == -1) System.out.println("Responsive Offering Not Found"); //Replace with something better
                else signUp(user.getEmail(), user.getPassword(), responsiveSessions.getJSONObject(i).getInt("responsiveSessionID"), offeringID, cookiesStr.toString());
            }

        }
        catch (Exception e) {
            logException(e);
        }
    }

    public boolean signUp(String userEmail, String userPassword, int sessionID, int offeringID, String cookies) {
        OkHttpClient client = new OkHttpClient();

        try {
            RequestBody body = RequestBody.create(MediaType.parse("application/json"),
                    "{\"calendarID\":1078, \"personID\":" + getPersonID(userEmail, userPassword, cookies) +", \"responsiveOfferingID\":" + offeringID + ", \"responsiveSessionID\" :" + sessionID + "}");
            Request signUp = new Request.Builder()
                    .url("https://rockwoodmo.infinitecampus.org/campus/resources/prism/portal/responsiveSchedule/update")
                    .method("POST", body)
                    .addHeader("Cookie", cookies)
                    .addHeader("Content-Type", "application/json")
                    .build();

            okhttp3.Response response = client.newCall(signUp).execute();
            JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response.body(),
                    "Null return from responsiveSchedule/update endpoint whilst attempting to sign up " + userEmail).string());
            return jsonObject.getBoolean("success");

        }
        catch (Exception e) {
            logException(e);
            return false;
        }
    }

    public int getPersonID(String userEmail, String userPassword, String cookies) {
        OkHttpClient client = new OkHttpClient();

        Request getResponsiveSchedule = new Request.Builder()
                .url("https://rockwoodmo.infinitecampus.org/campus/resources/my/userAccount")
                .addHeader("Cookie", cookies)
                .get()
                .build();
        try {
            okhttp3.Response accountInfo = client.newCall(getResponsiveSchedule).execute();
            return new JSONObject(Objects.requireNonNull(accountInfo.body(),
                    "Null return from resources/my/userAccount endpoint for " + userEmail).string()).getInt("personID");

        }
        catch (JSONException je) {
            logException(je, "Unexpected Return From campus/resources/my/userAccount Endpoint");
            return -1;
        }
        catch (Exception e) {
            logException(e);
            return -1;
        }
    }

    public int getStructureID(String userEmail, String userPassword, String cookies) {
        OkHttpClient client = new OkHttpClient();

        Request getResponsiveSchedule = new Request.Builder()
                .url("https://rockwoodmo.infinitecampus.org/campus/resources/portal/generalInfo")
                .addHeader("Cookie", cookies)
                .get()
                .build();
        try {
            okhttp3.Response generalInfo = client.newCall(getResponsiveSchedule).execute();
            JSONObject response  = new JSONObject(Objects.requireNonNull(generalInfo.body(), "Null response from generalInfo endpoint for " + userEmail).string());
            JSONArray enrollments = response.getJSONArray("enrollments");
            JSONObject lafayette = null;
            for (int i = 0; i < enrollments.length(); i++) if(enrollments.getJSONObject(i).getString("schoolName").equals("LAFAYETTE HIGH")) lafayette = enrollments.getJSONObject(i);
            if (lafayette == null) {
                System.out.println("Unable to find Lafayette enrollment");
                return -1;
            }
            return lafayette.getInt("structureID");

        }
        catch (JSONException je) {
            logException(je, "Unexpected Return From campus/resources/my/userAccount Endpoint");
            return -1;
        }
        catch (Exception e) {
            logException(e);
            return -1;
        }
    }

    public int getCalendarID(String userEmail, String userPassword, String cookies) {
        OkHttpClient client = new OkHttpClient();

        Request getResponsiveSchedule = new Request.Builder()
                .url("https://rockwoodmo.infinitecampus.org/campus/resources/portal/generalInfo")
                .addHeader("Cookie", cookies)
                .get()
                .build();
        try {
            okhttp3.Response generalInfo = client.newCall(getResponsiveSchedule).execute();
            JSONObject response  = new JSONObject(Objects.requireNonNull(generalInfo.body(), "Null response from generalInfo endpoint for " + userEmail).string());
            JSONArray enrollments = response.getJSONArray("enrollments");
            JSONObject lafayette = null;
            for (int i = 0; i < enrollments.length(); i++) if(enrollments.getJSONObject(i).getString("schoolName").equals("LAFAYETTE HIGH")) lafayette = enrollments.getJSONObject(i);
            if (lafayette == null) {
                System.out.println("Unable to find Lafayette enrollment");
                return -1;
            }
            return lafayette.getInt("calendarID");

        }
        catch (Exception e) {
            logException(e);
            return -1;
        }

    }


    public Page pageSetup(String userEmail, String userPassword) {
        Page page = browser.newPage();
        try {
            page.navigate("https://rockwoodmo.infinitecampus.org/campus/portal/students/rockwood.jsp");

            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Login with RSD Google Account")).click();
            page.getByRole(AriaRole.TEXTBOX).fill(userEmail);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();

            page.waitForSelector("text='Enter your password'", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.getByRole(AriaRole.TEXTBOX).fill(userPassword);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            if (isHeadless) page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in")).click();
            else page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
            page.waitForURL("https://rockwoodmo.infinitecampus.org/campus/nav-wrapper/student/portal/student/home");
            return page;

        } catch (Exception e) {
            logException(e);
            return null;
        }
    }

    public synchronized void logException(Exception e) {
        stringWriter.getBuffer().setLength(0);
        e.printStackTrace(printWriter);
        logger.error(stringWriter.toString());
    }
    public synchronized void logException(Exception e, String message) {
        stringWriter.getBuffer().setLength(0);
        e.printStackTrace(printWriter);
        logger.error(message + "\n" + stringWriter.toString());
    }
}