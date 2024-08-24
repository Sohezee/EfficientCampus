package com.example.demo.service;

import com.example.demo.dao.UserRepository;
import com.example.demo.entity.Selection;
import com.example.demo.entity.User;
import com.example.demo.util.BrowserManager;
import com.example.demo.util.EventLogger;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Configuration
@EnableScheduling
@Service
public class Backend {
    BrowserManager browser = new BrowserManager();
    EventLogger eventLogger = new EventLogger(Backend.class);

    private UserRepository userRepository;

    @Autowired
    public Backend(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 1 0 * * ?", zone = "America/Chicago")
    public void signUpUsers() {
        List<User> users = userRepository.findAll();
        List<User> failed = new ArrayList<>();
        for(User user: users) {
            if (!signUpAll(user,3)) failed.add(user);
        }
        for(User user: failed) {
            if (!signUpAll(user,10)) eventLogger.logException("CRITICAL FAILURE: " + user.getEmail() + " NOT SIGNED UP");
        }
    }

    // Boolean does NOT represent successful execution of method, but rather more simply successful page setup
    public boolean signUpAll(User user, int attempts) {
        Page page = browser.pageSetup(user.getEmail(), user.getPassword(),attempts);
        if(page == null) return false;

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
                        + getCalendarID(user.getEmail(), cookiesStr.toString()) + "&personID="
                        + getPersonID(user.getEmail(), cookiesStr.toString())+ "&structureID="
                        + getStructureID(user.getEmail(), cookiesStr.toString()))
                .addHeader("Cookie", cookiesStr.toString())
                .get()
                .build();

        try (okhttp3.Response response = client.newCall(getResponsiveSchedule).execute()) {
            JSONArray responsiveSessions = new JSONArray(Objects.requireNonNull(response.body(),
                    "Null return from responsiveSchedule endpoint for " + user.getEmail()).string());
            outer:
            for (int i = 0; i < responsiveSessions.length(); i++) {
                Selection selection = responsiveSessions.getJSONObject(i).getString("sessionName").charAt(6) == '1' ?
                        new Selection(user.getOfferingNameOne(), user.getTeacherDisplayOne()) : new Selection(user.getOfferingNameTwo(), user.getTeacherDisplayTwo());

                if (selection.getOfferingName().isEmpty()) continue;

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
                else signUp(user.getEmail(), responsiveSessions.getJSONObject(i).getInt("responsiveSessionID"), offeringID, cookiesStr.toString());
            }
        }
        catch (Exception e) {
            eventLogger.logException(e);
        }
        return true;
    }

    public boolean signUp(String userEmail, int sessionID, int offeringID, String cookies) {
        OkHttpClient client = new OkHttpClient();

        try {
            RequestBody body = RequestBody.create(
                    "{\"calendarID\":" + getCalendarID(userEmail, cookies) +", \"personID\":" + getPersonID(userEmail, cookies) + ", \"responsiveOfferingID\":" + offeringID + ", \"responsiveSessionID\" :" + sessionID + "}",
                    MediaType.get("application/json")
            );
            Request signUp = new Request.Builder()
                    .url("https://rockwoodmo.infinitecampus.org/campus/resources/prism/portal/responsiveSchedule/update")
                    .method("POST", body)
                    .addHeader("Cookie", cookies)
                    .addHeader("Content-Type", "application/json")
                    .build();

            okhttp3.Response response = client.newCall(signUp).execute();
            JSONObject jsonObject = new JSONObject(Objects.requireNonNull(response.body(),
                    "Null return from responsiveSchedule/update endpoint whilst attempting to sign up " + userEmail).string());
            response.close();
            return jsonObject.getBoolean("success");

        }
        catch (Exception e) {
            eventLogger.logException(e);
            return false;
        }
    }

    public int getPersonID(String userEmail, String cookies) {
        OkHttpClient client = new OkHttpClient();

        Request getResponsiveSchedule = new Request.Builder()
                .url("https://rockwoodmo.infinitecampus.org/campus/resources/my/userAccount")
                .addHeader("Cookie", cookies)
                .get()
                .build();
        try (okhttp3.Response accountInfo = client.newCall(getResponsiveSchedule).execute()){
            return new JSONObject(Objects.requireNonNull(accountInfo.body(),
                    "Null return from resources/my/userAccount endpoint for " + userEmail).string()).getInt("personID");

        }
        catch (JSONException je) {
            eventLogger.logException(je, "Unexpected Return From campus/resources/my/userAccount Endpoint");
            return -1;
        }
        catch (Exception e) {
            eventLogger.logException(e);
            return -1;
        }
    }

    public int getStructureID(String userEmail, String cookies) {
        OkHttpClient client = new OkHttpClient();

        Request getResponsiveSchedule = new Request.Builder()
                .url("https://rockwoodmo.infinitecampus.org/campus/resources/portal/generalInfo")
                .addHeader("Cookie", cookies)
                .get()
                .build();
        try (okhttp3.Response generalInfo = client.newCall(getResponsiveSchedule).execute()) {
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
            eventLogger.logException(je, "Unexpected Return From campus/resources/my/userAccount Endpoint");
            return -1;
        }
        catch (Exception e) {
            eventLogger.logException(e);
            return -1;
        }
    }

    public int getCalendarID(String userEmail, String cookies) {
        OkHttpClient client = new OkHttpClient();

        Request getResponsiveSchedule = new Request.Builder()
                .url("https://rockwoodmo.infinitecampus.org/campus/resources/portal/generalInfo")
                .addHeader("Cookie", cookies)
                .get()
                .build();
        try {
            okhttp3.Response generalInfo = client.newCall(getResponsiveSchedule).execute();
            JSONObject response  = new JSONObject(Objects.requireNonNull(generalInfo.body(), "Null response from generalInfo endpoint for " + userEmail).string());
            generalInfo.close();
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
            eventLogger.logException(e);
            return -1;
        }

    }

}