package ru.rapidcoder.forward.bot.handler.test;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.Main;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.List;

import static ch.qos.logback.classic.Level.ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
public class MainTest {

    private ListAppender<ILoggingEvent> listAppender;

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(Main.class);

        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);
    }

    @AfterEach
    public void cleanup() {
        Logger logger = (Logger) LoggerFactory.getLogger(Main.class);
        logger.detachAppender(listAppender);
    }

    @Test
    public void testCompleteBotScenario() throws Exception {
        new EnvironmentVariables().set("botEnv", "dev")
                .set("devBotName", "MyBot")
                .set("devBotToken", "token123")
                .set("devStorageFile", "storage.db")
                .set("devAdmins", "1,2")
                .execute(() -> {
                    String environment = System.getenv("botEnv") != null ? System.getenv("botEnv") : "dev";
                    String botName = System.getenv(environment + "BotName");
                    String botToken = System.getenv(environment + "BotToken");
                    String storageFile = System.getenv(environment + "StorageFile");
                    String admins = System.getenv(environment + "Admins");

                    assertEquals("dev", environment);
                    assertEquals("MyBot", botName);
                    assertEquals("token123", botToken);
                    assertEquals("storage.db", storageFile);
                    assertEquals("1,2", admins);

                });
    }

    @Test
    public void testDevEnvironmentByDefault() throws Exception {
        new EnvironmentVariables().set("devBotName", "MyDevBot")
                .execute(() -> {
                    String environment = System.getenv("botEnv") != null ? System.getenv("botEnv") : "dev";
                    String botName = System.getenv(environment + "BotName");

                    assertEquals("dev", environment);
                    assertEquals("MyDevBot", botName);
                });
    }

    @Test
    public void testMissingBotName() throws Exception {
        new EnvironmentVariables().set("botEnv", "prod")
                .execute(() -> {
                    String environment = System.getenv("botEnv") != null ? System.getenv("botEnv") : "dev";
                    String botName = System.getenv(environment + "BotName");

                    assertEquals("prod", environment);
                    assertNull(botName);
                });
    }

    @Test
    public void testMainWithException() {
        Main.main(new String[]{});

        List<ILoggingEvent> logs = listAppender.list;
        assertFalse(logs.isEmpty());

        boolean hasError = logs.stream()
                .anyMatch(event -> event.getLevel() == ERROR && event.getMessage()
                        .contains("Role's model not defined in environment"));
        assertTrue(hasError);
    }

    @Test
    void testMainWithEnvironmentVariables() throws TelegramApiException {
        environmentVariables.set("botEnv", "dev");
        environmentVariables.set("devBotName", "testBot");
        environmentVariables.set("devTokenId", "testToken");
        environmentVariables.set("devStorageFile", "storage.json");
        environmentVariables.set("devAdmins", "1,2");
        try (MockedStatic<Main> mainStatic = mockStatic(Main.class, CALLS_REAL_METHODS)) {
            TelegramBotsApi mockApi = mock(TelegramBotsApi.class);
            Bot mockBot = mock(Bot.class);

            mainStatic.when(() -> Main.createTelegramBotsApi())
                    .thenReturn(mockApi);
            mainStatic.when(() -> Main.createBot(anyString(), anyString(), anyString(), anyList()))
                    .thenReturn(mockBot);

            Main.main(new String[]{});

            verify(mockApi).registerBot(mockBot);
        }
    }

    @Test
    void testMainWithEmptyAdminsVariable() throws TelegramApiException {
        environmentVariables.set("botEnv", "dev");
        environmentVariables.set("devBotName", "testBot");
        environmentVariables.set("devTokenId", "testToken");
        environmentVariables.set("devStorageFile", "storage.json");
        environmentVariables.set("devAdmins", "");
        try (MockedStatic<Main> mainStatic = mockStatic(Main.class, CALLS_REAL_METHODS)) {
            TelegramBotsApi mockApi = mock(TelegramBotsApi.class);
            Bot mockBot = mock(Bot.class);

            mainStatic.when(() -> Main.createTelegramBotsApi())
                    .thenReturn(mockApi);
            mainStatic.when(() -> Main.createBot(anyString(), anyString(), anyString(), anyList()))
                    .thenReturn(mockBot);

            Main.main(new String[]{});

            verify(mockApi, never()).registerBot(mockBot);
        }
    }

    @Test
    void testMainWithFailAdminsVariable() throws TelegramApiException {
        environmentVariables.set("botEnv", "dev");
        environmentVariables.set("devBotName", "testBot");
        environmentVariables.set("devTokenId", "testToken");
        environmentVariables.set("devStorageFile", "storage.json");
        environmentVariables.set("devAdmins", "TEST");
        try (MockedStatic<Main> mainStatic = mockStatic(Main.class, CALLS_REAL_METHODS)) {
            TelegramBotsApi mockApi = mock(TelegramBotsApi.class);
            Bot mockBot = mock(Bot.class);

            mainStatic.when(() -> Main.createTelegramBotsApi())
                    .thenReturn(mockApi);
            mainStatic.when(() -> Main.createBot(anyString(), anyString(), anyString(), anyList()))
                    .thenReturn(mockBot);

            Main.main(new String[]{});

            verify(mockApi, never()).registerBot(mockBot);
        }
    }

}
