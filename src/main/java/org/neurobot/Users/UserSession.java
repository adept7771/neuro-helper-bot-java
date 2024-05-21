package org.neurobot.Users;

import com.codeborne.selenide.SelenideDriver;

public class UserSession {
    private SelenideDriver driver;
    private long lastActivityTime;

    public UserSession(SelenideDriver driver) {
        this.driver = driver;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public SelenideDriver getDriver() {
        return driver;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void updateLastActivityTime() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    public void close() {
        driver.close();
    }
}
