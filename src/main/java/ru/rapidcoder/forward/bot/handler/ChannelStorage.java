package ru.rapidcoder.forward.bot.handler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.forward.bot.dto.BaseChatMembership;
import ru.rapidcoder.forward.bot.dto.ChatMembership;
import ru.rapidcoder.forward.bot.dto.HistoryChatMembership;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

class ChannelStorage {
    private static final Logger logger = LoggerFactory.getLogger(ChannelStorage.class);
    private static final String DB_URL = "jdbc:sqlite:";
    private static ChannelStorage instance;
    private final String storageFile;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ChannelStorage(String storageFile) {
        logger.info("Initializing ChatStorage with storage file: {}", storageFile);
        if (StringUtils.isEmpty(storageFile)) {
            throw new IllegalArgumentException("Storage file not defined");
        }
        this.storageFile = storageFile;
        initDataBase();
    }

    public static synchronized ChannelStorage getInstance(String storageFile) {
        if (instance == null) {
            instance = new ChannelStorage(storageFile);
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL + storageFile);
    }

    private void initDataBase() {
        String sqlMonitoredChats = """
                CREATE TABLE IF NOT EXISTS monitored_chats (
                    chat_id INTEGER PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    user_name TEXT NOT NULL,
                    chat_title TEXT NOT NULL,
                    chat_type TEXT NOT NULL,
                    bot_new_status TEXT NOT NULL,
                    bot_old_status TEXT,
                    deleted INTEGER DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;
        String sqlMonitoredChatsUpdate = """
                CREATE TRIGGER IF NOT EXISTS monitored_chats_update
                AFTER UPDATE ON monitored_chats
                FOR EACH ROW
                BEGIN
                    UPDATE monitored_chats
                    SET updated_at = CURRENT_TIMESTAMP
                    WHERE chat_id = NEW.chat_id;

                    INSERT INTO history_monitored_chats
                    (chat_id, user_id, user_name, chat_title, chat_type, bot_new_status, bot_old_status, deleted)
                    VALUES
                    (NEW.chat_id, NEW.user_id, NEW.user_name, NEW.chat_title, NEW.chat_type, NEW.bot_new_status, NEW.bot_old_status, NEW.deleted);
                END;
                """;
        String sqlHistoryMonitoredChats = """
                CREATE TABLE IF NOT EXISTS history_monitored_chats (
                    chat_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    user_name TEXT NOT NULL,
                    chat_title TEXT NOT NULL,
                    chat_type TEXT NOT NULL,
                    bot_new_status TEXT NOT NULL,
                    bot_old_status TEXT,
                    deleted INTEGER,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;
        String sqlTriggerMonitoredChatsInsert = """
                CREATE TRIGGER IF NOT EXISTS monitored_chats_insert
                AFTER INSERT ON monitored_chats
                BEGIN
                    INSERT INTO history_monitored_chats
                    (chat_id, user_id, user_name, chat_title, chat_type, bot_new_status, bot_old_status, deleted)
                    VALUES
                    (NEW.chat_id, NEW.user_id, NEW.user_name, NEW.chat_title, NEW.chat_type, NEW.bot_new_status, NEW.bot_old_status, NEW.deleted);
                END;
                """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlMonitoredChats);
            stmt.execute(sqlHistoryMonitoredChats);
            stmt.execute(sqlMonitoredChatsUpdate);
            stmt.execute(sqlTriggerMonitoredChatsInsert);
            logger.info("Chat's storage database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize chat's storage database: {}", e.getMessage(), e);
        }
    }

    public void saveOrUpdateChat(ChatMembership chat) {
        String sql = """
                INSERT OR REPLACE INTO monitored_chats
                    (chat_id, user_id, user_name, chat_title, chat_type, bot_new_status, bot_old_status)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chat.getChatId());
            stmt.setLong(2, chat.getUserId());
            stmt.setString(3, chat.getUserName());
            stmt.setString(4, chat.getChatTitle());
            stmt.setString(5, chat.getChatType());
            stmt.setString(6, chat.getBotNewStatus());
            stmt.setString(7, chat.getBotOldStatus());
            stmt.executeUpdate();
            logger.debug("Information of chat '{}' saved into database", chat.getChatTitle());
        } catch (SQLException e) {
            throw new IllegalArgumentException(String.format("Failed to save chat by chatId %d", chat.getChatId()), e);
        }
    }

    public void deleteChat(Long chatId) {
        String sql = "UPDATE monitored_chats SET deleted=? WHERE chat_id=?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, 1);
            stmt.setLong(2, chatId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                logger.debug("Chat deleted from database: {}", +chatId);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete chat by chatId {}: {}", chatId, e.getMessage(), e);
        }
    }

    public List<ChatMembership> getAllChats() {
        List<ChatMembership> chats = new ArrayList<>();
        String sql = """
                    SELECT
                        chat_id,
                        user_id,
                        user_name,
                        chat_title,
                        chat_type,
                        bot_new_status,
                        bot_old_status,
                        created_at,
                        updated_at
                    FROM monitored_chats
                    WHERE deleted=?
                """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, 0);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ChatMembership chat = resultSetToChat(rs);
                chats.add(chat);
            }
        } catch (SQLException e) {
            logger.error("Failed to get chats {}", e.getMessage(), e);
        }
        return chats;
    }

    public List<HistoryChatMembership> getHistoryChats() {
        List<HistoryChatMembership> chats = new ArrayList<>();
        String sql = """
                    SELECT
                        chat_id,
                        user_id,
                        user_name,
                        chat_title,
                        chat_type,
                        bot_new_status,
                        bot_old_status,
                        created_at,
                        deleted
                    FROM history_monitored_chats
                    ORDER BY created_at DESC LIMIT 20
                """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                HistoryChatMembership chat = resultSetToHistoryChat(rs);
                chats.add(chat);
            }
        } catch (SQLException e) {
            logger.error("Failed to get chats {}", e.getMessage(), e);
        }
        return chats;
    }

    public ChatMembership findChatById(Long chatId) {
        String sql = """
                    SELECT
                        chat_id,
                        user_id,
                        user_name,
                        chat_title,
                        chat_type,
                        bot_new_status,
                        bot_old_status,
                        created_at,
                        updated_at
                    FROM monitored_chats WHERE chat_id=? AND deleted=?
                """;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            stmt.setInt(2, 0);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return resultSetToChat(rs);
            }
        } catch (SQLException e) {
            logger.error("Failed to define chat by chatId {}: {}", chatId, e.getMessage(), e);
        }
        return null;
    }

    public void updateBotStatus(Long chatId, String newStatus, String oldStatus) {
        String sql = "UPDATE monitored_chats SET bot_new_status=?, bot_old_status=? WHERE chat_id=?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, oldStatus);
            stmt.setLong(3, chatId);
            stmt.executeUpdate();
            logger.debug("Chat's status modified: {}", chatId);
        } catch (SQLException e) {
            logger.error("Failed to modify chat's status by chatId {}: {}", chatId, e.getMessage(), e);
        }
    }

    private ChatMembership resultSetToChat(ResultSet rs) throws SQLException {
        ChatMembership chat = new ChatMembership();
        resultSetToBaseChat(rs, chat);
        chat.setUpdatedDate(LocalDateTime.parse(rs.getString("updated_at"), formatter));
        return chat;
    }

    private HistoryChatMembership resultSetToHistoryChat(ResultSet rs) throws SQLException {
        HistoryChatMembership chat = new HistoryChatMembership();
        resultSetToBaseChat(rs, chat);
        chat.setDeleted(rs.getInt("deleted") == 1);
        return chat;
    }

    private void resultSetToBaseChat(ResultSet rs, BaseChatMembership chat) throws SQLException {
        chat.setChatId(rs.getLong("chat_id"));
        chat.setUserId(rs.getLong("user_id"));
        chat.setUserName(rs.getString("user_name"));
        chat.setChatTitle(rs.getString("chat_title"));
        chat.setChatType(rs.getString("chat_type"));
        chat.setBotNewStatus(rs.getString("bot_new_status"));
        chat.setBotOldStatus(rs.getString("bot_old_status"));
        chat.setAddedDate(LocalDateTime.parse(rs.getString("created_at"), formatter));

    }
}
