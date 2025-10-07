package org.example;

public class PermissionCheckResult {
    private final boolean success;
    private final String errorMessage;

    private PermissionCheckResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static PermissionCheckResult success() {
        return new PermissionCheckResult(true, null);
    }

    public static PermissionCheckResult noPermission(String errorMessage) {
        return new PermissionCheckResult(false, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasError() {
        return !success;
    }
}
