package org.megoru.entity.api;

public class Session {

    private boolean requiresPassword;
    private boolean authenticated;

    public boolean isRequiresPassword() {
        return requiresPassword;
    }

    public void setRequiresPassword(boolean requiresPassword) {
        this.requiresPassword = requiresPassword;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
