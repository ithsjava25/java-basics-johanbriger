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

        if (!flags.containsKey("--zone")) {
            System.out.println("Fel: --zone måste anges.");
            printHelp();
            return;
        }

        ElpriserAPI.Prisklass zone;
        try {
            zone = ElpriserAPI.Prisklass.valueOf(flags.get("--zone"));
        } catch (Exception e) {
            System.out.println("Fel: Ogiltig zon. Använd SE1, SE2, SE3 eller SE4.");
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

        ElpriserAPI elpriserAPI = new ElpriserAPI(false); // disable caching
        List<ElpriserAPI.Elpris> valdaPriser = elpriserAPI.getPriser(date, zone);

        if (valdaPriser.isEmpty()) {
            System.out.println("Inga priser hittades för " + date + " i zon " + zone);
            return;
        }

        if (flags.containsKey("--charging")) {
            List<ElpriserAPI.Elpris> morgondagensPriser = elpriserAPI.getPriser(date.plusDays(1), zone);
            if (morgondagensPriser != null && !morgondagensPriser.isEmpty()) {
                valdaPriser.addAll(morgondagensPriser);
            }
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

        if (flags.containsKey("--charging")) {
            int hours = 0;
            String val = flags.get("--charging");
            if ("2h".equals(val)) hours = 2;
            if ("4h".equals(val)) hours = 4;
            if ("8h".equals(val)) hours = 8;

            if (hours > 0) {
                chargingWindow(valdaPriser, hours);
            }
        }
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

        // Bubbelsort
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

        // --- Utskrift i testformat ---
        System.out.println("\n--- Priser sorterade stigande ---");
        for (ElpriserAPI.Elpris p : priser) {

            String start = p.timeStart().format(DateTimeFormatter.ofPattern("HH"));
            String end = p.timeEnd().format(DateTimeFormatter.ofPattern("HH"));

            System.out.printf("%s-%s %s öre%n", start, end, df.format(p.sekPerKWh() * 100));
        }
        System.out.println("--------------------------------\n");
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
            System.out.printf("Påbörja laddning kl %02d:00 (%dh) - Medelpris för fönster: %s öre%n",
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
    }
}