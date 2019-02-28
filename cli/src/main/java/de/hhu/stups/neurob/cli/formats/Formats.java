package de.hhu.stups.neurob.cli.formats;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.formats.JsonFormat;
import de.hhu.stups.neurob.training.formats.TrainingDataFormat;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDumpFormat;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public enum Formats {

    JSON("Json format for training", (b) -> new JsonFormat()),
    JSONDB("Json for predicate data bases", (b) -> new JsonDbFormat(b.toArray(new Backend[0]))),
    PDUMP("(Legacy) Predicate Dump data base format", (b) -> new PredicateDumpFormat());

    public final String description;
    public final FormatParser getter;

    Formats(String id, FormatParser getter) {
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

    public static TrainingDataFormat parseFormat(String id) {
        return Formats.valueOf(id.toUpperCase()).getter.get(new ArrayList<>());
    }

    public static TrainingDataFormat parseFormat(String id, List<Backend> backends) {
        return Formats.valueOf(id.toUpperCase()).getter.get(backends);
    }
}
