package ru.rapidcoder.forward.bot.handler;

import ru.rapidcoder.forward.bot.dto.AccessRequest;
import ru.rapidcoder.forward.bot.dto.PermissionUser;

import java.util.List;
import java.util.Optional;

public class PermissionManager {

    private final PermissionStorage storage;
    private final List<Long> admins;

    public PermissionManager(String storageFile, List<Long> admins) {
        this.storage = PermissionStorage.getInstance(storageFile);
        this.admins = admins;
    }

    public void saveUser(Long userId, String userName) {
        PermissionUser user = new PermissionUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setStatus(PermissionUser.UserStatus.ACTIVE);
        user.setRole(PermissionUser.UserRole.MEMBER);
        storage.saveUser(user);
    }

    public void activeUser(Long userId) {
        storage.updateUserStatus(userId, PermissionUser.UserStatus.ACTIVE);
    }

    public void blockedUser(Long userId) {
        storage.updateUserStatus(userId, PermissionUser.UserStatus.BLOCKED);
    }

    public PermissionUser findUserById(Long userId) {
        return storage.findUserById(userId);
    }

    public AccessRequest findRequestById(Long userId) {
        return storage.findRequestById(userId);
    }

    public void approvedRequest(Long userId) {
        storage.updateRequestStatus(userId, AccessRequest.RequestStatus.APPROVED);
    }

    public void rejectRequest(Long userId) {
        storage.updateRequestStatus(userId, AccessRequest.RequestStatus.REJECT);
    }

    public boolean isAdmin(Long userId) {
        return admins.contains(userId);
    }

    public boolean hasAccess(Long userId) {
        return isAdmin(userId) || Optional.ofNullable(findUserById(userId))
                .map(user -> user.getStatus() == PermissionUser.UserStatus.ACTIVE)
                .orElse(false);
    }

    public void saveRequest(Long userId, String userName) {
        AccessRequest request = new AccessRequest();
        request.setUserId(userId);
        request.setUserName(userName);
        request.setStatus(AccessRequest.RequestStatus.PENDING);
        storage.saveRequestAccess(request);
    }

    public List<AccessRequest> getRequests() {
        return storage.getAllRequests();
    }

    public List<PermissionUser> getUsers() {
        return storage.getAllUsers();
    }
}
