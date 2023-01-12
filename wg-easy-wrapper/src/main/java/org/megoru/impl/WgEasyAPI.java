package org.megoru.impl;

import org.jetbrains.annotations.Nullable;
import org.megoru.entity.api.Clients;
import org.megoru.entity.api.NoContent;
import org.megoru.entity.api.Session;
import org.megoru.io.UnsuccessfulHttpException;

public interface WgEasyAPI {

    NoContent disableClient(String userId) throws UnsuccessfulHttpException;

    NoContent enableClient(String userId) throws UnsuccessfulHttpException;

    NoContent deleteClient(String userId) throws UnsuccessfulHttpException;

    /**
     * @param userId - it`s userId
     * @param name - The username must be unique! If it is non-unique. When searching for userId, there will be 2 or more values.
     * @return {@link NoContent}
     */
    NoContent renameClient(String userId, String name)  throws UnsuccessfulHttpException;

    /**
     * @throws IllegalStateException - if more than 1 user
     * @return {@link Clients}
     */
    @Nullable
    Clients getClientId(String name) throws UnsuccessfulHttpException, IllegalStateException;

    /**
     * List of Clients
     * @return {@link Clients[]}
     */
    Clients[] getClients() throws UnsuccessfulHttpException;

    /**
     * Get current Session
     * @return {@link Session}
     */
    Session getSession() throws UnsuccessfulHttpException;

    /**
     * Setup new Session
     */
    void setSession() throws UnsuccessfulHttpException;

    class Builder {

        // Required
        private String password;
        private boolean devMode;
        private String domain;
        private int port;
        private String ip;

        /**
         * This enables LOGS
         */
        public Builder enableDevMode() {
            this.devMode = true;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }

        /**
         * @param domain It`s domain address with out https://. Example: vpn.megoru.ru
         */
        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        /**
         * @param password Password
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * @throws IllegalArgumentException if password or ip|domain null
         */
        public WgEasyAPI build() {
            if (password == null)
                throw new IllegalArgumentException("The provided password cannot be null!");

            if (domain == null && ip == null)
                throw new IllegalArgumentException("The provided ip and domain cannot be null!");

            if (domain != null && ip == null)
                return new WgEasyAPIImpl(password, domain, devMode);

            if (port > 0)
                return new WgEasyAPIImpl(password, ip, port, devMode);

            throw new IllegalArgumentException("You a made error");
        }

    }

}
