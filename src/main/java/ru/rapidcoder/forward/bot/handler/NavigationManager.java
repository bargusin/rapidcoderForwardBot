package ru.rapidcoder.forward.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NavigationManager {
    public static final String KEY_STATE = "state";
    public static final String KEY_CONTEXT = "context";
    public static final String DEFAULT_STATE = "MAIN";
    private static final Logger logger = LoggerFactory.getLogger(NavigationManager.class);
    private static final String DB_URL = "jdbc:sqlite://tmp/menu_bot.db";
    private static NavigationManager instance = new NavigationManager();

    private NavigationManager() {
        initDataBase();
    }

    public static synchronized NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    private static void initDataBase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS navigation_history (
                    chat_id INTEGER PRIMARY KEY,
                    state TEXT NOT NULL,
                    context TEXT,
                    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            logger.info("Navigation database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize navigation database: {}", e.getMessage(), e);
        }
    }

    public void saveNavigationState(long chatId, String state, String context) {
        String sql = """
                INSERT OR REPLACE INTO navigation_history
                    (chat_id, state, context)
                    VALUES (?, ?, ?)
                """;
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, chatId);
            stmt.setString(2, state);
            stmt.setString(3, context);
            stmt.executeUpdate();

            logger.debug("Saved navigation state for user {}: state={}, context={}", chatId, state, context);
        } catch (SQLException e) {
            logger.error("Failed to save navigation state for chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    public Map<String, String> getNavigationState(long chatId) {
        String sql = """
                SELECT state, context FROM navigation_history WHERE chat_id = ? LIMIT 1
                """;
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, String> state = new HashMap<>();
                state.put(KEY_STATE, rs.getString(KEY_STATE));
                state.put(KEY_CONTEXT, rs.getString(KEY_CONTEXT));
                return Collections.unmodifiableMap(state);
            }
        } catch (SQLException e) {
            logger.error("Failed to get navigation state for chat {}: {}", chatId, e.getMessage(), e);
        }

        // Возвращаем состояние по умолчанию
        Map<String, String> defaultState = new HashMap<>();
        defaultState.put(KEY_STATE, DEFAULT_STATE);
        defaultState.put(KEY_CONTEXT, null);
        return defaultState;
    }

    public void clearNavigationState(long chatId) {
        String sql = "DELETE FROM navigation_history WHERE chat_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, chatId);
            stmt.executeUpdate();
            logger.debug("Cleared navigation state for chat {}", chatId);

        } catch (SQLException e) {
            logger.error("Failed to clear navigation state for chat {}: {}", chatId, e.getMessage(), e);
        }
    }
}
