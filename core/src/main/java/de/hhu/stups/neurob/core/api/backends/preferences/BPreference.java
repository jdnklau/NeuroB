package de.hhu.stups.neurob.core.api.backends.preferences;

public class BPreference {
    private final String name;
    private final String value;

    public BPreference(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public static BPreference set(String name, String value) {
        return new BPreference(name, value);
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BPreference) {
            BPreference other = (BPreference) o;

            return this.name.equals(other.name)
                    && this.value.equals(other.value);
        }

        return false;
    }
}
