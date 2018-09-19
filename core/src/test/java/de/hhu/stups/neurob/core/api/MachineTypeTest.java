package de.hhu.stups.neurob.core.api;

import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.statespace.StateSpace;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MachineTypeTest {

    @Test
    void shouldPredictClassicalBFromStateSpace() {
        StateSpace ss = mock(StateSpace.class);
        ClassicalBModel model = mock(ClassicalBModel.class);
        when(ss.getModel()).thenReturn(model);

        MachineType expected = MachineType.CLASSICALB;
        MachineType actual = MachineType.getTypeFromStateSpace(ss);

        assertEquals(expected, actual);
    }

    @Test
    void shouldPredictEventBFromStateSpace() {
        StateSpace ss = mock(StateSpace.class);
        EventBModel model = mock(EventBModel.class);
        when(ss.getModel()).thenReturn(model);

        MachineType expected = MachineType.EVENTB;
        MachineType actual = MachineType.getTypeFromStateSpace(ss);

        assertEquals(expected, actual);
    }

    @Test
    void shouldPredictClassicalBFromStateSpaceWhenUnknown() {
        StateSpace ss = mock(StateSpace.class);
        AbstractModel model = mock(AbstractModel.class);
        when(ss.getModel()).thenReturn(model);

        MachineType expected = MachineType.CLASSICALB;
        MachineType actual = MachineType.getTypeFromStateSpace(ss);

        assertEquals(expected, actual);
    }

    @Test
    void shouldPredictClassicalBFromPath() {
        Path location = Paths.get("path/to/machine.mch");

        MachineType expected = MachineType.CLASSICALB;
        MachineType actual = MachineType.predictTypeFromLocation(location);

        assertEquals(expected, actual);
    }

    @Test
    void shouldPredictEventBFromPath() {
        Path location = Paths.get("path/to/machine.bcm");

        MachineType expected = MachineType.EVENTB;
        MachineType actual = MachineType.predictTypeFromLocation(location);

        assertEquals(expected, actual);
    }

    @Test
    void shouldPredictClassicalBFromPathWhenUnknown() {
        Path location = Paths.get("path/to/machine.txt");

        MachineType expected = MachineType.CLASSICALB;
        MachineType actual = MachineType.predictTypeFromLocation(location);

        assertEquals(expected, actual);
    }
}
