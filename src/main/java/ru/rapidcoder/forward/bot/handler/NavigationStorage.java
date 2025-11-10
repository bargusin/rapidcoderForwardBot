package ru.rapidcoder.forward.bot.handler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.forward.bot.component.ChatState;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

class NavigationStorage {
    private static final Logger logger = LoggerFactory.getLogger(NavigationStorage.class);
    private static final String DB_URL = "jdbc:sqlite:";
    private static NavigationStorage instance;
    private final String storageFile;

    private NavigationStorage(String storageFile) {
        logger.info("Initializing NavigationStorage with storage file: {}", storageFile);
        if (StringUtils.isEmpty(storageFile)) {
            throw new IllegalArgumentException("Storage file not defined");
        }
        this.storageFile = storageFile;
        initDataBase();
    }

    public static synchronized NavigationStorage getInstance(String storageFile) {
        if (instance == null) {
            instance = new NavigationStorage(storageFile);
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL + storageFile);
    }

    private void initDataBase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS navigation_history (
                    chat_id INTEGER PRIMARY KEY,
                    state TEXT NOT NULL,
                    context TEXT,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;
        String triggerSql = """
                CREATE TRIGGER IF NOT EXISTS update_timestamp
                AFTER UPDATE ON navigation_history
                FOR EACH ROW
                BEGIN
                    UPDATE navigation_history
                    SET updated_at = CURRENT_TIMESTAMP
                    WHERE chat_id = NEW.chat_id;
                END;
                """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(triggerSql);
            logger.info("Navigation database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize navigation database: {}", e.getMessage(), e);
        }
    }

    public void saveNavigationState(ChatState chatState) {
        String sql = """
                INSERT OR REPLACE INTO navigation_history
                    (chat_id, state, context)
                    VALUES (?, ?, ?)
                """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatState.getChatId());
            stmt.setString(2, chatState.getState());
            stmt.setString(3, chatState.getContext());
            stmt.executeUpdate();
            logger.debug("Saved navigation state for chat {}: ", chatState);
        } catch (SQLException e) {
            throw new IllegalArgumentException(String.format("Failed to save navigation state for chatId %d", chatState.getChatId()), e);
        }
    }

    public Optional<ChatState> getNavigationState(long chatId) {
        String sql = """
                SELECT chat_id, state, context, updated_at FROM navigation_history WHERE chat_id = ? LIMIT 1
                """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                ChatState chatState = new ChatState();
                chatState.setChatId(rs.getLong("chat_id"));
                chatState.setState(rs.getString("state"));
                chatState.setContext(rs.getString("context"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                chatState.setLastUpdated(LocalDateTime.parse(rs.getString("updated_at"), formatter));
                return Optional.of(chatState);
            }
        } catch (SQLException e) {
            logger.error("Failed to get navigation state for chat {}: {}", chatId, e.getMessage(), e);
        }

        return Optional.empty();
    }

    public void clearNavigationState(long chatId) {
        String sql = "DELETE FROM navigation_history WHERE chat_id = ?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.executeUpdate();
            logger.debug("Cleared navigation state for chat {}", chatId);
        } catch (SQLException e) {
            logger.error("Failed to clear navigation state for chat {}: {}", chatId, e.getMessage(), e);
        }
    }
}
