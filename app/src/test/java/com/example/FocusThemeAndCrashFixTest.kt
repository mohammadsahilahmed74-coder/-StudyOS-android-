package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.FocusVisualTheme
import com.example.ui.viewmodel.FocusViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusThemeAndCrashFixTest {

    private lateinit var application: Application

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences("studyos_focus_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testFocusViewModelInitializationDoesNotCrash() {
        val viewModel = FocusViewModel(application)
        val state = viewModel.uiState.value

        assertNotNull(state)
        assertFalse(state.isError)
        assertNull(state.initializationError)
        assertEquals(FocusVisualTheme.DEFAULT, state.visualTheme)
    }

    @Test
    fun testSwitchingVisualThemes() {
        val viewModel = FocusViewModel(application)

        // Switch to Forest
        viewModel.setVisualTheme(FocusVisualTheme.FOREST)
        assertEquals(FocusVisualTheme.FOREST, viewModel.uiState.value.visualTheme)

        // Switch to Ocean
        viewModel.setVisualTheme(FocusVisualTheme.OCEAN)
        assertEquals(FocusVisualTheme.OCEAN, viewModel.uiState.value.visualTheme)

        // Switch to Space
        viewModel.setVisualTheme(FocusVisualTheme.SPACE)
        assertEquals(FocusVisualTheme.SPACE, viewModel.uiState.value.visualTheme)

        // Switch back to Default
        viewModel.setVisualTheme(FocusVisualTheme.DEFAULT)
        assertEquals(FocusVisualTheme.DEFAULT, viewModel.uiState.value.visualTheme)
    }

    @Test
    fun testVisualThemePersistenceAcrossRestarts() {
        val viewModel = FocusViewModel(application)
        viewModel.setVisualTheme(FocusVisualTheme.OCEAN)

        // Recreate ViewModel simulating next app launch
        val newViewModel = FocusViewModel(application)
        assertEquals(FocusVisualTheme.OCEAN, newViewModel.uiState.value.visualTheme)
    }

    @Test
    fun testRetryInitializationAndResetCorruptedState() {
        val viewModel = FocusViewModel(application)
        viewModel.retryInitialization()

        assertFalse(viewModel.uiState.value.isError)

        viewModel.resetCorruptedState()
        assertFalse(viewModel.uiState.value.isError)
    }
}
