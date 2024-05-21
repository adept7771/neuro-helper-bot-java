package org.neurobot.Users;
import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;
import org.neurobot.Settings;
import org.openqa.selenium.MutableCapabilities;

import java.util.concurrent.*;

public class UserSessionManager {

    private final ConcurrentHashMap<Long, UserSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final long sessionTimeout = TimeUnit.MINUTES.toMillis(Settings.USER_SESSION_TIMEOUT); // 30 минут

    public UserSessionManager() {
        scheduler.scheduleAtFixedRate(this::removeInactiveSessions, 1, 1, TimeUnit.MINUTES);
    }

    public UserSession getSession(long userId) {
        return sessions.computeIfAbsent(userId, id -> createNewSession());
    }

    private UserSession createNewSession() {
        System.setProperty("chromeoptions.args", "--disable-notifications");
        System.setProperty("chromeoptions.args", "--ignore-certificate-errors");
        System.setProperty("chromeoptions.args", "--disable-popup-blocking");
        System.setProperty("chromeoptions.args", "--disable-default-apps");

        MutableCapabilities capabilities = new MutableCapabilities();
        capabilities.setCapability("chromeoptions.args", "--disable-notifications");
        capabilities.setCapability("chromeoptions.args", "--ignore-certificate-errors");
        capabilities.setCapability("chromeoptions.args", "--disable-popup-blocking");
        capabilities.setCapability("chromeoptions.args", "--disable-default-apps");

        SelenideDriver driver = new SelenideDriver(new SelenideConfig()
                .browser("chrome")
                .headless(Settings.HEADLESS_SELENIDE)
                .browserCapabilities(capabilities)
                .timeout(Settings.TIMEOUT_TO_FIND_ELEMENTS));

        return new UserSession(driver);
    }

    private void removeInactiveSessions() {
        long currentTime = System.currentTimeMillis();
        sessions.forEach((userId, session) -> {
            if (currentTime - session.getLastActivityTime() > sessionTimeout) {
                session.close();
                sessions.remove(userId);
            }
        });
    }

    public void updateSessionActivity(long userId) {
        UserSession session = sessions.get(userId);
        if (session != null) {
            session.updateLastActivityTime();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        sessions.values().forEach(UserSession::close);
    }
}
