package de.hhu.stups.neurob.core.labelling;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.MachineAccess;
import de.hhu.stups.neurob.core.exceptions.MachineAccessException;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.training.migration.labelling.LabelTranslation;
import de.prob.animator.command.CbcSolveCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankingBasedataTest {
    private MachineAccess machineAccess;
    private Backend prob;
    private Backend kodkod;
    private Backend z3;

    @BeforeEach
    public void mockBackEnds() {
        prob = mock(Backend.class);
        doReturn(CbcSolveCommand.Solvers.PROB).when(prob).toCbcEnum();
        kodkod = mock(Backend.class);
        doReturn(CbcSolveCommand.Solvers.KODKOD).when(kodkod).toCbcEnum();
        z3 = mock(Backend.class);
        doReturn(CbcSolveCommand.Solvers.Z3).when(z3).toCbcEnum();
    }

    @Test
    void shouldTranslateInput() {
        Backend[] backends = {prob, z3};
        RankingBasedata.Translator translator = new RankingBasedata.Translator(backends);

        Map<Backend, TimedAnswer> results = new HashMap<>();
        results.put(prob, new TimedAnswer(Answer.VALID, 300L));
        results.put(z3, new TimedAnswer(Answer.TIMEOUT, 500L));
        PredDbEntry data = new PredDbEntry(null, null, results);

        Double[] expected = {300., 1., 500., 4.};
        Double[] actual = translator.translate(data).labellingArray;

        assertArrayEquals(expected, actual);
    }

}
