package com.example.storyworthy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UserTest {

    @Test
    fun userInitialization_setsUpDefaultPreferencesAndStats() {
        val user = User("testuser", "password123", "John", "Doe")
        assertEquals("testuser", user.uName)
        assertEquals("John Doe", user.fullName)
        assertNotNull(user.userPrefs)
        assertNotNull(user.userStats)
        assertNotNull(user.userPrefs.desiredKeywords)
        assertEquals(28, user.userPrefs.desiredKeywords.size)
    }

    @Test
    fun defaultConstructor_initializesPreferencesAndStats() {
        val user = User()
        assertNotNull(user.userPrefs)
        assertNotNull(user.userStats)
        assertNotNull(user.userPrefs.desiredKeywords)
    }
}
