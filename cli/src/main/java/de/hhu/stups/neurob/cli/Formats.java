package de.hhu.stups.neurob.cli;

import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.formats.JsonFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDumpFormat;

import java.util.concurrent.Callable;

public enum Formats {

    JSON("Json format for training", JsonFormat::new),
    JSONDB("Json for predicate data bases", () -> new JsonDbFormat(JsonDbFormat.DEFAULT_BACKENDS)),
    PDUMP("(Legacy) Predicate Dump data base format", PredicateDumpFormat::new);

    public final String description;
    public final Callable<TrainingDataFormat> getter;

    Formats(String id, Callable<TrainingDataFormat> getter) {
        this.description = id;
        this.getter = getter;
    }

    public static String getFormatInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Supported formats:\n");

        for (Formats f : Formats.values()) {
            info.append(" - ");
            int length = 27 - f.name().length();
            while (length < 0) {
                length += 4;
            }
            info.append(f.name().toLowerCase());
            for (int i = 0; i < length; i++) {
                info.append(' ');
            }
            info.append(f.description);
            info.append('\n');
        }

        return info.toString();
    }

    public static TrainingDataFormat parseFormat(String id) throws Exception {
        return Formats.valueOf(id.toUpperCase()).getter.call();
    }
}
