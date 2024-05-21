package org.neurobot;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideDriver;
import org.neurobot.Users.UserSession;
import org.neurobot.Users.UserSessionManager;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;


public class MyTelegramBot extends TelegramLongPollingBot {

    private static final long MAX_LOG_SIZE = 500 * 1024 * 1024; // 500 MB
    private final UserSessionManager sessionManager = new UserSessionManager();
    private Set<Long> authorizedUsers = new HashSet<>();
    private final ConcurrentHashMap<Long, Boolean> userStatus = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private String botToken = "";

    public MyTelegramBot() {
        loadToken();
        if (botToken.isEmpty()){
            botToken = Settings.BOT_TOKEN;
        }
        loadAuthorizedUsers();
        authorizedUsers.addAll(Settings.authorizedUsers);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long userId = update.getMessage().getFrom().getId();
            String messageText = update.getMessage().getText();

            if (authorizedUsers.contains(userId)) {
                if (messageText.startsWith("/add_user_")) {
                    addUser(update, messageText);
                } else {
                    //handleUserRequest(update, userId, messageText);
                    executorService.submit(() -> handleUserRequest(update, userId, messageText));
                }
            }
        }
    }

    private void handleUserRequest(Update update, long userId, String messageText) {

        UserSession userSession = sessionManager.getSession(userId);
        sessionManager.updateSessionActivity(userId);

        try {
            System.out.println("Начал обрабатывать запрос от - " + update.getMessage().getChatId());

            SelenideDriver driver = userSession.getDriver();

            System.out.println("Открываю гугль и ищу по фразе - " + messageText);
            driver.open("https://www.google.com");


            driver.$x("//*[@name='q']").shouldBe(Condition.exist);
            if(driver.$x("//*[text()=\"Принять все\"]").exists()){
                driver.$x("//*[text()=\"Принять все\"]").scrollTo().click();
            }

            driver.$x("//*[@name='q']").shouldBe(Condition.visible).setValue(messageText).pressEnter();
            String title = driver.$$("h3").first().shouldBe(Condition.visible).getText();
            System.out.println("Title - " + title);
            String link = driver.$$("cite").first().shouldBe(Condition.visible).getText();
            System.out.println("Link - " + link);

            sendMessage(userId, title + link);

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(userId, "Произошла ошибка при обработке запроса.");
        }

        userStatus.putIfAbsent(userId, false);

//        if (userStatus.get(userId)) {
//            sendMessage(update.getMessage().getChatId(), "Простите, ваш запрос пока обрабатывается, подождите пожалуйста");
//        } else {
//            userStatus.put(userId, true);
//            executorService.submit(() -> {
//                try {
//                    System.out.println("Начал обрабатывать запрос от - " + update.getMessage().getChatId());
//                    String response = processRequest(messageText);
//                    sendMessage(update.getMessage().getChatId(), response);
//                } catch (Exception e) {
//                    logError(userId, e);
//                    sendMessage(update.getMessage().getChatId(), "Произошла ошибка: " + e.getMessage());
//                } finally {
//                    userStatus.put(userId, false);
//                }
//            });
//        }
    }

//    private String processRequest(String query) throws Exception {
//        System.out.println("Открываю гугль и ищу по фразе - " + query);
//        open("https://www.google.com");
//
//        $x("//*[@name='q']").shouldBe(Condition.exist);
//        if($x("//*[text()=\"Принять все\"]").exists()){
//            $x("//*[text()=\"Принять все\"]").scrollTo().click();
//        }
//
//        $x("//*[@name='q']").shouldBe(Condition.visible).setValue(query).pressEnter();
//        String title = $$("h3").first().shouldBe(Condition.visible).getText();
//        System.out.println("Title - " + title);
//        String link = $$("cite").first().shouldBe(Condition.visible).getText();
//        System.out.println("Link - " + link);
//        closeWebDriver();
//        return title + "\n" + link;
//    }

    private void addUser(Update update, String messageText) {
        try {
            long newUserId = Long.parseLong(messageText.split("_")[2]);
            if (authorizedUsers.add(newUserId)) {
                Files.write(Paths.get(Settings.USERS_FILE), (newUserId + "\n").getBytes(), StandardOpenOption.APPEND);
                sendMessage(update.getMessage().getChatId(), "Пользователь добавлен: " + newUserId);
            } else {
                sendMessage(update.getMessage().getChatId(), "Пользователь уже существует в списке: " + newUserId);
            }
        } catch (Exception e) {
            logError(update.getMessage().getFrom().getId(), e);
            sendMessage(update.getMessage().getChatId(), "Произошла ошибка при добавлении пользователя: " + e.getMessage());
        }
    }

    private void loadToken() {
        try (BufferedReader br = new BufferedReader(new FileReader(Settings.TOKEN_FILE))) {
            botToken = br.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAuthorizedUsers() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(Settings.USERS_FILE));
            for (String line : lines) {
                authorizedUsers.add(Long.parseLong(line.trim()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logError(long userId, Exception e) {
        try {
            String errorMessage = "User ID: " + userId + " - " + e.getMessage() + "\n";
            Files.write(Paths.get(Settings.LOG_FILE), errorMessage.getBytes(), StandardOpenOption.APPEND);

            if (Files.size(Paths.get(Settings.LOG_FILE)) > MAX_LOG_SIZE) {
                Files.write(Paths.get(Settings.LOG_FILE), new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

//    private void initializeSelenide() {
//        Configuration.headless = true;
//        Configuration.browser = "chrome";
//        Configuration.downloadsFolder = "/tmp"; // Укажите путь для загрузок, если необходимо
//        System.setProperty("webdriver.chrome.driver", "src/main/resources/chromedriver-mac-arm64/chromedriver"); // Укажите путь к chromedriver
//    }

    @Override
    public String getBotUsername() {
        return "YourBotUsername"; // Укажите имя вашего бота
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}



