package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.rapidcoder.forward.bot.dto.AccessRequest;
import ru.rapidcoder.forward.bot.dto.PermissionUser;
import ru.rapidcoder.forward.bot.handler.PermissionManager;
import ru.rapidcoder.forward.bot.handler.PermissionStorage;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

public class PermissionManagerTest {

    private static final String TEST_DB = "test_chat.db";
    private PermissionManager permissionManager;

    @BeforeEach
    void setUp() throws Exception {
        resetPermissionStorageSingleton();
        permissionManager = new PermissionManager(TEST_DB, List.of(100L));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void tearDown() throws Exception {
        new File(TEST_DB).delete();
    }

    private void resetPermissionStorageSingleton() throws Exception {
        Field instanceField = PermissionStorage.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSaveUser() {
        assertThrows(IllegalArgumentException.class, () -> {
            permissionManager.saveUser(1L, null);
        });

        permissionManager.saveUser(1L, "userName");
        PermissionUser user = permissionManager.findUserById(1L)
                .orElseThrow();
        assertThat(user.getUserId()).isEqualTo(1L);
        assertThat(user.getUserName()).isEqualTo("userName");
        assertThat(user.getStatus()).isEqualTo(PermissionUser.UserStatus.ACTIVE);
        assertThat(user.getRole()).isEqualTo(PermissionUser.UserRole.MEMBER);
    }

    @Test
    void testSaveAccessRequest() {
        assertThrows(IllegalArgumentException.class, () -> {
            permissionManager.saveRequest(1L, null);
        });

        permissionManager.saveRequest(1L, "userName");
        List<AccessRequest> requests = permissionManager.getRequests();
        assertThat(requests.size()).isEqualTo(1);
    }

    @Test
    void testGetAccessRequests() {
        permissionManager.saveRequest(1L, "userName");
        List<AccessRequest> requests = permissionManager.getRequests();
        assertThat(requests.size()).isEqualTo(1);

        permissionManager.saveRequest(1L, "userName");
        requests = permissionManager.getRequests();
        assertThat(requests.size()).isEqualTo(1);

        AccessRequest request = requests.get(0);
        assertThat(request.getUserId()).isEqualTo(1L);
        assertThat(request.getUserName()).isEqualTo("userName");
        assertThat(request.getStatus()).isEqualTo(AccessRequest.RequestStatus.PENDING);
    }

    @Test
    void testApprovedAccessRequest() {
        permissionManager.saveRequest(1L, "userName");
        List<AccessRequest> requests = permissionManager.getRequests();
        assertThat(requests.size()).isEqualTo(1);

        AccessRequest request = requests.get(0);
        assertThat(request.getUserId()).isEqualTo(1L);
        assertThat(request.getUserName()).isEqualTo("userName");
        assertThat(request.getStatus()).isEqualTo(AccessRequest.RequestStatus.PENDING);

        permissionManager.approvedRequest(1L);
        requests = permissionManager.getRequests();
        assertThat(requests.size()).isEqualTo(0);

        request = permissionManager.findRequestById(1L).get();
        assertThat(request.getUserId()).isEqualTo(1L);
        assertThat(request.getUserName()).isEqualTo("userName");
        assertThat(request.getStatus()).isEqualTo(AccessRequest.RequestStatus.APPROVED);
    }

    @Test
    void testRejectAccessRequest() {
        permissionManager.saveRequest(1L, "userName");
        List<AccessRequest> requests = permissionManager.getRequests();
        assertThat(requests.size()).isEqualTo(1);

        AccessRequest request = requests.get(0);
        assertThat(request.getUserId()).isEqualTo(1L);
        assertThat(request.getUserName()).isEqualTo("userName");
        assertThat(request.getStatus()).isEqualTo(AccessRequest.RequestStatus.PENDING);

        permissionManager.rejectRequest(1L);
        requests = permissionManager.getRequests();
        assertThat(requests.size()).isEqualTo(0);

        request = permissionManager.findRequestById(1L).get();
        assertThat(request.getUserId()).isEqualTo(1L);
        assertThat(request.getUserName()).isEqualTo("userName");
        assertThat(request.getStatus()).isEqualTo(AccessRequest.RequestStatus.REJECT);
    }

    @Test
    void testFindUser() {
        permissionManager.saveUser(1L, "userName");
        Optional<PermissionUser> user = permissionManager.findUserById(1L);
        assertThat(user).isPresent();
        user = permissionManager.findUserById(2L);
        assertThat(user).isNotPresent();
    }

    @Test
    void testUpdateStatus() {
        permissionManager.saveUser(1L, "userName");

        PermissionUser user = permissionManager.findUserById(1L)
                .orElseThrow();
        assertThat(user.getUserId()).isEqualTo(1L);
        assertThat(user.getUserName()).isEqualTo("userName");
        assertThat(user.getStatus()).isEqualTo(PermissionUser.UserStatus.ACTIVE);

        permissionManager.activeUser(1L);
        user = permissionManager.findUserById(1L)
                .orElseThrow();
        assertThat(user.getStatus()).isEqualTo(PermissionUser.UserStatus.ACTIVE);

        permissionManager.blockedUser(1L);
        user = permissionManager.findUserById(1L)
                .orElseThrow();
        assertThat(user.getStatus()).isEqualTo(PermissionUser.UserStatus.BLOCKED);
    }
}
