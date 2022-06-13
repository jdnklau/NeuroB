package de.hhu.stups.neurob.training.db;

import de.hhu.stups.neurob.core.api.backends.Answer;
import de.hhu.stups.neurob.core.api.backends.TimedAnswer;

import java.text.DecimalFormat;

public class SampledTimedAnswer extends TimedAnswer {

    protected SamplingStatistic stats;

    public SampledTimedAnswer(Answer answer, Long nanoseconds, SamplingStatistic stats) {
        super(answer, nanoseconds);
        this.stats = stats;
    }

    public SampledTimedAnswer(Answer answer, Long nanoseconds, SamplingStatistic stats, String message) {
        super(answer, nanoseconds, message);
        this.stats = stats;
    }

    public SamplingStatistic getStats() {
        return stats;
    }

    public static SampledTimedAnswer from(TimedAnswer answer) {
        SamplingStatistic stats = new SamplingStatistic(
                1, answer.getNanoSeconds(), 0, 0);

        return new SampledTimedAnswer(
                answer.getAnswer(),
                answer.getNanoSeconds(),
                stats,
                answer.getMessage());
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("0.000");
        return "["
               + "answer=" + this.answer + ", "
               + "nanoSeconds=" + this.nanoSeconds
               + ", sample-size=" + this.stats.getSampleSize()
//               + ", mean=" + this.stats.getMean()  // this.nanoSeconds supposed to be mean already.
               + ", stdev=" + df.format(this.stats.getStdev())
               + ", sem=" + df.format(this.stats.getSem())
               + (this.message != null ? ", message=" + this.message : "")
               + "]";
    }
}
