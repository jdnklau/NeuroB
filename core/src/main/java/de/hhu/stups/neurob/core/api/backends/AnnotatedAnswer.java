package de.hhu.stups.neurob.core.api.backends;

public class AnnotatedAnswer {
    private final Answer answer;
    private final String message;

    public AnnotatedAnswer(Answer answer, String message) {
        this.answer = answer;
        this.message = message;
    }

    public Answer getAnswer() {
        return answer;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Instantiates a {@link TimedAnswer} instance which
     * hold this answer's {@link #getAnswer() answer}
     * and {@link #getMessage() annotation} values,
     * and also has the given, associated runtime.
     * <p>
     * The runtime is expected to be in nanoseconds.
     *
     * @param nanoseconds Time to be encapsulated in the new TimedAnswer.
     *
     * @return
     */
    public TimedAnswer getTimedAnswer(Long nanoseconds) {
        return new TimedAnswer(answer, nanoseconds, message);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AnnotatedAnswer) {
            AnnotatedAnswer other = (AnnotatedAnswer) o;

            // Messages, like in TimedAnswer, are not the main focus here
            return this.answer != null
                            ? this.answer.equals(other.answer)
                            : other.answer == null;
        } else if(o instanceof Answer) {
            Answer other = (Answer) o;
            return other.equals(this.answer);
        }

        return false;
    }
}
