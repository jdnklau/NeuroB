package de.hhu.stups.neurob.training.formats;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.ProBBackend;
import de.hhu.stups.neurob.core.api.backends.Z3Backend;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.core.features.predicates.RawPredFeature;
import de.hhu.stups.neurob.core.labelling.BackendClassification;
import de.hhu.stups.neurob.core.labelling.PredicateLabelling;
import de.hhu.stups.neurob.training.data.TrainingData;
import de.hhu.stups.neurob.training.data.TrainingSample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TfTextDirectoryIT {

    @Test
    void shouldCreateDirectoryLayout() throws IOException {
        Path targetDir = Files.createTempDirectory("neurob");
        TfTextDirectory<BackendClassification> format = new TfTextDirectory<>();

        TrainingData<RawPredFeature, BackendClassification> data = new TrainingData<>(
                null,
                Stream.of(
                        createSample("foo1", 1),
                        createSample("foo2", 1),
                        createSample("foo3", 1),
                        createSample("bar1", 0),
                        createSample("bar2", 0),
                        createSample("baz1", 2),
                        createSample("baz2", 2)
                )
        );

        format.writeSamples(data, targetDir);

        Map<String, String> fileContents = new HashMap<>();
        fileContents.put("1.0/pred_0.txt", "foo1");
        fileContents.put("1.0/pred_1.txt", "foo2");
        fileContents.put("1.0/pred_2.txt", "foo3");
        fileContents.put("0.0/pred_3.txt", "bar1");
        fileContents.put("0.0/pred_4.txt", "bar2");
        fileContents.put("2.0/pred_5.txt", "baz1");
        fileContents.put("2.0/pred_6.txt", "baz2");

        List<Executable> tests = new ArrayList<>();
        for(Map.Entry<String, String> entry : fileContents.entrySet()) {
            Path file = targetDir.resolve(entry.getKey());
            String contents = entry.getValue();
            try {
                String actual = Files.readAllLines(file).get(0);
                tests.add(() -> assertEquals(contents, actual,
                        "Contents in " + file + " do not match."));
            } catch (IOException e) {
                tests.add(() -> fail("Unable to access " + file));
            }
        }

        assertAll(tests);

    }

    /**
     * @param pred
     * @param label 1 for ProB, 2 for Z3, 0 for none.
     * @return
     */
    TrainingSample<RawPredFeature, BackendClassification>
    createSample(String pred, int label) {
        BPredicate bpred = BPredicate.of(pred);
        Backend[] backends = new Backend[]{
                new ProBBackend(),
                new Z3Backend()
        };
        Backend target = (label == 0) ? null : backends[label-1];
        BackendClassification classification = new BackendClassification(
                bpred,
                backends,
                target
        );
        RawPredFeature feat = new RawPredFeature(bpred);
        return new TrainingSample<>(feat,classification);
    }

}
