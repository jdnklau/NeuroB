package de.hhu.stups.neurob.core.api.bmethod;

import de.hhu.stups.neurob.core.api.MachineType;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreference;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.prob.animator.command.SetPreferenceCommand;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MachineAccessTest {

    private StateSpace stateSpace;

    @BeforeEach
    void setupStateSpace() {
        stateSpace = mock(StateSpace.class);
    }

    @Test
    void shouldApplyPreferences() throws MachineAccessException {
        Path source = Paths.get("non/existent.mch");
        MachineAccess ma = spy(new MachineAccess(source,
                MachineType.CLASSICALB, false));
        doReturn(stateSpace).when(ma).loadStateSpace(source);

        ma.load();

        ma.setPreferences(BPreference.set("PREF1", "val1"), BPreference.set("PREF2", "val2"));

        verify(stateSpace, times(2))
                // NOTE: There is no equality on SetPreferenceCommands, so we can only use any()
                .execute(any(SetPreferenceCommand.class));
    }

    @Test
    void shouldNotApplyPreferencesTwice() throws MachineAccessException {
        Path source = Paths.get("non/existent.mch");
        MachineAccess ma = spy(new MachineAccess(source,
                MachineType.CLASSICALB, false));
        doReturn(stateSpace).when(ma).loadStateSpace(source);

        ma.load();

        ma.setPreferences(BPreference.set("PREF1", "val1"));
        ma.setPreferences(BPreference.set("PREF1", "val1"), BPreference.set("PREF2", "val2"));

        verify(stateSpace, times(2))
                // NOTE: There is no equality on SetPreferenceCommands, so we can only use any()
                .execute(any(SetPreferenceCommand.class));
    }

    @Test
    void shouldApplyPreferencesTwiceIfValueIsUpdated() throws MachineAccessException {
        Path source = Paths.get("non/existent.mch");
        MachineAccess ma = spy(new MachineAccess(source,
                MachineType.CLASSICALB, false));
        doReturn(stateSpace).when(ma).loadStateSpace(source);

        ma.load();

        ma.setPreferences(BPreference.set("PREF1", "val1"));
        ma.setPreferences(BPreference.set("PREF1", "val2"));

        verify(stateSpace, times(2))
                // NOTE: There is no equality on SetPreferenceCommands, so we can only use any()
                .execute(any(SetPreferenceCommand.class));
    }

    @Test
    void shouldApplyPreferencesOnlyWhenStateSpaceIsLoaded() throws MachineAccessException {
        Path source = Paths.get("non/existent.mch");
        MachineAccess ma = spy(new MachineAccess(source,
                MachineType.CLASSICALB, false));
        doReturn(stateSpace).when(ma).loadStateSpace(source);

        ma.setPreferences(BPreference.set("PREF1", "val1"));
        ma.setPreferences(BPreference.set("PREF1", "val2"));

        verify(stateSpace, never())
                // NOTE: There is no equality on SetPreferenceCommands, so we can only use any()
                .execute(any(SetPreferenceCommand.class));
    }

    @Test
    void shouldApplyPreferencesWhenLoadingStateSpaceAfterwards() throws MachineAccessException {
        Path source = Paths.get("non/existent.mch");
        MachineAccess ma = spy(new MachineAccess(source,
                MachineType.CLASSICALB, false));
        doReturn(stateSpace).when(ma).loadStateSpace(source);

        ma.setPreferences(BPreference.set("PREF1", "val1"));

        ma.load();

        verify(stateSpace, times(1))
                // NOTE: There is no equality on SetPreferenceCommands, so we can only use any()
                .execute(any(SetPreferenceCommand.class));
    }

    @Test
    void shouldBeEqualWhenSameSource() throws MachineAccessException {
        MachineAccess m1 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);
        MachineAccess m2 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);

        assertEquals(m1, m2);
    }

    @Test
    void shouldBeUnequalWhenSameSourceAndMismatchingPreferences() throws MachineAccessException {
        MachineAccess m1 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);
        m1.setPreferences(BPreference.set("PREF1", "val1"));

        MachineAccess m2 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);
        m2.setPreferences(BPreference.set("PREF2", "val2"));

        assertNotEquals(m1, m2);
    }

    @Test
    void shouldBeEqualWhenSameSourceAndMatchingPreferences() throws MachineAccessException {
        MachineAccess m1 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);
        m1.setPreferences(BPreference.set("PREF1", "val1"));

        MachineAccess m2 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);
        m2.setPreferences(BPreference.set("PREF1", "val1"));

        assertEquals(m1, m2);
    }

    @Test
    void shouldHaveEqualHashCodeWhenSourceAndPreferencesMatch() throws MachineAccessException {
        MachineAccess m1 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);
        m1.setPreferences(BPreference.set("PREF1", "val1"));

        MachineAccess m2 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);
        m2.setPreferences(BPreference.set("PREF1", "val1"));

        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void shouldHaveUnequalHashCodeWhenSourceAndPreferencesMismatch() throws MachineAccessException {
        MachineAccess m1 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);
        m1.setPreferences(BPreference.set("PREF1", "val1"));

        MachineAccess m2 = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);
        m2.setPreferences(BPreference.set("PREF2", "val2"));

        assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void shouldCallCloseHandlers() throws MachineAccessException {
        MachineAccess access = new MachineAccess(Paths.get("non/existent.mch"), MachineType.CLASSICALB, false);

        AtomicInteger counter = new AtomicInteger(0);
        access.onClose(s -> counter.addAndGet(1));
        access.onClose(s -> counter.addAndGet(2));

        access.close();

        assertEquals(3, counter.get());
    }

}
