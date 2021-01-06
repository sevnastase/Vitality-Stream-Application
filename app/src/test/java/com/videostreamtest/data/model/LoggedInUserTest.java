package com.videostreamtest.data.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoggedInUserTest {

    @DisplayName("LoggedInUser:: Check if UserId is filled")
    @Test
    void testWhenUseridIsFilled() {
        final LoggedInUser loggedInUser = new LoggedInUser("TestId", "RickyvanRijn");
        assertEquals("TestId", loggedInUser.getUserId());
    }

    @DisplayName("LoggedInUser:: Check if displayName is filled")
    @Test
    void testWhenUserDisplayNameIsFilled() {
        final LoggedInUser loggedInUser = new LoggedInUser("TestId", "RickyvanRijn");
        assertEquals("RickyvanRijn", loggedInUser.getDisplayName());
    }

}