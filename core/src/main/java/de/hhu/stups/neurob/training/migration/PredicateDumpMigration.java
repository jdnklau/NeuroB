package de.hhu.stups.neurob.training.migration;

import de.hhu.stups.neurob.training.migration.legacy.PredicateDumpFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredicateDumpMigration extends PredicateDbMigration {

    private static final Logger log =
            LoggerFactory.getLogger(PredicateDumpMigration.class);

    public PredicateDumpMigration() {
        super(new PredicateDumpFormat());
    }
}
