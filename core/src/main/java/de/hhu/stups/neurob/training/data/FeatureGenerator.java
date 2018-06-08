package de.hhu.stups.neurob.training.data;

import de.hhu.stups.neurob.core.api.Features;

/**
 * Factory class for generic creation of specific {@link Features}.
 * If a specific {@link Features} instance is needed, it can be created using
 * the {@link #create(Class, Object)} method.
 * <p>
 * Note that this is only a utility class for
 * </p>
 */
public class FeatureGenerator {

    /**
     * Instantiates a {@link Features} object of given class.
     * <p>
     * There are some limitations, as the given object <code>object</code>
     * is passed as constructor argument.
     * If the respective constructor does not exist, an exception is thrown
     * accordingly.
     * </p>
     *
     * @param featureClass
     * @param object
     * @param <F>
     *
     * @return
     *
     * @throws InstantiationException
     */
    public static <F extends Features> F create(Class<F> featureClass,
            Object object) throws InstantiationException {
        try {
            return featureClass .getConstructor(object.getClass())
                    .newInstance(object);
        } catch (Exception e) {
            throw (InstantiationException) new InstantiationException(
                    "Unable to create instance of " + featureClass.getName())
                    .initCause(e);
        }
    }
}
