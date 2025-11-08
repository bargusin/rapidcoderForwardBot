package ru.rapidcoder.forward.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import ru.rapidcoder.forward.bot.component.MonitorChat;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChatManager {
    private static final Logger logger = LoggerFactory.getLogger(ChatManager.class);
    private static final String DB_URL = "jdbc:sqlite:";
    private static ChatManager instance;
    private final String storageFile;

    private ChatManager(String storageFile) {
        this.storageFile = storageFile;

        initDataBase();
    }

    public static synchronized ChatManager getInstance(String storageFile) {
        if (instance == null) {
            instance = new ChatManager(storageFile);
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL + storageFile);
    }

    private void initDataBase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS monitored_chats (
                    chat_id INTEGER PRIMARY KEY,
                    chat_title TEXT NOT NULL,
                    chat_type TEXT NOT NULL,
                    bot_status TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;
        String triggerSqlL = """
                CREATE TRIGGER IF NOT EXISTS update_timestamp
                AFTER UPDATE ON monitored_chats
                FOR EACH ROW
                BEGIN
                    UPDATE monitored_chats
                    SET updated_at = CURRENT_TIMESTAMP
                    WHERE chat_id = NEW.chat_id;
                END;
                """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(triggerSqlL);

            logger.info("Chat's storage database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize chat's storage database: {}", e.getMessage(), e);
        }
    }

    public void saveOrUpdateChat(MonitorChat chat) {
        String sql = """
                INSERT OR REPLACE INTO monitored_chats
                (chat_id, chat_title, chat_type, bot_status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chat.getChatId());
            stmt.setString(2, chat.getChatTitle());
            stmt.setString(3, chat.getChatType());
            stmt.setString(4, chat.getBotStatus());
            stmt.executeUpdate();
            logger.info("Chat saved into database: {}", chat.getChatTitle());
        } catch (SQLException e) {
            logger.error("Failed to save chat by chatId {}: {}", chat.getChatId(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void deleteChat(Long chatId) {
        String sql = "DELETE FROM monitored_chats WHERE chat_id=?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Chat deleted from database: {}", +chatId);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete chat by chatId {}: {}", chatId, e.getMessage(), e);
        }
    }

    public List<MonitorChat> getAllChats() {
        List<MonitorChat> chats = new ArrayList<>();
        String sql = "SELECT * FROM monitored_chats";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                MonitorChat chat = resultSetToChat(rs);
                chats.add(chat);
            }
        } catch (SQLException e) {
            logger.error("Failed to get chats {}", e.getMessage(), e);
        }
        return chats;
    }

    public MonitorChat findChatById(Long chatId) {
        String sql = "SELECT * FROM monitored_chats WHERE chat_id=?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return resultSetToChat(rs);
            }
        } catch (SQLException e) {
            logger.error("Failed to define chat by chatId {}: {}", chatId, e.getMessage(), e);
        }
        return null;
    }

    public void updateBotStatus(Long chatId, String newStatus) {
        String sql = "UPDATE monitored_chats SET bot_status=? WHERE chat_id=?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setLong(2, chatId);
            stmt.executeUpdate();
            logger.info("Chat's staus modified: {}", chatId);
        } catch (SQLException e) {
            logger.error("Failed to modify chat's status by chatId {}: {}", chatId, e.getMessage(), e);
        }
    }

    private MonitorChat resultSetToChat(ResultSet rs) throws SQLException {
        MonitorChat chat = new MonitorChat();
        chat.setChatId(rs.getLong("chat_id"));
        chat.setChatTitle(rs.getString("chat_title"));
        chat.setChatType(rs.getString("chat_type"));
        chat.setBotStatus(rs.getString("bot_status"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        chat.setAddedDate(LocalDateTime.parse(rs.getString("created_at"), formatter));
        chat.setLastActivity(LocalDateTime.parse(rs.getString("updated_at"), formatter));
        return chat;
    }

    public void handleChatMemberUpdate(ChatMemberUpdated chatMember) {
        Chat chat = chatMember.getChat();
        String status = chatMember.getNewChatMember()
                .getStatus();
        Long chatId = chat.getId();
        String chatType = getChatType(chat);
        logger.info("Bot's status changed from chat '{}' to '{}'", chat.getTitle(), status);
        switch (status) {
            case "administrator":
                addOrUpdateChat(chatId, chat.getTitle(), chatType, status);
            case "member":
            case "restricted":
                addOrUpdateChat(chatId, chat.getTitle(), chatType, status);
                break;
            case "left":
            case "kicked":
                deleteChat(chatId);
                break;
            default:
                logger.info("Unknown status {}", status);
                break;
        }
    }

    private String getChatType(Chat chat) {
        if (chat.isChannelChat())
            return "channel";
        if (chat.isGroupChat())
            return "group";
        if (chat.isSuperGroupChat())
            return "supergroup";
        if (chat.isUserChat())
            return "private";
        return "unknown";
    }

    private void addOrUpdateChat(Long chatId, String chatTitle, String chatType, String botStatus) {
        MonitorChat chat = new MonitorChat();
        chat.setChatId(chatId);
        chat.setChatTitle(chatTitle);
        chat.setChatType(chatType);
        chat.setBotStatus(botStatus);
        saveOrUpdateChat(chat);
    }
}
