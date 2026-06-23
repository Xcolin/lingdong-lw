package com.lingdong.payroll.security;

public record DataScope(boolean unrestricted, Long createdBy) {

    public static DataScope forUser(CurrentUser currentUser) {
        if (currentUser.isAdmin()) {
            return new DataScope(true, null);
        }
        return new DataScope(false, currentUser.id());
    }

}
