package org.neurobot;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    public static boolean HEADLESS_SELENIDE = false;
    public static final String TOKEN_FILE = "token.txt";
    public static final String USERS_FILE = "users.txt";
    public static final String LOG_FILE = "log.txt";

    public static final String BOT_TOKEN = "7062023995:AAEpyDPS7mtnmkLDI6zhAJX10cBwlb8C4_0";


    public static final long USER_SESSION_TIMEOUT = 30L;

    public static final long TIMEOUT_TO_FIND_ELEMENTS = 4000;

    public static final List<Long> authorizedUsers = new ArrayList<Long>() {{
        add(168171351L);
        add(777777777L);
    }};
}
