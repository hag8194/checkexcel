import com.google.gson.Gson;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Main {
  private static final int ID = 0;
  private static final int COUNTRY = 4;
  private static final int COUNTRY_LONG = 5;
  private static final int STATE = 6;
  private static final int STATE_LONG = 7;
  private static final int CITY = 10;
  private static final String EXCELPATH = "GeoLite2-City-Locations-en.xlsx";

  public static void main(String... args) {
    Supplier<Stream<Row>> streamSupplier = createStreamSupplier();

    Map<Country, Map<State, List<String>>> countriesStatesCities = streamSupplier.get()
        .filter(cells -> cells.getCell(COUNTRY_LONG) != null && cells.getCell(COUNTRY) != null)
        .map(cells -> new Country(cells.getCell(COUNTRY_LONG).getStringCellValue(), cells.getCell(COUNTRY).getStringCellValue()))
        .sorted(Comparator.comparing(Country::getCode))
        .distinct()
        .collect(Collectors.toMap(o -> o, o -> new HashMap<>()));

    Set<Country> countries = countriesStatesCities.keySet();

    deleteFilesIfExist();

    StringBuilder countriesSql = new StringBuilder("INSERT INTO COUNTRIES VALUES ");
    countries.forEach(country -> countriesSql.append("('").append(country.getCode()).append("', '").append(country.getName()).append("'),"));

    writeToFile("countries.sql", countriesSql);
    System.out.println("Created countries.sql");

    System.out.println("Creating map with all data");
    countries.forEach(country -> {
      List<State> states = streamSupplier.get()
          .filter(cells -> cells.getCell(STATE_LONG) != null && cells.getCell(COUNTRY).getStringCellValue().equals(country.getCode()))
          .map(cells -> cells.getCell(STATE_LONG).getStringCellValue())
          .distinct()
          .map(State::new)
          .collect(Collectors.toList());

      states.forEach(state -> countriesStatesCities.get(country).put(state, streamSupplier.get()
          .filter(cells -> cells.getCell(CITY) != null &&
              cells.getCell(CITY).getCellTypeEnum() == CellType.STRING &&
              cells.getCell(STATE_LONG) != null &&
              cells.getCell(STATE_LONG).getStringCellValue().equals(state.getName()))
          .map(cells -> convertToUtf8(cells.getCell(CITY).getStringCellValue())).collect(Collectors.toList())));
    });
    System.out.println("Created map with all data");

    countriesStatesCities.forEach((country, stateListMap) -> {
      System.out.println("Creating inserts for " + country);
      StringBuilder statesSql = new StringBuilder("INSERT INTO STATES VALUES ");

      stateListMap.forEach((state, cities) -> {
        statesSql.append("(").append(state.getId()).append(",'")
          .append(state.getName()).append("',").append("'").append(country.getCode()).append("'),");

        System.out.println("Creating inserts for " + state);
        StringBuilder citiesSql = new StringBuilder("INSERT INTO CITIES VALUES ");
        cities.forEach(s -> citiesSql.append("('").append(s).append("',").append(state.getId()).append("),"));

        writeToFile("cities.sql", citiesSql);
        System.out.println("Appended data to cities.sql");
      });

      writeToFile("states.sql", statesSql);
      System.out.println("Appended data to states.sql");
    });


    try {
      System.out.println("Creating output.json");
      Files.write(Paths.get("output.json"), new Gson().toJson(countriesStatesCities).getBytes());
      System.out.println("Created output.json");
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private static String convertToUtf8(String stringToConvert) {
    return stringToConvert != null && !stringToConvert.isEmpty() ?
        replaceAllSpecialCharacter(new String(stringToConvert.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)): stringToConvert;
  }

  private static String replaceAllSpecialCharacter(String string) {
    return string.replaceAll("'", "").replaceAll("\"", "");
  }

  private static void writeToFile(String filePath, StringBuilder sql) {
    try {
      Path path = Paths.get(filePath);
      if(!Files.exists(path))
        Files.createFile(path);
      if(sql.toString().endsWith("VALUES "))
        return;
      Files.write(path, sql.substring(0, sql.length() - 1).concat(";\n").getBytes(), StandardOpenOption.APPEND);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void deleteFilesIfExist() {
    try {
      Files.deleteIfExists(Paths.get("countries.sql"));
      Files.deleteIfExists(Paths.get("states.sql"));
      Files.deleteIfExists(Paths.get("cities.sql"));
      Files.deleteIfExists(Paths.get("output.json"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Supplier<Stream<Row>> createStreamSupplier() {
    try {
      FileInputStream fileInputStream = new FileInputStream(new File(EXCELPATH));
      Workbook workbook = new XSSFWorkbook(fileInputStream);
      return () -> StreamSupport.stream(workbook.getSheetAt(0).spliterator(), false);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return () -> null;
  }

  private static final class Country {
    private String name;
    private String code;

    Country(String name, String code) {
      this.name = convertToUtf8(name);
      this.code = convertToUtf8(code);
    }

    String getName() {
      return name;
    }

    String getCode() {
      return code;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Country country = (Country) o;
      return Objects.equals(name, country.name) &&
          Objects.equals(code, country.code);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, code);
    }

    @Override
    public String toString() {
      return "name:" + name + ",code:" + code;
    }
  }

  private static final class State {
    private static long idCounter = 0;
    private long id;
    private String name;

    State(String name) {
      this.id = ++idCounter;
      this.name = convertToUtf8(name);
    }

    long getId() {
      return id;
    }

    String getName() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      State state = (State) o;
      return Objects.equals(name, state.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }

    @Override
    public String toString() {
      return "id: " + id + ", name:" + name;
    }
  }
}
