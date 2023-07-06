package de.hhu.stups.neurob.core.api;

import com.google.inject.Guice;
import de.prob.MainModule;
import de.prob.scripting.Api;

public class ProB2 {
    public static Api api = Guice.createInjector(new MainModule()).getInstance(Api.class);

    public static void reloadApi() {
        synchronized (ProB2.class) {
            api = Guice.createInjector(new MainModule()).getInstance(Api.class);
        }
    }
}
