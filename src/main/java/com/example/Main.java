package com.example;

import com.example.api.ElpriserAPI;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    // Framtida fix
    //private static final DateTimeFormatter HH_FORMAT = DateTimeFormatter("HH");


    public static void main(String[] args) {

        LocalDate idag = LocalDate.now();
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        if (args.length == 0) {

            printHelp();
            return;
        } else{

            for(int i = 0; i < args.length; i++){
                switch(args[i].toLowerCase()){
                    case "--help" -> {
                        printHelp();
                        return;
                    }
                    case "--zone" -> {}
                    case  "--date" -> {}
                    case "--sorted" -> {}
                }
            }
        }

    }


    /// Hjälpmeny ///

    private static void printHelp() {

        System.out.println("--zone SE1|SE2|SE3|SE4 (required)");
        System.out.println("--date YYYY-MM-DD (optional, defaults to current date)");
        System.out.println("--sorted (optional, to display prices in descending order)");
        System.out.println("--charging 2h|4h|8h (optional, to find optimal charging windows)");
        System.out.println("--help (optional, to display usage information)");
    }

}


    

