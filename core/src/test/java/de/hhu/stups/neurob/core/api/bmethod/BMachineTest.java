package de.hhu.stups.neurob.core.api.bmethod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BMachineTest {

    @Test
    void shouldGiveLocationAndTypeWhenMchPath() {
        BMachine mch = new BMachine("non/existent.mch");

        String expected = "non/existent.mch[CLASSICALB]";
        String actual = mch.toString();

        assertEquals(expected, actual);
    }

    @Test
    void shouldGiveLocationAndTypeWhenBcmPath() {
        BMachine mch = new BMachine("non/existent.bcm");

        String expected = "non/existent.bcm[EVENTB]";
        String actual = mch.toString();

        assertEquals(expected, actual);
    }

}
