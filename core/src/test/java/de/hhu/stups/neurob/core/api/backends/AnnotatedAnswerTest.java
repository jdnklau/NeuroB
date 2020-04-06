package de.hhu.stups.neurob.core.api.backends;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class AnnotatedAnswerTest {

    @Test
    void shouldTranslateToTimedAnswer() {
        Answer answer = Answer.VALID;
        String message = "informative message";
        Long nanoseconds = 200000L;

        AnnotatedAnswer anno = new AnnotatedAnswer(answer, message);

        TimedAnswer expected = new TimedAnswer(answer, nanoseconds, message);
        TimedAnswer actual = anno.getTimedAnswer(nanoseconds);

        assertEquals(expected, actual);
    }

    @Test
    void shouldBeEqual() {
        Answer answer = Answer.VALID;
        String message = "informative message";

        AnnotatedAnswer answer1 = new AnnotatedAnswer(answer, message);
        AnnotatedAnswer answer2 = new AnnotatedAnswer(answer, message);

        assertEquals(answer1, answer2);
    }

    @Test
    void shouldBeEqualWithAnswer() {
        Answer answer = Answer.VALID;
        String message = "informative message";

        AnnotatedAnswer answer1 = new AnnotatedAnswer(answer, message);

        assertTrue(answer1.equals(Answer.VALID));
    }

    @Test
    void shouldBeEqualWhenAnswersAreNull() {
        Answer answer = null;
        String message = "informative message";

        AnnotatedAnswer answer1 = new AnnotatedAnswer(answer, message);
        AnnotatedAnswer answer2 = new AnnotatedAnswer(answer, message);

        assertEquals(answer1, answer2);
    }

    @Test
    void shouldBeUnequal() {
        String message = "informative message";

        AnnotatedAnswer answer1 = new AnnotatedAnswer(Answer.VALID, message);
        AnnotatedAnswer answer2 = new AnnotatedAnswer(Answer.INVALID, message);

        assertNotEquals(answer1, answer2);
    }

}
