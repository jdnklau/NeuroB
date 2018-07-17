package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.bmethod.BElement;
import de.hhu.stups.neurob.core.labelling.Labelling;

import java.nio.file.Path;

public class DbSample<B extends BElement> {
    private final B bElement;
    // TODO: DbLabelling?
    private final Labelling labelling;
    private final Path sourceMachine;

    public DbSample(B bElement, Labelling labelling) {
        this(bElement, labelling, null);
    }

    public DbSample(B bElement, Labelling labelling, Path sourceMachine) {
        this.bElement = bElement;
        this.labelling = labelling;
        this.sourceMachine = sourceMachine;
    }

    public B getBElement() {
        return bElement;
    }

    public Labelling getLabelling() {
        return labelling;
    }

    public Path getSourceMachine() {
        return sourceMachine;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DbSample) {
            DbSample other = (DbSample) o;
            boolean areBElementsEqual = bElement != null
                    ? bElement.equals(other.bElement)
                    : other.getBElement() == null;
            boolean areLabelsEqual = labelling != null
                    ? labelling.equals(other.labelling)
                    : other.labelling == null;
            boolean areSourcesEqual = sourceMachine != null
                    ? sourceMachine.equals(other.getSourceMachine())
                    : other.getSourceMachine() == null;

            return areBElementsEqual && areLabelsEqual && areSourcesEqual;
        }

        return false;
    }

    @Override
    public String toString() {
        return "[bElement=" + bElement + ", "
               + "labelling=" + labelling.getLabellingString() + ", "
               + "sourceFile=" + sourceMachine + "]";
    }
}
