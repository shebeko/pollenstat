package com.eitkey;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.util.stream.Collectors.*;

public class PollenStat {

    private final List<PollenPerDay> data = new ArrayList<>();
    private final String city;
    private final static String DIR = "data";
    private final static String DEFAULT_DATE_FORMAT = "dd/MM/yyyy";
    private final static String NOT_COUNTED_SYMBOL = "-";
    private final static String UNKNOWN_CITY = "unknown city";
    private DateTimeFormatter dateFormatter;

    public static enum PollenLevel {
        LOW(1, 9), MEDIUM (10,99),
        HIGH (100, 999), VERY_HIGH(1000, 9999),
        SUPER_HIGH(10000, 50000), NOTHING(0, 0);

        private final int lower;
        private final int upper;

        PollenLevel(int lower, int upper) {
            this.lower = lower;
            this.upper = upper;
        }

        public int getLowerLevel() {
            return lower;
        }

        public int getUpperLevel() {
            return upper;
        }

        @Override
        public String toString() {
            return String.format("    [Уровень пыльцы %s : (%d - %d)]", super.toString(), lower, upper);
        }
    }

    private PollenStat(String city, String dateFormat) {
        this.city = city;
        setDateFormat(dateFormat);
    }

    private PollenStat(String city) {
        this.city = city;
        setDateFormat(DEFAULT_DATE_FORMAT);
    }

    private void setDateFormat(String format) {
        dateFormatter = DateTimeFormatter.ofPattern(format);
    }

    public static PollenStat loadStat(String filename) throws IOException {
        String city;
        Path path = Paths.get(DIR, filename);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
          city = reader.readLine();
        }
        if (city == null) {
              return new PollenStat(UNKNOWN_CITY);
        }
        PollenStat stat =  city.isBlank() ? new PollenStat(UNKNOWN_CITY) : new PollenStat(city);
        try (Stream<String> lines = Files.lines(path);) {
            lines.skip(1).forEach(st -> {
                String[] args = st.split("\\s");
                if (args.length <= 1) throw new IllegalFileFormatException("Wrong file format:  " + path);
                LocalDate date = LocalDate.parse(args[0], stat.dateFormatter);
                int amount = 0;
                boolean counted = true;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    if (args[1].equals(NOT_COUNTED_SYMBOL)) {
                        counted = false;
                    } else {
                        throw new IllegalFileFormatException("Corrupt data in file " + path + " : " + st);
                    }
                }
                PollenPerDay ppday = new PollenPerDay(date, amount, counted);
                stat.data.add(ppday);
            });
        }

        return stat;
    }

    public String getCity() {
        return city;
    }

    public Optional<Integer> getYear() {
        if (data.size() != 0) {
            return Optional.of(data.get(0).getDate().getYear());
        }
        return Optional.empty();
    }

    public Map<PollenLevel, List<PollenPerDay>> daysByLevel() {
        return data.stream().filter(PollenPerDay::isCounted).collect(Collectors.groupingBy(
                ppday -> {
                    if (ppday.getAmount() >= PollenLevel.LOW.getLowerLevel() &&
                            ppday.getAmount() <= PollenLevel.LOW.getUpperLevel()) return PollenLevel.LOW;
                    else if (ppday.getAmount() > PollenLevel.LOW.getUpperLevel() &&
                            ppday.getAmount() <= PollenLevel.MEDIUM.getUpperLevel()) return PollenLevel.MEDIUM;
                    else if (ppday.getAmount() > PollenLevel.MEDIUM.getUpperLevel() &&
                            ppday.getAmount() <= PollenLevel.HIGH.getUpperLevel()) return PollenLevel.HIGH;
                    else if (ppday.getAmount() > PollenLevel.HIGH.getUpperLevel() &&
                            ppday.getAmount() <= PollenLevel.VERY_HIGH.getUpperLevel()) return PollenLevel.VERY_HIGH;
                    else if (ppday.getAmount() > PollenLevel.VERY_HIGH.getUpperLevel()) return PollenLevel.SUPER_HIGH; else
                        return PollenLevel.NOTHING;

                }
        ));
    }

    public Optional<PollenPerDay> getPeak() {
        return data.stream().collect(Collectors.maxBy(Comparator.comparing(PollenPerDay::getAmount)));
    }

    public long blossomDays() {
        return data.stream().filter(ppd -> ppd.getAmount() > 0 && ppd.isCounted() ).count() + daysNotCounted();
    }

    public long daysNotCounted() {
        return data.stream().filter(((Predicate<PollenPerDay>)PollenPerDay::isCounted).negate()).count();
    }

    public List<PollenPerDay> datesNotCounted() {
        return data.stream().filter(((Predicate<PollenPerDay>)PollenPerDay::isCounted).negate())
                .collect(toList());
    }

    public Optional<PollenPerDay> startOfBlossom() {
        return data.stream().dropWhile(ppd -> ppd.getAmount() == 0).findFirst();
    }

    public Optional<PollenPerDay> endOfBlossom() {
        return data.stream().sorted(Comparator.comparing(PollenPerDay::getDate).reversed()).dropWhile(ppd -> ppd.getAmount() == 0 && ppd.isCounted()).findFirst();
    }

    public int totalAmount() {
        return data.stream().mapToInt(PollenPerDay::getAmount).sum();
    }
}