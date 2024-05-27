package de.hhu.stups.neurob.training.analysis;

import de.hhu.stups.neurob.core.api.backends.Backend;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;
import de.hhu.stups.neurob.core.api.bmethod.BMachine;
import de.hhu.stups.neurob.core.api.bmethod.BPredicate;
import de.hhu.stups.neurob.training.data.TrainingSample;
import de.hhu.stups.neurob.training.db.PredDbEntry;
import de.hhu.stups.neurob.core.api.backends.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Summary of the analysis of a predicate database.
 * <p>
 * Contains the following metrics (Answers are those used in {@link Answer}):
 * <ul>
 * <li>Total number of predicates</li>
 * <li>Number of VALID predicates</li>
 * <li>Number of INVALID predicates</li>
 * <li>Number of UNKNOWN predicates</li>
 * <li>Number of TIMEOUT predicates</li>
 * <li>Number of ERROR predicates</li>
 * <li>Number of SOLVABLE predicates
 * (Note that this is greater or equal than the sum of VALID and INVALID)</li>
 * <li>
 * Runtime subsets per backend.
 * The subsets are split into one set for each Answer,
 * and the combined sets:
 * "Answered" (SOLVABLE + UNKNOWN), and
 * "Timed" (Answered + ERROR).
 * A set containing all data (Timed + TIMEOUT) appears to be a non-sensible choice,
 * as the timeouts all appear roughly around the same time and would only bias
 * the subset to said timeout.
 * <ul>
 * <li>Lowest runtime</li>
 * <li>1st Quartile of runtime</li>
 * <li>Median runtime</li>
 * <li>3rd Quartile of runtime</li>
 * <li>Longest runtime</li>
 * <li>Average runtime</li>
 * </ul>
 * </li>
 * <li>Number of predicates answered to fastest per backend.
 * Note that this accounts only predicates which did not run into an Error or Timeout.</li>
 * <li>
 * Answers per backend and combination of any backends.
 * Note that the combination of backends, like ProB+Z3 acts as conjunction constraint:
 * Only predicates are accounted for for which both backends yielded the same answer.
 * <ul>
 * <li>Total number of predicates</li>
 * <li>Number of VALID predicates</li>
 * <li>Number of INVALID predicates</li>
 * <li>Number of UNKNOWN predicates</li>
 * <li>Number of TIMEOUT predicates</li>
 * <li>Number of ERROR predicates</li>
 * <li>Number of SOLVABLE predicates</li>
 * </ul>
 * </li>
 * </ul>
 */
public class PredDbAnalysis
        implements AnalysisData<TrainingSample<BPredicate, PredDbEntry>, PredDbAnalysis> {

    private Long predCount;
    private ClassificationAnalysis<Answer> answers;
    private Set<PredDbEntry> contradictions;
    private Map<Answer, ClassificationAnalysis<Backend>> backendAnswers;
    private Set<BMachine> bMachines;

    /** All the different Backends seen so far. */
    private Set<Backend> backendsSeen;

    private Map<Answer, Map<Backend, RegressionAnalysis<Long>>> backendRegressions;
    private Map<Backend, RegressionAnalysis<Long>> answeredRegressions;
    private Map<Backend, RegressionAnalysis<Long>> timedRegressions;

    private ClassificationAnalysis<Backend> fastest;

    private static final Logger log = LoggerFactory.getLogger(PredDbAnalysis.class);

    public PredDbAnalysis() {
        predCount = 0L;
        answers = new ClassificationAnalysis<>();
        contradictions = new HashSet<>();
        backendAnswers = new HashMap<>();

        backendRegressions = new HashMap<>();
        answeredRegressions = new HashMap<>();
        timedRegressions = new HashMap<>();

        fastest = new ClassificationAnalysis<>();

        backendsSeen = new HashSet<>();

        bMachines = new HashSet<>();
    }

    /**
     * Summarises the analysis of data seen so far in a text.
     *
     * @return
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();

        // General predicate information
        summary.append("Predicates seen in total: ").append(predCount);
        summary.append('\n');
        summary.append("  Collected from ").append(bMachines.size()).append(" B Machines");
        summary.append('\n');
        // Per Answer
        Answer[] answerOrder = {Answer.VALID, Answer.INVALID, Answer.SOLVABLE,
                Answer.UNKNOWN, Answer.TIMEOUT, Answer.ERROR};
        for (Answer a : answerOrder) {
            Long amount = answers.getCount(a);
            Double fraction = amount / (double) predCount;
            summary.append("  ").append(amount).append(" are ").append(a)
                    .append(" (").append(fraction * 100).append("%)");
            summary.append('\n');
        }

        if (contradictions.size() > 0) {
            summary.append("NOTE: There were ").append(contradictions.size())
                    .append(" contradictions!")
                    .append("\n");
            contradictions.stream().map(this::getContradictionSummary)
                    .forEach(summary::append);
        }

        // Backend-specific data
        for (Backend b : backendsSeen) {
            summary.append(getBackendSummary(b, answerOrder));
            summary.append('\n');
        }

        return summary.toString();
    }

    public String getBackendSummary(Backend b, Answer[] answerOrder) {
        if (!backendsSeen.contains(b)) {
            return "Backend " + b.toString() + " was not seen in the data.\n";
        }

        StringBuilder summary = new StringBuilder();

        summary.append("\nBackend: ").append(b);
        summary.append('\n');

        // Answers
        for (Answer a : answerOrder) {
            ClassificationAnalysis<Backend> classification =
                    backendAnswers.getOrDefault(a, new ClassificationAnalysis<>());

            Long amount = classification
                    .getCount(b);
            Double fraction = amount / (double) predCount;
            summary.append("  ").append(amount).append(" are ").append(a)
                    .append(" (").append(fraction * 100).append("%)");
            summary.append('\n');

            // Intersatisfiability
            List<Set<Backend>> multilabels = classification.getSeenMultilabels()
                    .stream()
                    .filter(s -> s.contains(b))
                    .sorted((x, y) -> y.size() - x.size())
                    .collect(Collectors.toList());
            for (Set<Backend> others : multilabels) {
                if (others.size() <= 1) {
                    continue; // Should be this very backend
                }

                String combination = others.stream()
                        .filter(x -> !x.equals(b)) // don't print this backend
                        .map(Backend::getDescriptionString)
                        .collect(Collectors.joining(" & "));

                Long othersCount = classification.getCount(others);
                summary.append("    ")
                        .append(othersCount)
                        .append(" (").append(100 * othersCount / (double) amount).append("%)")
                        .append(" where also ").append(a)
                        .append(" for ").append(combination);
                summary.append('\n');
            }

        }

        // Fastest
        Long fst = fastest.getCount(b);
        if (fst > 0) {
            int answered = answeredRegressions.getOrDefault(b, new RegressionAnalysis<>())
                    .getSampleCount();
            summary.append("  Fastest for ")
                    .append(fst).append(" of ").append(answered)
                    .append(" answered predicates")
                    .append(" (").append(fst / (double) answered * 100).append("%)");
        } else {
            summary.append("  Never the fastest");
        }
        summary.append('\n');

        // Runtime stats
        summary.append("  Runtime metrics for all answered (VALID, INVALID, UNKNOWN) predicates:\n");
        RegressionAnalysis<Long> regAna = answeredRegressions.getOrDefault(b, new RegressionAnalysis<>());
        addRegressionAnalysisSummary(regAna, summary);
        summary.append('\n');

        summary.append("  Runtime metrics for all timed (VALID, INVALID, UNKNOWN, ERROR) predicates:\n");
        regAna = timedRegressions.getOrDefault(b, new RegressionAnalysis<>());
        addRegressionAnalysisSummary(regAna, summary);
        summary.append('\n');

        summary.append("  Runtime metrics for all answers (includes TIMEOUT as well):\n");
        regAna.mergeWith(backendRegressions.get(Answer.TIMEOUT).get(b));
        addRegressionAnalysisSummary(regAna, summary);


        return summary.toString();
    }

    private void addRegressionAnalysisSummary(RegressionAnalysis regAna, StringBuilder summary) {
        // We divide by 1 000 000 to convert from nanoseconds to milliseconds.
        summary.append("  - Minimum: ");
        summary.append(regAna.getMinimum().doubleValue()/1_000_000).append(" ms");
        summary.append("\n  - 1. Quartile: ");
        summary.append(regAna.getFirstQuartile()/1_000_000).append(" ms");
        summary.append("\n  - Median: ");
        summary.append(regAna.getMedian()/1_000_000).append(" ms");
        summary.append("\n  - 3. Quartile: ");
        summary.append(regAna.getThirdQuartile()/1_000_000).append(" ms");
        summary.append("\n  - Maximum: ");
        summary.append(regAna.getMaximum().doubleValue()/1_000_000).append(" ms");
    }

    public String getContradictionSummary(PredDbEntry dbEntry) {
        StringBuilder summary = new StringBuilder();
        BPredicate predicate = dbEntry.getPredicate();

        List<Backend> valids = new ArrayList<>();
        List<Backend> invalids = new ArrayList<>();

        for (Map.Entry<Backend, TimedAnswer> pair : dbEntry.getResults().entrySet()) {
            Backend b = pair.getKey();
            TimedAnswer answer = pair.getValue();

            switch (answer.getAnswer()) {
                case VALID:
                    valids.add(b);
                    break;
                case INVALID:
                    invalids.add(b);
                    break;
            }
        }

        summary.append("  ").append(predicate);
        summary.append('\n');
        summary.append("    over ").append(dbEntry.getSource());
        summary.append('\n');
        summary.append("    VALID for ");
        valids.stream()
                .flatMap(b -> Stream.of(", ", b.getDescriptionString()))
                .skip(1)
                .forEach(summary::append);
        summary.append('\n');
        summary.append("    INVALID for ");
        invalids.stream()
                .flatMap(b -> Stream.of(", ", b.getDescriptionString()))
                .skip(1)
                .forEach(summary::append);
        summary.append('\n');

        return summary.toString();
    }

    @Override
    public String toString() {
        return getSummary();
    }

    public Set<Backend> getBackendsSeen() {
        return backendsSeen;
    }

    /**
     * Adds the given sample's metrics to this analysis,
     * by expanding the so far acquired statistics.
     * <p>
     * returns a reference to itself for method chaining.
     *
     * @param sample
     *
     * @return
     */
    @Override
    public PredDbAnalysis add(TrainingSample<BPredicate, PredDbEntry> sample) {
        return add(sample.getData(), sample.getLabelling());
    }

    /**
     * Adds the given data's metrics to this analysis,
     * by expanding the so far acquired statistics.
     * <p>
     * returns a reference to itself for method chaining.
     *
     * @param data Data over which the metrics were taken
     * @param metrics Metrics measured over the data
     *
     * @return
     */
    public PredDbAnalysis add(BPredicate data, PredDbEntry metrics) {
        // Count BMachine
        bMachines.add(metrics.getSource());
        // Classification: Predicate level
        log.info("Analysing metrics of {}", data);
        predCount++;
        countBestAnswer(data, metrics);
        // Classification: Backend Level
        for (Map.Entry<Answer, Set<Backend>> entry : clusterBackendsByAnswer(metrics).entrySet()) {
            Answer answer = entry.getKey();
            Set<Backend> cluster = entry.getValue();
            log.debug("Add answer {} for cluster {}", answer, cluster);
            Backend[] clusterArray = cluster.toArray(new Backend[0]);
            addAnswer(answer, clusterArray);
        }

        // Add seen backends
        backendsSeen.addAll(metrics.getResults().keySet());

        // fastest
        log.debug("Analyse fastest answer for {}", data);
        countFastest(metrics.getResults());

        // Regression
        log.debug("Analyse runtime metrics for {}", data);
        addRuntimes(metrics.getResults());

        return this;
    }

    /**
     * Clusters the answer map in the predicate db entry into sets belonging to
     * their respective answers.
     *
     * @param metrics
     *
     * @return
     */
    static Map<Answer, Set<Backend>> clusterBackendsByAnswer(PredDbEntry metrics) {
        Map<Backend, TimedAnswer> answerMap = metrics.getResults();
        Map<Answer, Set<Backend>> result = new HashMap<>();

        for (Map.Entry<Backend, TimedAnswer> entry : answerMap.entrySet()) {
            Backend backend = entry.getKey();
            TimedAnswer tAnswer = entry.getValue();
            if (tAnswer == null) {
                continue;
            }
            Answer answer = tAnswer.getAnswer();

            Set<Backend> cluster = result.getOrDefault(answer, new HashSet<>());
            cluster.add(backend);
            result.put(answer, cluster);

            // Valid and Invalid are also Solvable
            if (answer.equals(Answer.VALID) || answer.equals(Answer.INVALID)) {
                Set<Backend> solvableCluster = result.getOrDefault(Answer.SOLVABLE, new HashSet<>());
                solvableCluster.add(backend);
                result.put(Answer.SOLVABLE, solvableCluster);
            }
        }

        return result;
    }

    void countBestAnswer(BPredicate pred, PredDbEntry dbEntry) {
        Answer best = Answer.ERROR;
        Collection<TimedAnswer> values = dbEntry.getResults().values();

        for (TimedAnswer timedAnswer : values) {
            if (timedAnswer == null) {
                continue;
            }
            Answer answer = timedAnswer.getAnswer();

            // Everything is better than an ERROR
            if (best.equals(Answer.ERROR)) {
                best = answer;
            }
            // Everything non-Error is better than TimeOut
            else if (best.equals(Answer.TIMEOUT)
                && !answer.equals(Answer.ERROR)
                && !answer.equals(Answer.TIMEOUT)) {
                best = answer;
            }
            // Solvable predicates are better than UNKNOWN
            else if (best.equals(Answer.UNKNOWN) && (answer.equals(Answer.VALID)
                                                || answer.equals(Answer.INVALID)
                                                || answer.equals(Answer.SOLVABLE))) {
                best = answer;
            }
            // VALID/INVALID is better than SOLVABLE
            else if (best.equals(Answer.SOLVABLE) && (answer.equals(Answer.VALID)
                                                 || answer.equals(Answer.INVALID))) {
                best = answer;
            }
            // Find contradictions
            if (best.equals(Answer.VALID) && answer.equals(Answer.INVALID)
                || best.equals(Answer.INVALID) && answer.equals(Answer.VALID)) {
                log.warn("Contradiction found: {} is classified as VALID and INVALID "
                         + "by different backends over file {}", pred, dbEntry.getSource());
                contradictions.add(dbEntry);
                return;
            }
        }

        answers.add(best);
        // VALID and INVALID are still SOLVABLE
        if (best.equals(Answer.VALID) || best.equals(Answer.INVALID)) {
            answers.add(Answer.SOLVABLE);
        }

    }

    /**
     * Adds the answer to analysis of the given backends.
     *
     * @param answer
     * @param backends
     */
    void addAnswer(Answer answer, Backend... backends) {
        if (!backendAnswers.containsKey(answer)) {
            backendAnswers.put(answer, new ClassificationAnalysis<>());
        }
        backendAnswers.get(answer).add(backends);
    }

    /**
     * Returns the number of predicates for which the backend returned the specified answer.
     * If multiple backends are listed, return the number of instances for which all
     * backends return this answer.
     *
     * @param answer
     * @param backends
     *
     * @return
     */
    public Long getAnswerCount(Answer answer, Backend... backends) {
        if (backendAnswers.containsKey(answer)) {
            return backendAnswers.get(answer).getCount(backends);
        } else {
            return 0L;
        }
    }

    /**
     * Returns number of seen predicates.
     *
     * @return
     */
    public Long getPredCount() {
        return predCount;
    }

    /**
     * Returns number of predicates belonging to the given answer.
     * <p>
     * Note that the best answers are counted only.
     * Assuming for backends which answer UNKNOWN and one answering VALID,
     * then the predicate is counted as VALID.
     *
     * @param answer
     *
     * @return
     */
    public Long getPredCount(Answer answer) {
        return answers.getCount(answer);
    }

    /**
     * Gets the database entries which contain a contradiction.
     * <p>
     * A contradiction is caused by a predicate being classified as VALID by one,
     * and INVALID by another backend.
     *
     * @return
     */
    public Set<PredDbEntry> getContradictions() {
        return contradictions;
    }

    /**
     * Gets number of contradictions found in the predicates.
     * <p>
     * A contradiction is caused by a predicate being classified as VALID by one,
     * and INVALID by another backend.
     *
     * @return
     */
    public Long getContradictionCount() {
        return (long) contradictions.size();
    }

    void addRuntimes(Map<Backend, TimedAnswer> results) {
        for (Map.Entry<Backend, TimedAnswer> entry : results.entrySet()) {
            Backend backend = entry.getKey();
            TimedAnswer answer = entry.getValue();
            if (answer == null) {
                continue;
            }
            addRuntime(backend, answer.getAnswer(), answer.getNanoSeconds());
        }
    }

    void addRuntime(Backend backend, Answer answer, Long time) {
        RegressionAnalysis<Long> analysis = getRegressionAnalysis(answer, backend);
        analysis.add(time);

        Map<Backend, RegressionAnalysis<Long>> backendMap =
                backendRegressions.getOrDefault(answer, new HashMap<>());
        backendMap.put(backend, analysis);
        backendRegressions.put(answer, backendMap);

        // VALID and INVALID are both Solvable
        if (answer.equals(Answer.VALID) || answer.equals(Answer.INVALID)) {
            addRuntime(backend, Answer.SOLVABLE, time);
        } else if (answer.equals(Answer.SOLVABLE)
                   || answer.equals(Answer.UNKNOWN)
                   || answer.equals(Answer.ERROR)) {

            // Everything that is not a time-out is Timed
            RegressionAnalysis<Long> timed = getTimedRegressionAnalysis(backend);
            timed.add(time);
            timedRegressions.put(backend, timed);

            // SOLVABLE and UNKNOWN are both Answered
            if (!answer.equals(Answer.ERROR)) {
                RegressionAnalysis<Long> answered = getAnswererdRegressionAnalysis(backend);
                answered.add(time);
                answeredRegressions.put(backend, answered);
            }
        }
    }

    /**
     * Returns the regression analysis of the given Answer/Backend combination.
     * <p>
     * Might be empty of no such analysis data exist.
     *
     * @param answer
     * @param backend
     *
     * @return
     */
    RegressionAnalysis<Long> getRegressionAnalysis(Answer answer, Backend backend) {
        if (backendRegressions.containsKey(answer)) {
            Map<Backend, RegressionAnalysis<Long>> brMap = backendRegressions.get(answer);
            if (brMap.containsKey(backend)) {
                return brMap.get(backend);
            }
        }
        return new RegressionAnalysis<>();
    }

    RegressionAnalysis<Long> getAnswererdRegressionAnalysis(Backend backend) {
        return answeredRegressions.getOrDefault(backend, new RegressionAnalysis<>());
    }

    RegressionAnalysis<Long> getTimedRegressionAnalysis(Backend backend) {
        return timedRegressions.getOrDefault(backend, new RegressionAnalysis<>());
    }

    /**
     * Return the average runtime needed by the given backend for predicates for
     * which it returned the specified answer.
     *
     * @param answer
     * @param backend
     *
     * @return
     */
    public Double getAverageRuntime(Answer answer, Backend backend) {
        RegressionAnalysis<Long> analysis = getRegressionAnalysis(answer, backend);
        return analysis.getAverage();
    }

    /**
     * Return the average runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Double getAverageAnsweredRuntime(Backend backend) {
        return getAnswererdRegressionAnalysis(backend).getAverage();
    }

    /**
     * Return the average runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Double getAverageTimedRuntime(Backend backend) {
        return getTimedRegressionAnalysis(backend).getAverage();
    }

    /**
     * Return the minimum runtime needed by the given backend for predicates for
     * which it returned the specified answer.
     *
     * @param answer
     * @param backend
     *
     * @return
     */
    public Long getMinimumRuntime(Answer answer, Backend backend) {
        RegressionAnalysis<Long> analysis = getRegressionAnalysis(answer, backend);
        return analysis.getMinimum();
    }

    /**
     * Return the minimum runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Long getMinimumAnsweredRuntime(Backend backend) {
        return getAnswererdRegressionAnalysis(backend).getMinimum();
    }

    /**
     * Return the minimum runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Long getMinimumTimedRuntime(Backend backend) {
        return getTimedRegressionAnalysis(backend).getMinimum();
    }

    /**
     * Return the maximum runtime needed by the given backend for predicates for
     * which it returned the specified answer.
     *
     * @param answer
     * @param backend
     *
     * @return
     */
    public Long getMaximumRuntime(Answer answer, Backend backend) {
        RegressionAnalysis<Long> analysis = getRegressionAnalysis(answer, backend);
        return analysis.getMaximum();
    }

    /**
     * Return the maximum runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Long getMaximumAnsweredRuntime(Backend backend) {
        return getAnswererdRegressionAnalysis(backend).getMaximum();
    }

    /**
     * Return the maximum runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Long getMaximumTimedRuntime(Backend backend) {
        return getTimedRegressionAnalysis(backend).getMaximum();
    }

    /**
     * Return the median runtime needed by the given backend for predicates for
     * which it returned the specified answer.
     *
     * @param answer
     * @param backend
     *
     * @return
     */
    public Double getMedianRuntime(Answer answer, Backend backend) {
        RegressionAnalysis<Long> analysis = getRegressionAnalysis(answer, backend);
        return analysis.getMedian();
    }

    /**
     * Return the median runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Double getMedianAnsweredRuntime(Backend backend) {
        return getAnswererdRegressionAnalysis(backend).getMedian();
    }

    /**
     * Return the median runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Double getMedianTimedRuntime(Backend backend) {
        return getTimedRegressionAnalysis(backend).getMedian();
    }

    /**
     * Return the first quartile runtime needed by the given backend for predicates for
     * which it returned the specified answer.
     *
     * @param answer
     * @param backend
     *
     * @return
     */
    public Double getFirstQuartileRuntime(Answer answer, Backend backend) {
        RegressionAnalysis<Long> analysis = getRegressionAnalysis(answer, backend);
        return analysis.getFirstQuartile();
    }

    /**
     * Return the first quartile runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Double getFirstQuartileAnsweredRuntime(Backend backend) {
        return getAnswererdRegressionAnalysis(backend).getFirstQuartile();
    }

    /**
     * Return the first quartile runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Double getFirstQuartileTimedRuntime(Backend backend) {
        return getTimedRegressionAnalysis(backend).getFirstQuartile();
    }

    /**
     * Return the third quartile runtime needed by the given backend for predicates for
     * which it returned the specified answer.
     *
     * @param answer
     * @param backend
     *
     * @return
     */
    public Double getThirdQuartileRuntime(Answer answer, Backend backend) {
        RegressionAnalysis<Long> analysis = getRegressionAnalysis(answer, backend);
        return analysis.getThirdQuartile();
    }

    /**
     * Return the third quartile runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Double getThirdQuartileAnsweredRuntime(Backend backend) {
        return getAnswererdRegressionAnalysis(backend).getThirdQuartile();
    }

    /**
     * Return the third quartile runtime needed by the given backend for predicates for
     * which the answer was SOLVABLE or UNKNOWN.
     *
     * @param backend
     *
     * @return
     */
    public Double getThirdQuartileTimedRuntime(Backend backend) {
        return getTimedRegressionAnalysis(backend).getThirdQuartile();
    }

    void countFastest(Map<Backend, TimedAnswer> timings) {
        Set<Backend> fastestBackends = new HashSet<>();
        Long fastestTime = Long.MAX_VALUE;

        for (Map.Entry<Backend, TimedAnswer> entry : timings.entrySet()) {
            TimedAnswer tAnswer = entry.getValue();
            if (tAnswer == null) {
                continue;
            }
            Answer answer = tAnswer.getAnswer();

            // Skip errors and timeouts
            if (answer.equals(Answer.TIMEOUT) || answer.equals(Answer.ERROR)) {
                continue;
            }

            Long time = tAnswer.getNanoSeconds();
            if (fastestTime > time) {
                fastestTime = time;
                fastestBackends.clear();
                fastestBackends.add(entry.getKey());
            } else if (fastestTime.equals(time)) {
                fastestBackends.add(entry.getKey());
            }
        }

        fastestBackends.stream().forEach(fastest::add);
    }

    /**
     * Returns the number of times the given backend answered faster than others.
     * <p>
     * Only takes the answers VALID, INVALID, SOLVABLE, and UNKNOWN into account.
     *
     * @param b
     *
     * @return
     */
    public Long getFastestAnswerCount(Backend b) {
        return fastest.getCount(b);
    }

    /**
     * Returns the number of BMachines from which the database was build.
     *
     * @return
     */
    public int getBMachineCount() {
        return bMachines.size();
    }

    /**
     * Merges the data of the other analysis into this.
     * <p>
     * Returns reference to itself for method chaining.
     *
     * @param other Analysis data to merge into this.
     *
     * @return
     */
    @Override
    public synchronized PredDbAnalysis mergeWith(PredDbAnalysis other) {

        this.predCount += other.predCount;
        this.answers.mergeWith(other.answers);

        this.contradictions.addAll(other.contradictions);

        this.backendsSeen.addAll(other.backendsSeen);
        this.fastest.mergeWith(other.fastest);

        this.bMachines.addAll(other.bMachines);

        // Merge classification map
        this.backendAnswers = mergeAnalysisMap(this.backendAnswers, other.backendAnswers);

        // Merge the regression maps
        answeredRegressions = mergeAnalysisMap(answeredRegressions, other.answeredRegressions);
        timedRegressions = mergeAnalysisMap(timedRegressions, other.timedRegressions);

        for (Answer a : other.backendRegressions.keySet()) {
            if (this.backendRegressions.containsKey(a)) {
                this.backendRegressions.put(
                        a,
                        mergeAnalysisMap(
                                this.backendRegressions.get(a),
                                other.backendRegressions.get(a)));
            } else {
                this.backendRegressions.put(a, other.backendRegressions.get(a));
            }
        }

        return this;
    }

    /**
     * Merges two maps which have analysis data as values.
     * <p>
     * Returns the merged map.
     *
     * @param analysisMap
     * @param mergeMap
     * @param <K>
     * @param <S>
     * @param <A>
     *
     * @return
     */
    <K, S, A extends AnalysisData<S, A>> Map<K, A> mergeAnalysisMap(
            Map<K, A> analysisMap, Map<K, A> mergeMap) {
        Set<K> mergeKeys = mergeMap.keySet();
        for (K key : mergeKeys) {
            if (analysisMap.containsKey(key)) {
                analysisMap.get(key).mergeWith(mergeMap.get(key));
            } else {
                analysisMap.put(key, mergeMap.get(key));
            }
        }
        return analysisMap;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PredDbAnalysis) {
            PredDbAnalysis other = (PredDbAnalysis) o;

            return this.predCount.equals(other.predCount)
                   && this.answers.equals(other.answers)
                   && this.contradictions.equals(other.contradictions)
                   && this.backendAnswers.equals(other.backendAnswers)
                   && this.backendsSeen.equals(other.backendsSeen)
                   && this.backendRegressions.equals(other.backendRegressions)
                   && this.answeredRegressions.equals(other.answeredRegressions)
                   && this.timedRegressions.equals(other.timedRegressions)
                   && this.fastest.equals(other.fastest);

        }
        return false;
    }
}
