package de.hhu.stups.neurob.cli;

import de.hhu.stups.neurob.cli.formats.Formats;
import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.formats.JsonFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDumpFormat;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FormatsTest {

    @Test
    void shouldGetJsonFormat() throws Exception {
        TrainingDataFormat format = Formats.parseFormat("json", 0,0);

        assertTrue(format instanceof JsonFormat);
    }

    @Test
    void shouldGetPdumpFormat() throws Exception {
        TrainingDataFormat format = Formats.parseFormat("pdump", 0,0);

        assertTrue(format instanceof PredicateDumpFormat);
    }

    @Test
    void shouldGetJsonDbWithCorrespondingBackends() throws Exception {
        Backend[] backends = {new ProBBackend(), new Z3Backend()};

        TrainingDataFormat format = Formats.parseFormat(
                "jsondb", 0,0,
                Arrays.stream(backends).collect(Collectors.toList()));

        JsonDbFormat jsonDbFormat = (JsonDbFormat) format;

        assertArrayEquals(backends, jsonDbFormat.getBackendsUsed());

    }

}
