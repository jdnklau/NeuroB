package de.hhu.stups.neurob.core.api;

import com.google.inject.Guice;
import de.prob.MainModule;
import de.prob.scripting.Api;

public class ProB2 {
    public static final Api api = Guice.createInjector(new MainModule()).getInstance(Api.class);
}
