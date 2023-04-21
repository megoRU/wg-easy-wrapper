package org.megoru.impl;

import org.jetbrains.annotations.Nullable;
import org.megoru.entity.api.Client;
import org.megoru.entity.api.Create;
import org.megoru.entity.api.Session;
import org.megoru.entity.api.Status;
import org.megoru.io.UnsuccessfulHttpException;

import java.io.File;

public interface WgEasyAPI {

    /**
     * Get qr code config
     *
     * @param userId   - it`s userId
     * @param fileName - file name without file extension
     * @return {@link File}
     */
    File getQRCode(String userId, String fileName) throws UnsuccessfulHttpException;

    /**
     * Get user config
     *
     * @param userId   - it`s userId
     * @param fileName - file name without file extension
     * @return {@link File}
     */
    File getConfig(String userId, String fileName) throws UnsuccessfulHttpException;

    /**
     * Create user
     *
     * @param name - The username must be unique! If it is non-unique. When searching for userId, there will be 2 or more values.
     * @return {@link Create}
     */
    Create createClient(String name) throws UnsuccessfulHttpException;

    /**
     * Update user peer address
     *
     * @param userId - it`s userId
     * @return {@link Status}
     */
    Status updateClientAddress(String userId, String address) throws UnsuccessfulHttpException;

    /**
     * Disable user peer
     *
     * @param userId - it`s userId
     * @return {@link Status}
     */
    Status disableClient(String userId) throws UnsuccessfulHttpException;

    /**
     * Enable user peer
     *
     * @param userId - it`s userId
     * @return {@link Status}
     */
    Status enableClient(String userId) throws UnsuccessfulHttpException;

    /**
     * Delete user peer
     *
     * @param userId - it`s userId
     * @return {@link Status}
     */
    Status deleteClient(String userId) throws UnsuccessfulHttpException;

    /**
     * @param userId - it`s userId
     * @param name   - The username must be unique! If it is non-unique. When searching for userId, there will be 2 or more values.
     * @return {@link Status}
     */
    Status renameClient(String userId, String name) throws UnsuccessfulHttpException;

    /**
     * @return {@link Client}
     * @throws IllegalStateException - if more than 1 user
     */
    @Nullable
    Client getClientByName(String name) throws UnsuccessfulHttpException, IllegalStateException, NullPointerException;

    /**
     * @return {@link Client}
     * @throws NullPointerException - if no user
     */
    @Nullable
    Client getClientById(String userId) throws UnsuccessfulHttpException, NullPointerException;

    /**
     * List of Clients
     *
     * @return {@link Client[]}
     */
    Client[] getClients() throws UnsuccessfulHttpException;

    /**
     * Get current Session
     *
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
