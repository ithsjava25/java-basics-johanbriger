package com.example;

import com.example.api.ElpriserAPI;

import javax.xml.transform.Source;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class Main {

    // Framtida fix
    //private static final DateTimeFormatter HH_FORMAT = DateTimeFormatter("HH");


    public static void main(String[] args) {


        ElpriserAPI elpriserAPI = new ElpriserAPI();
        ElpriserAPI.Prisklass myZone = ElpriserAPI.Prisklass.SE3;

        List<ElpriserAPI.Elpris> todayPrices = priceToday(elpriserAPI, myZone);
        List<ElpriserAPI.Elpris> priceTomorrow = priceTomorrow(elpriserAPI, myZone);

        //Front
        Scanner scanner = new Scanner(System.in);
        System.out.println("Vad vill du?");
        String villDu = scanner.next();

        if (villDu.equals("help")){
            printHelp();
        }

        if (villDu.equals("--prisIdag")){
            System.out.println("Dagens snittpris är " + meanPrice(todayPrices));
        }

        if (villDu.equals("--prisImorgon")){
            System.out.println("Morgondagens snittpris är " + meanPrice(priceTomorrow));
        }


        if(villDu.equals("--dagensPriser")) {
            for (ElpriserAPI.Elpris elpris : todayPrices) {
                System.out.printf("Mellan %s - %s är priset %.3f öre/kWh i %s%n", elpris.timeStart(), elpris.timeEnd(), elpris.sekPerKWh(), myZone);
            }
        }

        if (villDu.equals("--morgonDagensPriser")){
            for (ElpriserAPI.Elpris elpris : priceTomorrow) {
                System.out.println("Mellan " + elpris.timeStart() + " - " + elpris.timeEnd() + " är priset " + elpris.sekPerKWh() + " öre/kWh i " + myZone);
            }
        }

        if (villDu.equals("--dyrastePriset")) {
            System.out.println("Dagens dyraste timme är " + maxPrice(todayPrices));
        }

        if (villDu.equals("--dagensBilligaste")){
            System.out.println("Dagens billigaste timme är " + minPrice(todayPrices));
        }



    }

    // METODER



    // Dagens pris
    private static List<ElpriserAPI.Elpris> priceToday(ElpriserAPI elpriserAPI, ElpriserAPI.Prisklass zon) {
        return elpriserAPI.getPriser(LocalDate.now(), zon);
    }

    // Morgondagens pris
    private static List<ElpriserAPI.Elpris> priceTomorrow(ElpriserAPI elpriserAPI, ElpriserAPI.Prisklass zon) {
        return elpriserAPI.getPriser(LocalDate.now().plusDays(1), zon);
    }

    // Dagens snittpris
    private static double meanPrice(List<ElpriserAPI.Elpris> elpriser) {
        double sum = 0.0;
        for (int i = 0; i < elpriser.size(); i++) {
            sum += elpriser.get(i).sekPerKWh();
        }
        return sum / elpriser.size();
    }

    //find and return element with lowest price in provided list
    //sets lowest price to the first element in the list and then compares the rest
    //if multiple elements have the lowest price
    private static ElpriserAPI.Elpris minPrice(List<ElpriserAPI.Elpris> elpriser) {
        ElpriserAPI.Elpris min = elpriser.getFirst();
        for (int i = 1; i < elpriser.size(); i++) {
            if (elpriser.get(i).sekPerKWh() < min.sekPerKWh()) {
                min = elpriser.get(i);
            }
        }
        return min;
    }

    //find and return element with highest price in provided list
    //sets highest price to the first element in the list and then compares the rest
    //if multiple elements have the highest price return the first of them
    private static ElpriserAPI.Elpris maxPrice(List<ElpriserAPI.Elpris> elpriser) {
        ElpriserAPI.Elpris max = elpriser.getFirst();
        for (int i = 1; i < elpriser.size(); i++) {
            if (elpriser.get(i).sekPerKWh() > max.sekPerKWh()) {
                max = elpriser.get(i);
            }
        }
        return max;
    }

    private static void printHelp() {

        System.out.println("--zone SE1|SE2|SE3|SE4 (required)");
        System.out.println("--date YYYY-MM-DD (optional, defaults to current date)");
        System.out.println("--sorted (optional, to display prices in descending order)");
        System.out.println("--charging 2h|4h|8h (optional, to find optimal charging windows)");
        System.out.println("--help (optional, to display usage information)");
    }


}
