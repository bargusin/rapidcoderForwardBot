package ru.rapidcoder.forward.bot.handler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.forward.bot.dto.AccessRequest;
import ru.rapidcoder.forward.bot.dto.PermissionUser;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PermissionStorage {

    private static final Logger logger = LoggerFactory.getLogger(ChannelStorage.class);
    private static final String DB_URL = "jdbc:sqlite:";
    private static PermissionStorage instance;
    private final String storageFile;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PermissionStorage(String storageFile) {
        logger.info("Initializing PermissionStorage with storage file: {}", storageFile);
        if (StringUtils.isEmpty(storageFile)) {
            throw new IllegalArgumentException("Storage file not defined");
        }
        this.storageFile = storageFile;
        initDataBase();
    }

    public static synchronized PermissionStorage getInstance(String storageFile) {
        if (instance == null) {
            instance = new PermissionStorage(storageFile);
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL + storageFile);
    }

    private void initDataBase() {
        String sqlUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id INTEGER PRIMARY KEY,
                    user_name TEXT NOT NULL,
                    status TEXT NOT NULL,
                    role TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;
        String sqlAccessRequests = """
                CREATE TABLE IF NOT EXISTS access_requests (
                    user_id INTEGER PRIMARY KEY,
                    user_name TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlAccessRequests);
            logger.info("Permission storage database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize permission storage database: {}", e.getMessage(), e);
        }
    }

    public void saveUser(PermissionUser user) {
        String sql = """
                INSERT OR REPLACE INTO users
                    (user_id, user_name, status, role)
                    VALUES (?, ?, ?, ?)
                """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, user.getUserId());
            stmt.setString(2, user.getUserName());
            stmt.setString(3, user.getStatus()
                    .toString());
            stmt.setString(4, user.getRole()
                    .toString());
            stmt.executeUpdate();
            logger.debug("Information of user '{}' by userId={} saved into database", user.getUserName(), user.getUserId());
        } catch (SQLException e) {
            throw new IllegalArgumentException(String.format("Failed to save user by userName %s", user.getUserName()), e);
        }
    }

    public void saveRequestAccess(AccessRequest request) {
        String sql = """
                INSERT OR REPLACE INTO access_requests
                    (user_id, user_name, status)
                    VALUES (?, ?, ?)
                """;
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, request.getUserId());
            stmt.setString(2, request.getUserName());
            stmt.setString(3, request.getStatus()
                    .toString());
            stmt.executeUpdate();
            logger.debug("Information of access request by userName '{}' and userId={} saved into database", request.getUserName(), request.getUserId());
        } catch (SQLException e) {
            throw new IllegalArgumentException(String.format("Failed to save access request by userName %s", request.getUserName()), e);
        }
    }

    public void updateUserStatus(Long userId, PermissionUser.UserStatus status) {
        String sql = "UPDATE users SET status=? WHERE user_id=?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.toString());
            stmt.setLong(2, userId);
            stmt.executeUpdate();
            logger.debug("User permission status modified: {}", userId);
        } catch (SQLException e) {
            logger.error("Failed to modify permission user status by userId {}: {}", userId, e.getMessage(), e);
        }
    }

    public void updateRequestStatus(Long userId, AccessRequest.RequestStatus status) {
        String sql = "UPDATE access_requests SET status=? WHERE user_id=?";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.toString());
            stmt.setLong(2, userId);
            stmt.executeUpdate();
            logger.debug("Access request status modified: {}", userId);
        } catch (SQLException e) {
            logger.error("Failed to modify access request status by userId {}: {}", userId, e.getMessage(), e);
        }
    }

    public PermissionUser findUserById(Long userId) {
        String sql = """
                    SELECT
                        user_id,
                        user_name,
                        status,
                        role,
                        created_at
                    FROM users WHERE user_id=?
                """;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return resultSetPermissionUser(rs);
            }
        } catch (SQLException e) {
            logger.error("Failed to define user by userId {}: {}", userId, e.getMessage(), e);
        }
        return null;
    }

    public AccessRequest findRequestById(Long userId) {
        String sql = """
                    SELECT
                        user_id,
                        user_name,
                        status,
                        created_at
                    FROM access_requests WHERE user_id=?
                """;

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return resultSetAccessRequest(rs);
            }
        } catch (SQLException e) {
            logger.error("Failed to define user by userId {}: {}", userId, e.getMessage(), e);
        }
        return null;
    }

    public List<AccessRequest> getAllRequests() {
        String sql = """
                    SELECT
                        user_id,
                        user_name,
                        status,
                        created_at
                    FROM access_requests
                    WHERE status=?
                """;
        List<AccessRequest> requests = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, AccessRequest.RequestStatus.PENDING.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                AccessRequest request = resultSetAccessRequest(rs);
                requests.add(request);
            }
        } catch (SQLException e) {
            logger.error("Failed to get all access requests {}", e.getMessage(), e);
        }
        return requests;
    }

    public List<PermissionUser> getAllUsers() {
        String sql = """
                    SELECT
                        user_id,
                        user_name,
                        status,
                        role,
                        created_at
                    FROM users
                """;
        List<PermissionUser> users = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PermissionUser user = resultSetPermissionUser(rs);
                users.add(user);
            }
        } catch (SQLException e) {
            logger.error("Failed to get all users {}", e.getMessage(), e);
        }
        return users;
    }

    private PermissionUser resultSetPermissionUser(ResultSet rs) throws SQLException {
        PermissionUser user = new PermissionUser();
        user.setUserId(rs.getLong("user_id"));
        user.setUserName(rs.getString("user_name"));
        user.setStatus(PermissionUser.UserStatus.valueOf(rs.getString("status")));
        user.setRole(PermissionUser.UserRole.valueOf(rs.getString("role")));
        user.setAddedDate(LocalDateTime.parse(rs.getString("created_at"), formatter));
        return user;
    }

    private AccessRequest resultSetAccessRequest(ResultSet rs) throws SQLException {
        AccessRequest request = new AccessRequest();
        request.setUserId(rs.getLong("user_id"));
        request.setUserName(rs.getString("user_name"));
        request.setStatus(AccessRequest.RequestStatus.valueOf(rs.getString("status")));
        request.setAddedDate(LocalDateTime.parse(rs.getString("created_at"), formatter));
        return request;
    }

}
