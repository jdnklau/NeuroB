package de.hhu.stups.neurob.cli;

import org.apache.commons.cli.Options;

public interface CliModule {

    String getUsageInfo();

    String getHelpText();

    Options getOptions();

    void eval(String[] args) throws Exception;

}
