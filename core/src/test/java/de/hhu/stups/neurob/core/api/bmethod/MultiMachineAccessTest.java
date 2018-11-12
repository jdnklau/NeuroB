package de.hhu.stups.neurob.core.api.bmethod;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.backends.preferences.BPreferences;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.prob.animator.command.AbstractCommand;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MultiMachineAccessTest {

    @Test
    void shouldLoadAllAccesses() throws MachineAccessException {
        Backend b1 = new ProBBackend();
        Backend b2 = new Z3Backend();

        MachineAccess a1 = mock(MachineAccess.class);
        MachineAccess a2 = mock(MachineAccess.class);

        Map<Backend, MachineAccess> accessMap = new HashMap<>();
        accessMap.put(b1, a1);
        accessMap.put(b2, a2);

        MultiMachineAccess access = new MultiMachineAccess(null, null, accessMap, false);

        access.load();

        verify(a1).load();
        verify(a2).load();
    }

    @Test
    void shouldCloseAllAccesses() throws MachineAccessException {
        Backend b1 = new ProBBackend();
        Backend b2 = new Z3Backend();

        MachineAccess a1 = mock(MachineAccess.class);
        MachineAccess a2 = mock(MachineAccess.class);

        Map<Backend, MachineAccess> accessMap = new HashMap<>();
        accessMap.put(b1, a1);
        accessMap.put(b2, a2);

        MultiMachineAccess access = new MultiMachineAccess(null, null, accessMap, false);

        access.close();

        verify(a1).close();
        verify(a2).close();
    }

    @Test
    void shouldMapCommandExecution() throws MachineAccessException {
        Backend b1 = new ProBBackend();
        Backend b2 = new Z3Backend();

        MachineAccess a1 = mock(MachineAccess.class);
        MachineAccess a2 = mock(MachineAccess.class);

        Map<Backend, MachineAccess> accessMap = new HashMap<>();
        accessMap.put(b1, a1);
        accessMap.put(b2, a2);

        MultiMachineAccess access = new MultiMachineAccess(null, null, accessMap, false);

        AbstractCommand command = mock(AbstractCommand.class);
        access.execute(command);

        verify(a1).execute(command);
        verify(a2).execute(command);
    }

    @Test
    void shouldMapInterrupt() throws MachineAccessException {
        Backend b1 = new ProBBackend();
        Backend b2 = new Z3Backend();

        MachineAccess a1 = mock(MachineAccess.class);
        MachineAccess a2 = mock(MachineAccess.class);

        Map<Backend, MachineAccess> accessMap = new HashMap<>();
        accessMap.put(b1, a1);
        accessMap.put(b2, a2);

        MultiMachineAccess access = new MultiMachineAccess(null, null, accessMap, false);
        access.sendInterrupt();

        verify(a1).sendInterrupt();
        verify(a2).sendInterrupt();
    }

    @Test
    void shouldSetPreferencesForAllBackends() throws MachineAccessException {
        Backend b1 = new ProBBackend();
        Backend b2 = new Z3Backend();

        MachineAccess a1 = mock(MachineAccess.class);
        MachineAccess a2 = mock(MachineAccess.class);

        Map<Backend, MachineAccess> accessMap = new HashMap<>();
        accessMap.put(b1, a1);
        accessMap.put(b2, a2);

        MultiMachineAccess access = new MultiMachineAccess(null, null, accessMap, false);

        BPreferences preferences = BPreferences.set("PREF1", "val1").assemble();
        access.setPreferences(preferences);

        verify(a1).setPreferences(preferences);
        verify(a2).setPreferences(preferences);
    }

    @Test
    void shouldBeEqual() throws MachineAccessException {
        Backend b1 = new ProBBackend();
        Backend b2 = new Z3Backend();

        MachineAccess a1 = mock(MachineAccess.class);
        MachineAccess a2 = mock(MachineAccess.class);

        Map<Backend, MachineAccess> accessMap = new HashMap<>();
        accessMap.put(b1, a1);
        accessMap.put(b2, a2);

        MultiMachineAccess access1 = new MultiMachineAccess(null, null, accessMap, false);
        MultiMachineAccess access2 = new MultiMachineAccess(null, null, accessMap, false);

        assertEquals(access1, access2);
    }

    @Test
    void shouldBeUnequal() throws MachineAccessException {
        Backend b1 = new ProBBackend();
        Backend b2 = new Z3Backend();

        MachineAccess a1 = mock(MachineAccess.class);
        MachineAccess a2 = mock(MachineAccess.class);

        Map<Backend, MachineAccess> accessMap1 = new HashMap<>();
        accessMap1.put(b1, a1);
        accessMap1.put(b2, a2);

        Map<Backend, MachineAccess> accessMap2 = new HashMap<>();
        accessMap2.put(b1, a2);

        MultiMachineAccess access1 = new MultiMachineAccess(null, null, accessMap1, false);
        MultiMachineAccess access2 = new MultiMachineAccess(null, null, accessMap2, false);

        assertNotEquals(access1, access2);
    }

}
