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
     * @param id   - it`s config id
     * @param fileName - file name without file extension
     * @return {@link File}
     */
    File getQRCode(String id, String fileName) throws UnsuccessfulHttpException;

    /**
     * Get user config
     *
     * @param id       - it`s userId
     * @param fileName - file name without file extension
     * @return {@link File}
     */
    File getConfig(String id, String fileName) throws UnsuccessfulHttpException;

    /**
     * Create user
     *
     * @param name - The username must be unique! If it is non-unique. When searching for id, there will be 2 or more values.
     * @return {@link Create}
     */
    Status createClient(String name) throws UnsuccessfulHttpException;

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
     * @param id - it`s config id
     * @return {@link Status}
     */
    Status disableClient(String id) throws UnsuccessfulHttpException;

    /**
     * Enable user peer
     *
     * @param id - it`s config id
     * @return {@link Status}
     */
    Status enableClient(String id) throws UnsuccessfulHttpException;

    /**
     * Delete user peer
     *
     * @param id - it`s config id
     * @return {@link Status}
     */
    Status deleteClient(String id) throws UnsuccessfulHttpException;

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
    Client getClientByName(String name) throws UnsuccessfulHttpException, IllegalStateException;

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
        private String host;

        /**
         * This enables LOGS
         */
        public Builder enableDevMode() {
            this.devMode = true;
            return this;
        }

        /**
         * @param host It`s host address: https://vpn.megoru.ru or http://222.222.222.222:55222
         */
        public Builder host(String host) {
            this.host = host;
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

            if (host == null)
                throw new IllegalArgumentException("The provided host cannot be null!");

            return new WgEasyAPIImpl(password, host, devMode);
        }
    }
}