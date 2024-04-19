package de.hhu.stups.neurob.cli;

import de.hhu.stups.neurob.cli.data.DataCli;
import de.hhu.stups.neurob.cli.sampling.SamplingCli;
import de.hhu.stups.neurob.cli.solving.SolvingCli;
import org.apache.commons.cli.HelpFormatter;

public class NeuroBCli {

    public static void main(String[] args) {

        String key = (args.length > 0) ? args[0] : "help";

        // Modules
        DataCli data = new DataCli();

        try {
            switch (key) {
                case "data":
                    data.eval(args);
                    break;

                case "sampling":
                    SamplingCli sampling = new SamplingCli();
                    try {
                        sampling.eval(args);
                    } catch (Exception e) {
                        System.out.println(sampling.getHelpText());
                    }
                    break;

                case "solving":
                    SolvingCli cli = new SolvingCli();
                    try {
                        cli.eval(args);
                    } catch (Exception e) {
                        System.out.println(cli.getHelpText());
                    }
                    break;

                case "help":
                default:
                    System.out.println(data.getHelpText());
            }
        } catch (Exception e) {
            System.err.println("Exception found during NeuroB cli call");
            e.printStackTrace(System.err);
        }
    }
}
