package com.svvaap.bookhive.data;

import com.svvaap.bookhive.data.model.LoggedInUser;

import java.io.IOException;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    public Result<LoggedInUser> login(String username, String password) {

        try {
            // Admin credentials check
            if ("admin@gmail.com".equals(username) && "admin@123".equals(password)) {
                LoggedInUser adminUser = new LoggedInUser(
                        "admin-id",
                        "Admin");
                return new Result.Success<>(adminUser);
            }
            // TODO: handle loggedInUser authentication
            LoggedInUser fakeUser =
                    new LoggedInUser(
                            java.util.UUID.randomUUID().toString(),
                            "Jane Doe");
            return new Result.Success<>(fakeUser);
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}