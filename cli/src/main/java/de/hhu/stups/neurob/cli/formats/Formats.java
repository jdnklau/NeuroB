package de.hhu.stups.neurob.cli.formats;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.labelling.BackendClassification;
import de.hhu.stups.neurob.training.db.JsonDbFormat;
import de.hhu.stups.neurob.training.db.PredicateList;
import de.hhu.stups.neurob.training.formats.*;
import de.hhu.stups.neurob.training.migration.legacy.PredicateDumpFormat;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public enum Formats {

    JSON("Json format for training", (f,l,b) -> new JsonFormat()),
    JSONDB("Json for predicate data bases", (f,l,b) -> new JsonDbFormat(b.toArray(new Backend[0]))),
    PDUMP("(Legacy) Predicate Dump data base format", (f,l,b) -> new PredicateDumpFormat()),
    CSV("CSV", (f,l,b) -> new CsvFormat(f, l)),
    CSVC("CSV with comment column", (f,l,b) -> new CsvFormat(f, l, true, true)),
    TFTXT("Tensorflow Text Data directory structure", (f,l,b) -> new TfTextDirectory<BackendClassification>()),
    PLIST("Predicate list data base format", (f,l,b) -> new PredicateList()),
    RAWPRED("List of labelled, raw predicates for training", (f,l,b) -> new LabelledPredicateListFormat()),
    BAST("List of BAsts with preceeding labels", (f,l,b) -> new PrologAstListFormat());

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

    public static TrainingDataFormat parseFormat(String id, int featureSize, int labelSize) {
        return Formats.valueOf(id.toUpperCase()).getter.get(featureSize, labelSize, new ArrayList<>());
    }

    public static TrainingDataFormat parseFormat(String id, int featureSize, int labelSize, List<Backend> backends) {
        return Formats.valueOf(id.toUpperCase()).getter.get(featureSize, labelSize, backends);
    }
}
