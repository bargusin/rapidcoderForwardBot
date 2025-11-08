package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.rapidcoder.forward.bot.handler.NavigationManager;

import java.io.File;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

public class NavigationManagerTest {

    private static final String TEST_DB = "test_chat.db";
    private NavigationManager navigationManager;

    @BeforeAll
    static void cleanup() {
        new File(TEST_DB).delete();
    }

    @BeforeEach
    void setUp() {
        navigationManager = new NavigationManager(TEST_DB);
    }

    @Test
    void testSetState() {
        assertThrows(IllegalArgumentException.class, () -> {
            navigationManager.setState(1L, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            navigationManager.setState(0L, null, "context");
        });

        navigationManager.setState(1L, "state");
        String state = navigationManager.getState(1L)
                .orElse("not found");
        assertThat(state).isEqualTo("state");

        state = navigationManager.getState(2L)
                .orElse("not found");
        assertThat(state).isEqualTo("not found");
    }

    @Test
    void testGetContext() {
        navigationManager.setState(1L, "state", "context");
        String context = navigationManager.getContext(1L)
                .orElse("not found");
        assertThat(context).isEqualTo("context");

        context = navigationManager.getContext(2L)
                .orElse("not found");
        assertThat(context).isEqualTo("not found");
    }

    @Test
    void testClearState() {
        navigationManager.setState(1L, "state", "context");
        String state = navigationManager.getState(1L)
                .orElse("not found");
        assertThat(state).isEqualTo("state");

        navigationManager.clearState(2L);
        state = navigationManager.getState(1L)
                .orElse("not found");
        assertThat(state).isEqualTo("state");

        navigationManager.clearState(1L);
        state = navigationManager.getState(1L)
                .orElse("not found");
        assertThat(state).isEqualTo("not found");
    }

    @Test
    void testHasState() {
        navigationManager.setState(1L, "state", "context");
        navigationManager.hasState(1L);
        assertThat(navigationManager.hasState(1L)).isTrue();

    }
}
