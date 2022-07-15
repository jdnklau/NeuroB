package de.hhu.stups.neurob.cli;

import de.hhu.stups.neurob.cli.data.DataCli;
import de.hhu.stups.neurob.cli.sampling.SamplingCli;
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
                    sampling.eval(args);
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
