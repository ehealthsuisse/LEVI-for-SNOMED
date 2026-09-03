package ch.ehealth.levi.core.compare;

import ch.ehealth.levi.core.Conf;

public class SnomedComparisonMain {

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.err.println("Usage: SnomedComparisonMain <frDescPath> <chDescPath> <frLangPath> <chLangPath> <outputDir>");
            System.exit(1);
        }

        Conf conf = new Conf();
        conf.setDestination(args[4].endsWith("/") || args[4].endsWith("\\")
                ? args[4] : args[4] + "/");

        CompareManager manager = new CompareManager(conf);
        manager.runSnomedComparison(args[0], args[1], args[2], args[3], conf.getDestination());
    }
}