package com.example;
import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;



public class Main {

    // //////////////////////////  METODER  ///////////////////////////////////////////

    public static final Locale SWEDISH = new Locale("sv", "SE");
    public static NumberFormat nf = NumberFormat.getNumberInstance(SWEDISH);

    public static void printMin(List<ElpriserAPI.Elpris> dagensPriser) {

        if(dagensPriser.isEmpty()){
            System.out.println("Inga priser tillgängliga");
            return;
        }

        ElpriserAPI.Elpris minPrice = dagensPriser.getFirst();
        for (ElpriserAPI.Elpris pris : dagensPriser) {
            if (pris.sekPerKWh() < minPrice.sekPerKWh()) {
                minPrice = pris;
            }
        }
        double oresPris = minPrice.sekPerKWh() * 100;
        String start = minPrice.timeStart().format(DateTimeFormatter.ofPattern("HH"));
        String end = minPrice.timeEnd().format(DateTimeFormatter.ofPattern("HH"));
        System.out.printf("Lägsta pris: %s-%s %s öre", start, end, nf.format(oresPris));


    }

    public static void printMax(List<ElpriserAPI.Elpris> dagensPriser) {
        if(dagensPriser.isEmpty()){
            System.out.println("Inga priser tillgängliga");
            return;
        }

        ElpriserAPI.Elpris maxPrice = dagensPriser.getFirst();
        for (ElpriserAPI.Elpris pris : dagensPriser) {
            if (pris.sekPerKWh() > maxPrice.sekPerKWh()) {
                maxPrice = pris;
            }
        }
        double oresPris = maxPrice.sekPerKWh() * 100;
        String start = maxPrice.timeStart().format(DateTimeFormatter.ofPattern("HH"));
        String end = maxPrice.timeEnd().format(DateTimeFormatter.ofPattern("HH"));

        System.out.printf("Högsta pris: %s-%s %s öre", start, end, nf.format(oresPris));

    }

    public static void printMean(List<ElpriserAPI.Elpris> dagensPriser) {
        double sum = 0;
        if(dagensPriser.isEmpty()){
            System.out.println("Inga priser tillgängliga");
            return;
        }

        for (ElpriserAPI.Elpris pris : dagensPriser) {
            sum += pris.sekPerKWh();
        }

        double medel = (sum / dagensPriser.size()) * 100;
        System.out.printf("Medelpris: %s öre", nf.format(medel));
    }

    public static void printSorted(List<ElpriserAPI.Elpris> dagensPriser) {

        if(dagensPriser.isEmpty()){
            System.out.println("Inga priser tillgängliga");
            return;
        }

        dagensPriser.sort(Comparator.comparing((ElpriserAPI.Elpris::sekPerKWh)));

        for (ElpriserAPI.Elpris pris : dagensPriser) {
            double prisIOre = pris.sekPerKWh() * 100;

            System.out.printf("%s-%s %s öre\n", pris.timeStart().format(DateTimeFormatter.ofPattern("HH")),
                    pris.timeEnd().format(DateTimeFormatter.ofPattern("HH")), nf.format(prisIOre));
        }
    }

    public static void chargingHours(List<ElpriserAPI.Elpris> samladePriser, int laddTid) {

        if(samladePriser.isEmpty()){
            System.out.println("Inga priser tillgängliga");
            return;
        }

        double minSum = Double.MAX_VALUE;
        int startTid = 0;
        double average = 0;

        for (int i = 0; i <= samladePriser.size() - laddTid; i++) {
            double sum = 0;
            for (int j = 0; j < laddTid; j++) {
                sum += samladePriser.get(i + j).sekPerKWh();
            }
            if (sum < minSum) {
                minSum = sum;
                startTid = i;
            }

            average = minSum / laddTid;
        }

        ElpriserAPI.Elpris start = samladePriser.get(startTid); //Set start time
        double oresPris = average * 100;
        System.out.printf("Påbörja laddning kl %s för %d timmars laddning\nMedelpris för fönster: %s öre"
                ,start.timeStart().format(DateTimeFormatter.ofPattern("HH:mm")), laddTid, nf.format(oresPris));
    }


    public static void printHelp(){

        System.out.println("--zone SE1|SE2|SE3|SE4 (required)");
        System.out.println("--date YYYY-MM-DD (optional, defaults to current date)");
        System.out.println("--sorted (optional, to display prices in descending order)");
        System.out.println("--charging 2h|4h|8h (optional, to find optimal charging windows)");
        System.out.println("--help (optional, to display usage information)");
    }

    public static void visaHuvudMeny(String zon, LocalDate datum) {
        System.out.println("=========================================");
        System.out.println("   -  ELPRISKOLL - ");
        System.out.println("=========================================");
        System.out.println("1. Visa lägsta, högsta och medelpris");
        System.out.println("2. Visa alla priser sorterade");
        System.out.println("3. Optimera laddning (2, 4 eller 8 timmar)");
        System.out.println("4. Ändra elområde (SE1 SE2 SE3 SE4)");
        System.out.println("5. Ändra datum");
        System.out.println("0. Avsluta");
        System.out.println("-----------------------------------------");
        System.out.print("Välj ett alternativ (0-5): ");
    }



    /// //////////////////////////  MAIN  ///////////////////////////////////////////


    public static void main(String[] args) {
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        String zon = "";
        int laddTid = 0;
        boolean sorted = false;
        boolean validationError = false;
        LocalDate datum = LocalDate.now();

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {

                    case "--interactive":
                        visaHuvudMeny(zon,datum);
                        return;

                    case "--zone":
                        if (i + 1 >= args.length) {
                        System.out.println("Ogiltig zon");
                        validationError = true;
                        break;
                        }
                        String valdZon = args[++i].toUpperCase();

                        if (valdZon.equals("SE1") || valdZon.equals("SE2") || valdZon.equals("SE3")  || valdZon.equals("SE4")) {
                        zon = valdZon;
                        }else {
                            System.out.println("Ogiltig zon: " + valdZon);
                            validationError = true;
                        }

                        break;

                    case "--date":
                        if (i + 1 >= args.length) {
                        System.out.println("Ogiltigt datum");
                        validationError = true;
                        break;
                         }
                         String valtDatum = args[++i].trim();

                            if (valtDatum.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            datum = LocalDate.parse(valtDatum);
                         } else {
                            System.out.println("Ogiltigt datumformat: " + valtDatum);
                             validationError = true;
                            }
                         break;

                        case "--sorted":
                        sorted = true;
                        break;

                        case "--charging":
                        if (i + 1 >= args.length) {
                        System.out.println("Ogiltigt val för laddning, du måste välja 2h, 4h eller 8h");
                        validationError = true;
                        break;
                        }
                        String valdTimStr = args[++i].trim().replace("h", "");

                        try {
                        int valdTim = Integer.parseInt(valdTimStr);

                        if (valdTim == 2 || valdTim == 4 || valdTim == 8) {
                            laddTid = valdTim;
                        } else {
                            System.out.println("Ogiltigt val för laddning. Endast 2h, 4h eller 8h tillåts.");
                            validationError = true;
                        }
                        } catch (NumberFormatException e) {
                        System.out.println("Ogiltigt format för laddning: " + valdTimStr);
                        validationError = true;
                        }
                         break;

                        case "--help":
                        printHelp();
                        break;



                        default:
                        System.out.println("Okänt argument: " + args[i]);
                        validationError = true;
                         break;


            }
        }

        if (zon.isEmpty()) {
            zon = "SE3";
            printHelp();
        }

        ElpriserAPI.Prisklass valdZon = ElpriserAPI.Prisklass.valueOf(zon);
        List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(datum, valdZon);
        List<ElpriserAPI.Elpris> morgonDagensPriser = elpriserAPI.getPriser(datum.plusDays(1), valdZon); //

        // Vi samlar alla priser i en ny lista för laddning över tolvslaget.
        List<ElpriserAPI.Elpris> allaPriser =  new ArrayList<>();
        allaPriser.addAll(dagensPriser);
        allaPriser.addAll(morgonDagensPriser);

        // If-sats för Cli
        if (laddTid != 0) {
            chargingHours(allaPriser, laddTid);
        } else if (sorted) {
            printSorted(dagensPriser);
        } else {
            printMin(dagensPriser);
            printMax(dagensPriser);
            printMean(dagensPriser);
        }
    }


}