package com.eitkey;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.eitkey.PollenStat.PollenLevel.*;

public class Main {
    private static int counter = 1;
    private final static DateTimeFormatter dformatter;
    private final static String DATES_DELIMITER;
    private final static String NO_DATA;
    private final static Map<DayOfWeek, String> dow;

    static {
        dformatter = DateTimeFormatter.ofPattern("dd MMM");
        dow = daysOfWeekMap();
        DATES_DELIMITER = " - ";
        NO_DATA = "НЕТ ДАННЫХ";
    }

    public static void main(String[] args) throws IOException {
//      PollenStat pstat = PollenStat.loadStat("Moscow_birch_2020");
        PollenStat pstat = PollenStat.loadStat("Moscow_birch_2021");
//      PollenStat pstat = PollenStat.loadStat("Moscow_birch_2019");
        pstat.getYear().ifPresentOrElse(
                (year) -> {
                    System.out.format("Статистика по пыльце берёзы в %s в %d году:", pstat.getCity(), year);
                    System.out.println();
                    System.out.println("-------------------------------------------------");
                }, () -> {
                    System.out.format("Статистики по пыльце берёзы в %s нет", pstat.getCity());
                    System.exit(-1);
                }
        );
        PollenPerDay start = pstat.startOfBlossom().
                orElseThrow(()-> new IllegalFileFormatException("No data about pollen concentration found"));
        PollenPerDay end = pstat.endOfBlossom().get();
        System.out.format("(%d) Цветение продолжалось дней: %d. Начало: %s[%d] Окончание: %s[%s] %n", counter++, pstat.blossomDays(),
                start.getDate().format(dformatter), start.getAmount(), end.getDate().format(dformatter),
                end.getAmount() > 0 ? end.getAmount() : NO_DATA);
        pstat.getPeak().ifPresent(peak ->
                System.out.format("(%d) Пиковый показатель: %s [%d] %n", counter++, peak.getDate().format(dformatter), peak.getAmount()));
        System.out.format("(%d) Суммарное количество зёрен: %d%n", counter++, pstat.totalAmount());
        printDaysByLevel(pstat);
    }

    private static void printDaysByLevel(PollenStat pstat) {
        Map<PollenStat.PollenLevel, List<PollenPerDay>> dbl = pstat.daysByLevel();
        PollenStat.PollenLevel[] pollenLevels = {LOW, MEDIUM, HIGH, VERY_HIGH, SUPER_HIGH};
        System.out.format("(%d) Дни по уровням концентрации пыльцы:%n", counter);
        Stream.of(pollenLevels).forEachOrdered(lvl -> {
            System.out.println();
            System.out.println(lvl);
            List<PollenPerDay> days = dbl.get(lvl);
            if (days == null) days = Collections.emptyList();
            System.out.format("    Дней: %d (", days.size());
            String output = days.stream().map(ppd -> String.format("%s[%d]", ppd.getDate().format(dformatter), ppd.getAmount()))
                    .collect(Collectors.joining(DATES_DELIMITER));
            IntSummaryStatistics lvlstat = days.stream().mapToInt(PollenPerDay::getAmount).summaryStatistics();
            System.out.format("%s)%n", output);
            double percentage = (double)lvlstat.getSum() / pstat.totalAmount();
            System.out.format("    Статистика уровня: суммарное количество зёрен пыльцы = %d, доля в общем объёме = %.4f" +
                            ", среднее значение = %.2f %n", lvlstat.getSum(), percentage, lvlstat.getAverage());
        });
        System.out.println();
        System.out.println("    [Уровень пыльцы неизвестен: ???]");
        System.out.format("    Дней (когда не было измерений, но происходило пыление): %d (", pstat.daysNotCounted());
        Stream<String> datesNotCounted = pstat.datesNotCounted().stream().map(ppd ->String.format("%s(%s)", ppd.getDate().format(dformatter),
                dow.get(ppd.getDate().getDayOfWeek())));
        System.out.format("%s)%n", formString(datesNotCounted.collect(Collectors.toList()), 5));
    }

    private static Map<DayOfWeek, String> daysOfWeekMap() {
            Map<DayOfWeek, String> map = new EnumMap(Map.of(DayOfWeek.MONDAY, "понедельник",
                    DayOfWeek.TUESDAY, "вторник",
                    DayOfWeek.WEDNESDAY, "среда",
                    DayOfWeek.THURSDAY, "четверг",
                    DayOfWeek.FRIDAY, "пятница",
                    DayOfWeek.SATURDAY, "суббота",
                    DayOfWeek.SUNDAY, "воскресенье"));
            return map;
    }

    private static String formString(List<String> itemsList, int n) {
        if (itemsList.isEmpty()) return "";
        final StringBuilder result = new StringBuilder();
        final String delimiter = " - ";
        final int length = itemsList.size();
        final int num = length / n;
        for (int i = 0; i <= num ; i++) {
            final StringJoiner stJoiner = new StringJoiner(delimiter);
            int endIndx = i <= num - 1 ? (i + 1) * n : length;
            itemsList.subList(i * n, endIndx).forEach(stJoiner::add);
            result.append(stJoiner);
            if ((i != num) && ((i+1)*n != length)) {
                result.append(delimiter).append(System.lineSeparator());
            }
        }
        return result.toString();
    }
}