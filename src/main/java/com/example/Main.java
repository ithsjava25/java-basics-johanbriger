package com.example;

import com.example.api.ElpriserAPI;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {

    private static final DecimalFormat df;

    // Till den interaktiva menyn
    private static ElpriserAPI.Prisklass currentZone = ElpriserAPI.Prisklass.SE3;
    private static LocalDate currentDate = LocalDate.now();

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("sv-SE"));
        symbols.setDecimalSeparator(',');
        df = new DecimalFormat("0.00", symbols);
    }

    public static void main(String[] args) {
        Map<String, String> flags = parseArgs(args);

        if (args.length == 0 || flags.containsKey("--help")) {
            printHelp();
            return;
        }

        if(flags.containsKey("--interactive")) {
            interactiveMenu();
            return;
        }

        if (!flags.containsKey("--zone")) {
            System.out.println("--zone måste anges.");
            printHelp();
            return;
        }

        ElpriserAPI.Prisklass zone;
        try {
            zone = ElpriserAPI.Prisklass.valueOf(flags.get("--zone").toUpperCase());
        } catch (Exception e) {
            System.out.println("Ogiltig zon. Använd SE1, SE2, SE3 eller SE4.");
            return;
        }

        LocalDate date;
        if (flags.containsKey("--date")) {
            try {
                date = LocalDate.parse(flags.get("--date"), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                System.out.println("Ogiltigt datumformat. Använd formatet YYYY-MM-DD.");
                return;
            }
        } else {
            date = LocalDate.now();
        }
        // Hämta dagens och morgondagens listor
        ElpriserAPI elpriserAPI = new ElpriserAPI(false);
        List<ElpriserAPI.Elpris> valdaPriser = elpriserAPI.getPriser(date, zone);
        List<ElpriserAPI.Elpris> morgondagensPriser = elpriserAPI.getPriser(date.plusDays(1), zone);

        // Från 94 inputs till 24
        if(valdaPriser.size() == 96){

            valdaPriser = convertFrom96(valdaPriser);
            morgondagensPriser = convertFrom96(morgondagensPriser);

        }

        if (valdaPriser.isEmpty()) {
            System.out.println("Inga priser hittades för " + date + " i zon " + zone);
            return;
        }

        // Laddfönster
        if (flags.containsKey("--charging")) {
            if (morgondagensPriser != null && !morgondagensPriser.isEmpty()) {
                valdaPriser.addAll(morgondagensPriser);
            }
        }

        if (flags.containsKey("--charging")) {
            int hours = 0;
            String val = flags.get("--charging");
            if ("2h".equals(val)) hours = 2;
            if ("4h".equals(val)) hours = 4;
            if ("8h".equals(val)) hours = 8;

            if (hours > 0) {
                chargingWindow(valdaPriser, hours);
            }return;
        }

        // SORTERAT
        if (flags.containsKey("--sorted")) {
            sortedPrices(valdaPriser);
            return; // Avsluta efter sorterad utskrift
        }

        double mean = calculateMean(valdaPriser);
        ElpriserAPI.Elpris min = calculateMin(valdaPriser);
        ElpriserAPI.Elpris max = calculateMax(valdaPriser);

        System.out.println("Medelpris: " + df.format(mean * 100) + " öre/kWh");

        if (min != null) {
            System.out.printf("Lägsta pris: %02d-%02d %s öre%n",
                    min.timeStart().getHour(),
                    min.timeEnd().getHour(),
                    df.format(min.sekPerKWh() * 100));
        }
        if (max != null) {
            System.out.printf("Högsta pris: %02d-%02d %s öre%n",
                    max.timeStart().getHour(),
                    max.timeEnd().getHour(),
                    df.format(max.sekPerKWh() * 100));
        }

    }

    private static List<ElpriserAPI.Elpris> convertFrom96(List<ElpriserAPI.Elpris> valdaPriser) {
        List<ElpriserAPI.Elpris> hourlyPrices = new ArrayList<>();

        for (int i = 0; i < valdaPriser.size(); i += 4) {
            double sekSum = 0;
            double eurSum = 0;
            double exrSum = 0;

            for (int j = 0; j < 4; j++) {
                ElpriserAPI.Elpris p = valdaPriser.get(i + j); sekSum += p.sekPerKWh(); eurSum += p.eurPerKWh(); exrSum += p.exr();
            }

            double sekAvg = sekSum / 4.0; double eurAvg = eurSum / 4.0; double exrAvg = exrSum / 4.0;

            ElpriserAPI.Elpris first = valdaPriser.get(i);
            ElpriserAPI.Elpris last = valdaPriser.get(i + 3);


            ElpriserAPI.Elpris hourly = new ElpriserAPI.Elpris(sekAvg, eurAvg, exrAvg, first.timeStart(), last.timeEnd()
            );

            hourlyPrices.add(hourly);
        }

        return hourlyPrices;
    }


    private static double calculateMean(List<ElpriserAPI.Elpris> priser) {
        if (priser.isEmpty()) return 0.0;

        double total = 0;
        for (ElpriserAPI.Elpris p : priser) {
            total += p.sekPerKWh();
        }
        return total / priser.size();
    }

    private static ElpriserAPI.Elpris calculateMin(List<ElpriserAPI.Elpris> priser) {
        ElpriserAPI.Elpris min = null;
        double minVal = Double.MAX_VALUE;
        for (ElpriserAPI.Elpris p : priser) {
            if (p.sekPerKWh() < minVal) {
                minVal = p.sekPerKWh();
                min = p;
            }
        }
        return min;
    }

    private static ElpriserAPI.Elpris calculateMax(List<ElpriserAPI.Elpris> priser) {
        ElpriserAPI.Elpris max = null;
        double maxVal = Double.MIN_VALUE;
        for (ElpriserAPI.Elpris p : priser) {
            if (p.sekPerKWh() > maxVal) {
                maxVal = p.sekPerKWh();
                max = p;
            }
        }
        return max;
    }

    public static void sortedPrices(List<ElpriserAPI.Elpris> priser) {

        // Om listan är tom, avbryt
        if (priser.isEmpty()) {
            System.out.println("Kunde inte sortera: Ingen prisdata tillgänglig.");
            return;
        }

        for (int i = 0; i < priser.size() - 1; i++) {
            for (int j = i + 1; j < priser.size(); j++) {
                // Jämför priset i SEK per kWh
                if (priser.get(i).sekPerKWh() > priser.get(j).sekPerKWh()) {
                    // Byte (Swap)
                    ElpriserAPI.Elpris temp = priser.get(i);
                    priser.set(i, priser.get(j));
                    priser.set(j, temp);
                }
            }
        }

        for (ElpriserAPI.Elpris p : priser) {

            String start = p.timeStart().format(DateTimeFormatter.ofPattern("HH"));
            String end = p.timeEnd().format(DateTimeFormatter.ofPattern("HH"));

            System.out.printf("%s-%s %s öre%n", start, end, df.format(p.sekPerKWh() * 100));
        }

    }

    private static void chargingWindow(List<ElpriserAPI.Elpris> priser, int hours) {
        double bestSum = Double.MAX_VALUE;
        int bestIndex = -1;

        for (int i = 0; i <= priser.size() - hours; i++) {
            double sum = 0;
            for (int j = 0; j < hours; j++) {
                sum += priser.get(i + j).sekPerKWh();
            }
            if (sum < bestSum) {
                bestSum = sum;
                bestIndex = i;
            }
        }

        if (bestIndex >= 0) {
            ElpriserAPI.Elpris start = priser.get(bestIndex);
            ElpriserAPI.Elpris end = priser.get(bestIndex + hours - 1);

            double mean = bestSum / hours;
            System.out.printf("Påbörja laddning kl %02d:00 (%dh)\nMedelpris för fönster: %s öre%n",
                    start.timeStart().getHour(),
                    hours, df.format(mean * 100));
            System.out.printf("Slutar: %02d:00%n", end.timeEnd().getHour());
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    map.put(arg, args[i + 1]);
                    i++;
                } else {
                    map.put(arg, "true");
                }
            }
        }
        return map;
    }

    private static void printHelp() {
        System.out.println("Usage: java -cp target/classes com.example.Main --zone SE1|SE2|SE3|SE4 [options]");
        System.out.println("Options:");
        System.out.println("  --zone SE1|SE2|SE3|SE4   (required)");
        System.out.println("  --date YYYY-MM-DD        (optional, default today)");
        System.out.println("  --sorted                 (optional, show sorted prices ascending)");
        System.out.println("  --charging 2h|4h|8h      (optional, find optimal charging window)");
        System.out.println("  --help                   (show this help)");
        System.out.println("  --interactive            (Use the interactive menu)");
    }

    public static void interactiveMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        ElpriserAPI elpriserAPI = new ElpriserAPI(false);

        while (running) {

            // Visar vald zon och valt datum (Standard är se3 och dagens datum.
            System.out.println("\n--- Interaktiv Meny för ElpriserAPI ---");
            System.out.println("Vald zon: " + currentZone);
            System.out.println("Valt datum: " + currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            System.out.println("------------------------------------");

            // Meny
            System.out.println("1. Välj zon (SE1-SE4)");
            System.out.println("2. Välj datum (YYYY-MM-DD, standard: nuvarande)");
            System.out.println("3. Hämta och visa statistik för:  medel, min, max");
            System.out.println("4. Visa priser sorterat (lägsta till högsta)");
            System.out.println("5. Hitta bästa laddfönster (2h, 4h, eller 8h)");
            System.out.println("6. Avsluta");
            System.out.print("Välj ett alternativ: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    currentZone = selectZone(scanner);
                    System.out.println("Zon satt till: " + currentZone);
                    break;
                case "2":
                    currentDate = selectDate(scanner);
                    System.out.println("Datum satt till: " + currentDate);
                    break;
                case "3":
                case "4":
                case "5":


                    // Hämta data baserat på de statiska fälten
                    System.out.println("Hämtar priser för " + currentDate + " i zon " + currentZone + "...");
                    List<ElpriserAPI.Elpris> valdaPriser = elpriserAPI.getPriser(currentDate, currentZone);
                    List<ElpriserAPI.Elpris> morgondagensPriser = elpriserAPI.getPriser(currentDate.plusDays(1), currentZone);


                    if(valdaPriser.size() == 96){
                        valdaPriser = convertFrom96(valdaPriser);
                        morgondagensPriser = convertFrom96(morgondagensPriser);
                    }


                    if (valdaPriser.isEmpty()) {
                        System.out.println("Inga priser hittades för " + currentDate + " i zon " + currentZone + ". Kontrollera datumet.");
                        break;
                    }

                    if ("3".equals(choice)) {
                        // Statistik
                        double mean = calculateMean(valdaPriser);
                        ElpriserAPI.Elpris min = calculateMin(valdaPriser);
                        ElpriserAPI.Elpris max = calculateMax(valdaPriser);

                        System.out.println("\n--- Statistik för " + currentDate + " i " + currentZone + " ---");
                        System.out.println("Medelpris: " + df.format(mean * 100) + " öre/kWh");

                        if (min != null) {
                            System.out.printf("Lägsta pris: %02d-%02d %s öre%n",
                                    min.timeStart().getHour(),
                                    min.timeEnd().getHour(),
                                    df.format(min.sekPerKWh() * 100));
                        }
                        if (max != null) {
                            System.out.printf("Högsta pris: %02d-%02d %s öre%n",
                                    max.timeStart().getHour(),
                                    max.timeEnd().getHour(),
                                    df.format(max.sekPerKWh() * 100));
                        }
                    } else if ("4".equals(choice)) {
                        // Sorterat
                        System.out.println("\n--- Priser sorterade (lägsta först) för " + currentDate + " i " + currentZone + " ---");
                        List<ElpriserAPI.Elpris> sortedList = new ArrayList<>(valdaPriser);
                        sortedPrices(sortedList);
                    } else if ("5".equals(choice)) {
                        // Laddfönster
                        if (morgondagensPriser != null && !morgondagensPriser.isEmpty()) {
                            valdaPriser.addAll(morgondagensPriser); // Inkludera morgondagens priser
                            System.out.println("Laddfönster söker över både " + currentDate + " och " + currentDate.plusDays(1) + ".");
                        }

                        System.out.print("Välj laddningstid (2, 4, eller 8 timmar): ");
                        String hoursStr = scanner.nextLine().trim();
                        int hours = 0;
                        if ("2".equals(hoursStr)) hours = 2;
                        else if ("4".equals(hoursStr)) hours = 4;
                        else if ("8".equals(hoursStr)) hours = 8;

                        if (hours > 0) {
                            System.out.println("\n--- Bästa laddfönster på " + hours + " timmar ---");
                            chargingWindow(valdaPriser, hours);
                        } else {
                            System.out.println("Ogiltigt val. Välj 2, 4 eller 8.");
                        }
                    }
                    break;
                case "6":
                    running = false;
                    System.out.println("Avslutar interaktiv meny.");
                    break;
                default:
                    System.out.println("Ogiltigt val, försök igen.");
            }
        }
        scanner.close();
    }

    // Metod för att välja zon till interaktiva menyn
    private static ElpriserAPI.Prisklass selectZone(Scanner scanner) {
        ElpriserAPI.Prisklass zone = null;
        while (zone == null) {
            System.out.print("Ange elområde (SE1, SE2, SE3, SE4): ");
            String zoneStr = scanner.nextLine().trim().toUpperCase();
            try {
                zone = ElpriserAPI.Prisklass.valueOf(zoneStr);
            } catch (IllegalArgumentException e) {
                System.out.println("Ogiltig zon. Försök igen.");
            }
        }
        return zone;
    }

    // Metod för att välja datum till interaktiva menyn
    private static LocalDate selectDate(Scanner scanner) {
        LocalDate date = currentDate; // Använd det senaste valda datumet som standard
        System.out.print("Ange datum (YYYY-MM-DD) eller tryck Enter för aktuellt datum (" + date + "): ");
        String dateStr = scanner.nextLine().trim();
        if (!dateStr.isEmpty()) {
            try {
                date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                System.out.println("Ogiltigt datumformat. Använder det tidigare valda datumet.");
            }
        }
        return date;
    }


}