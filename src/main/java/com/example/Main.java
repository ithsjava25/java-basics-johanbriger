package com.example;
import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.time.format.DateTimeParseException;



public class Main {

    // //////////////////////////  METODER  ///////////////////////////////////////////

    public static final Locale SWEDISH = new Locale("sv", "SE");
    public static NumberFormat nf = NumberFormat.getNumberInstance(SWEDISH);
    private static final LocalTime PRIS_RELEASE_TIME = LocalTime.of(13, 0);

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
        System.out.printf("Lägsta pris: %s-%s %s öre \n", start, end, nf.format(oresPris));


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

        System.out.printf("Högsta pris: %s-%s %s öre \n", start, end, nf.format(oresPris));

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
        System.out.printf("Medelpris: %s öre \n", nf.format(medel));
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
        System.out.printf("Påbörja laddning kl %s för %d timmars laddning\nMedelpris för fönster: %s öre \n"
                ,start.timeStart().format(DateTimeFormatter.ofPattern("HH:mm")), laddTid, nf.format(oresPris));
    }



    public static void printHelp(){

        System.out.println("--zone SE1|SE2|SE3|SE4 (required)");
        System.out.println("--date YYYY-MM-DD (optional, defaults to current date)");
        System.out.println("--sorted (optional, to display prices in descending order)");
        System.out.println("--charging 2h|4h|8h (optional, to find optimal charging windows)");
        System.out.println("--help (optional, to display usage information)");
        System.out.println("--interactive (interactive menu)");
    }

    public static void visaHuvudMeny(String zon, LocalDate datum) {
        System.out.println("=========================================");
        System.out.printf("ELPRISKOLL - Aktuell Zon: %s, Datum: %s\n", zon, datum.format(DateTimeFormatter.ISO_DATE));
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

    public static void interactiveMenu(ElpriserAPI elpriserAPI) {
        Scanner scanner = new Scanner(System.in);
        String zon = "SE3";
        LocalDate datum = LocalDate.now();
        boolean running = true;

        while (running) {
            visaHuvudMeny(zon, datum);
            String input = scanner.nextLine().trim();
            int val = Integer.parseInt(input);
            try {


                if (val == 0) {
                    running = false;
                    System.out.println("Interaktiv meny avslutad");
                    continue;
                }

                // Förbered data för de val som kräver prisdata (1, 2, 3)
                List<ElpriserAPI.Elpris> dagensPriser = new ArrayList<>();
                List<ElpriserAPI.Elpris> morgonDagensPriser = new ArrayList<>();
                List<ElpriserAPI.Elpris> allaPriser = new ArrayList<>();

                if (val >= 1 && val <= 3) { // Vid val 1-3 behöver vi hämta vald datum & zon
                    System.out.println("\nHämtar priser för zon " + zon + " den " + datum.format(DateTimeFormatter.ISO_DATE));
                    ElpriserAPI.Prisklass valdZon = ElpriserAPI.Prisklass.valueOf(zon);
                    dagensPriser = elpriserAPI.getPriser(datum, valdZon);

                    if (val == 3) { // Vid val 3 hämtar vid 48 tim för laddning över tolvslaget
                        morgonDagensPriser = elpriserAPI.getPriser(datum.plusDays(1), valdZon);
                        allaPriser.addAll(dagensPriser);
                        allaPriser.addAll(morgonDagensPriser);
                    }

                    if (dagensPriser.isEmpty()) {
                        System.out.println("Kunde inte hämta priser för det valda datumet/zonen.");
                        continue;
                    }
                }

                switch (val) {
                    case 1:
                        printMin(dagensPriser);
                        printMax(dagensPriser);
                        printMean(dagensPriser);
                        break;
                    case 2:
                        printSorted(dagensPriser);
                        break;
                    case 3:
                        System.out.print("Ange laddtid i timmar (2, 4 eller 8): ");
                        String tidStr = scanner.nextLine().trim();
                        try {
                            int laddTid = Integer.parseInt(tidStr);
                            if (laddTid == 2 || laddTid == 4 || laddTid == 8) {
                                chargingHours(allaPriser, laddTid);
                            } else {
                                System.out.println("Ogiltigt val. Välj 2, 4 eller 8.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Ogiltig inmatning för laddtid.");
                        }
                        break;
                    case 4:
                        System.out.print("Ange nytt elområde (SE1, SE2, SE3, SE4): ");
                        String nyZon = scanner.nextLine().trim().toUpperCase();
                        if (nyZon.matches("SE[1-4]")) {
                            zon = nyZon;
                            System.out.println("Elområde uppdaterat till " + zon + ".");
                        } else {
                            System.out.println("Ogiltig zon. Måste vara SE1, SE2, SE3 eller SE4.");
                        }
                        break;
                    case 5:
                        System.out.print("Ange nytt datum (YYYY-MM-DD, t.ex. 2025-10-31): ");
                        String nyttDatumStr = scanner.nextLine().trim();
                        try {
                            datum = LocalDate.parse(nyttDatumStr, DateTimeFormatter.ISO_DATE);
                            System.out.println("Datum uppdaterat till " + datum.format(DateTimeFormatter.ISO_DATE) + ".");
                        } catch (DateTimeParseException e) {
                            System.out.println("Ogiltigt datumformat. Använd YYYY-MM-DD.");
                        }
                        break;
                    default:
                        System.out.println("Ogiltigt val. Försök igen.");
                }

            } catch (NumberFormatException e) {
                System.out.println("Ogiltig inmatning. Vänligen ange ett nummer (0-5).");
            } catch (IllegalArgumentException e) {
                System.out.println("Fel: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Ett oväntat fel uppstod: " + e.getMessage());
            }

            // Vänta på enter för att se nästa meny
            if(running && val != 4 && val != 5 && val != 0){
                System.out.print("Tryck Enter för att återgå till menyn...");
                scanner.nextLine();
            }

        }
        scanner.close();
    }





    /// //////////////////////////  MAIN  ///////////////////////////////////////////


    public static void main(String[] args) {
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        String zon = "";
        int laddTid = 0;
        boolean sorted = false;
        LocalDate datum = LocalDate.now();

        if (args.length == 0) {
            printHelp();
            return;
        }

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {

                    case "--interactive":
                        interactiveMenu(elpriserAPI);
                        return;

                    case "--zone":
                        if (i + 1 >= args.length) {
                        System.out.println("Ogiltig zon");
                            break;
                        }
                        String valdZon = args[++i].toUpperCase();

                        if (valdZon.matches("SE[1-4]")) {
                        zon = valdZon;
                        }else {
                            System.out.println("Ogiltig zon: " + valdZon);

                        }

                        break;

                    case "--date":
                        if (i + 1 >= args.length) {
                        System.out.println("Ogiltigt datum");
                            break;
                         }
                         String valtDatum = args[++i].trim();

                            if (valtDatum.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            datum = LocalDate.parse(valtDatum);
                         } else {
                            System.out.println("Ogiltigt datumformat: " + valtDatum);

                            }
                         break;

                        case "--sorted":
                        sorted = true;
                        break;

                        case "--charging":
                        if (i + 1 >= args.length) {
                        System.out.println("Ogiltigt val för laddning, du måste välja 2h, 4h eller 8h");

                        break;
                        }
                        String valdTimStr = args[++i].trim().replace("h", "");

                        try {
                        int valdTim = Integer.parseInt(valdTimStr);

                        if (valdTim == 2 || valdTim == 4 || valdTim == 8) {
                            laddTid = valdTim;
                        } else {
                            System.out.println("Ogiltigt val för laddning. Endast 2h, 4h eller 8h tillåts.");

                        }
                        } catch (NumberFormatException e) {
                        System.out.println("Ogiltigt format för laddning: " + valdTimStr);

                        }
                         break;

                        case "--help":
                        printHelp();
                        break;



                        default:
                        System.out.println("Okänt argument: " + args[i]);

                         break;


            }
        }

        if (zon.isEmpty()) {
            zon = "SE3";
            printHelp();
        }

        ElpriserAPI.Prisklass valdZon = ElpriserAPI.Prisklass.valueOf(zon);
        List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(datum, valdZon);


        // För tillgång till båda dagarnas prislista om kl är efter 13:00
        List<ElpriserAPI.Elpris> aktuellaPriser = dagensPriser;
        boolean isAfterReleaseTime = LocalTime.now().isAfter(PRIS_RELEASE_TIME) || LocalTime.now().equals(PRIS_RELEASE_TIME);

        if (laddTid == 0 && isAfterReleaseTime) { // Efter 13
            List<ElpriserAPI.Elpris> morgondagensPriser = elpriserAPI.getPriser(datum.plusDays(1), valdZon);

            if (!morgondagensPriser.isEmpty()) {
                // Sammanfoga dagens och morgondagens priser till en enda lista för visning
                aktuellaPriser = new ArrayList<>();
                aktuellaPriser.addAll(dagensPriser);
                aktuellaPriser.addAll(morgondagensPriser);

            }
        }

        // If-sats för Cli
        if (laddTid != 0) {
            List<ElpriserAPI.Elpris> morgondagensPriser = elpriserAPI.getPriser(datum.plusDays(1), valdZon);
            List<ElpriserAPI.Elpris> allaPriser =  new ArrayList<>();
            allaPriser.addAll(dagensPriser);
            allaPriser.addAll(morgondagensPriser);

            chargingHours(allaPriser, laddTid);
        } else if (sorted) {
            printSorted(dagensPriser);
        } else {
            printMin(aktuellaPriser);
            printMax(aktuellaPriser);
            printMean(aktuellaPriser);
        }
    }


}