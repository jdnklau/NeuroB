package de.hhu.stups.neurob.training.migration.labelling;

import de.hhu.stups.neurob.core.labelling.Labelling;

@FunctionalInterface
public interface LabelTranslation<From extends Labelling, To extends Labelling> {

    /**
     * Translates a given labelling into another format.
     *
     * @param origLabels Labels to be translated, usually from a data base.
     *
     * @return
     */
    To translate(From origLabels);

}
