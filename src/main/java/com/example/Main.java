package com.example;

import com.example.api.ElpriserAPI;

import java.sql.SQLOutput;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;

public class Main {

    public static final Locale SWEDISH = new Locale("sv", "SE");
    public static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH");

    static LocalDate date = LocalDate.now();
    static ElpriserAPI elpriser = new ElpriserAPI();

    public static void main(String[] args) {

        boolean skaViSortera = false;
        String zon = "";


            if (args.length == 0) { // Skriv ut hjälpmeny vid tom inmatning
                printHelp();
                return;
            } else {

                try {

                    for (int i = 0; i < args.length; i++) {
                        switch (args[i].toLowerCase()) {
                            case "--help":
                                printHelp();
                                return;

                            case "--zone":
                                String valdZon = args[++i].toUpperCase();
                                        if (valdZon.equals("SE1")
                                        || valdZon.equals("SE2")
                                        || valdZon.equals("SE3")
                                        || valdZon.equals("SE4")) {
                                            zon = valdZon;

                                        }

                                 else {
                                    throw new IllegalArgumentException("invalid zone");
                                }
                                break;

                            case "--date":
                                String valdDag = args[++i].trim();
                                if (valdDag.matches("\\d{4}-\\d{2}-\\d{2}")) {
                                    date = LocalDate.parse(LocalDate.parse(valdDag, DateTimeFormatter.ISO_LOCAL_DATE).toString());
                                } else {
                                    throw new IllegalArgumentException("invalid date");
                                }
                                break;

                            case "--sorted":
                                skaViSortera = true;
                                break;

                            case "--charging":
                                String valdLaddning = args[++i].trim();
                                int laddTid = Integer.parseInt(valdLaddning);
                                break;

                            default:
                                throw new IllegalArgumentException("invalid argument");
                        }
                    }

                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }


            }

        if (zon.isEmpty()){
            zon = "SE1";
            System.out.println("Zone is required");
            printHelp();
        }



        // För att få rätt zon
        ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zon);
        // Lista på priser från rätt zon och valt datum(Dagens om ej valt)
        List<ElpriserAPI.Elpris> dagensPriser = elpriser.getPriser(date, prisklass);
        // Morgondagens priser från valt datum
        List<ElpriserAPI.Elpris> morgondagensPriser = elpriser.getPriser(date.plusDays(1), prisklass);



        if(skaViSortera) {
            sorteradLista(dagensPriser);
        }else {
            skrivaUtPriser(dagensPriser);
        }
    }

     ///////////////////////////////////////////////////////////////////////////////////

    /// Utskrift av prislista från valt datum(dagens om ej valt --date) ///

    public static void skrivaUtPriser(List<ElpriserAPI.Elpris> dagensPriser) {

        //Medelpris
        double meanPrice = 0;
        for (ElpriserAPI.Elpris elpris : dagensPriser) {
            meanPrice += elpris.sekPerKWh();
        }
        double mediumPrice = meanPrice / dagensPriser.size() * 100;
        System.out.printf(SWEDISH,"Medelpris: %.2f ÖrePerKwh %n", mediumPrice);

        //Lägsta pris
        ElpriserAPI.Elpris min = dagensPriser.getFirst();
        for (int i = 1; i < dagensPriser.size(); i++) {
            if (dagensPriser.get(i).sekPerKWh() < min.sekPerKWh()) {
                min = dagensPriser.get(i);
            }
        }
        double minimum = min.sekPerKWh() * 100;
        System.out.printf(SWEDISH,"Det lägsta priset är: %.2f öreKwh mellan kl: %s-%s %n",minimum, min.timeStart().format(timeFormat), min.timeEnd().format(timeFormat));

        //Högsta pris
        ElpriserAPI.Elpris max = dagensPriser.getFirst();
        for (int i = 1; i < dagensPriser.size(); i++) {
            if (dagensPriser.get(i).sekPerKWh() > max.sekPerKWh()) {
                max = dagensPriser.get(i);
            }
        }
        double maximum = max.sekPerKWh() * 100;
        System.out.printf(SWEDISH,"Det högsta priset är: %.2f öreKwh mellan kl: %s-%s %n",maximum, max.timeStart().format(timeFormat), max.timeEnd().format(timeFormat));



    }

    /// Osorterad Lista ///

    public static void osorteradLista(List<ElpriserAPI.Elpris> dagensPriser){

        System.out.println("----Priser på valt datum----");

        for(ElpriserAPI.Elpris prisData : dagensPriser){
            String startTim = prisData.timeStart().format(timeFormat);
            String slutTim = prisData.timeEnd().format(timeFormat);

            double prisSekPerKwh = prisData.sekPerKWh();
            double prisOrePerKwh = prisSekPerKwh * 100;



            System.out.printf(SWEDISH,"Klockan: %4s - %4s Pris: %.2f ÖrePerKwh %n", startTim, slutTim, prisOrePerKwh );

        }

    }

    /// Sorterad Lista ///

    public static void sorteradLista(List<ElpriserAPI.Elpris> dagensPriser) {

            if (dagensPriser.isEmpty())
                System.out.println("Inga elpriser tillgängliga...");

                dagensPriser.sort(Comparator.comparing((ElpriserAPI.Elpris::sekPerKWh)));

                for (ElpriserAPI.Elpris prisdata : dagensPriser) {
                    double pris = prisdata.sekPerKWh() * 100;
                    String startTim = prisdata.timeStart().format(timeFormat);
                    String slutTim = prisdata.timeEnd().format(timeFormat);

                    System.out.printf(SWEDISH,"%s-%s %.2f öre %n", startTim, slutTim, pris );
                }
    }














    ///  Charging ///



    /// Hjälpmeny ///

    private static void printHelp() {

        System.out.println("--zone SE1|SE2|SE3|SE4 (required)");
        System.out.println("--date YYYY-MM-DD (optional, defaults to current date)");
        System.out.println("--sorted (optional, to display prices in descending order)");
        System.out.println("--charging 2h|4h|8h (optional, to find optimal charging windows)");
        System.out.println("--help (optional, to display usage information)");
    }

}




    

