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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

}
