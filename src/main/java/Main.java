import com.google.gson.Gson;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  private static final String EXCELPATH = "C:\\Users\\Hugo\\Desktop\\aerovuelos\\checkexcel\\src\\main\\resources\\GeoLite2-City-Locations-en.xlsx";

  public static void main(String... args) {
    Supplier<Stream<Row>> streamSupplier = createStreamSupplier();

    Map<Country, Map<String, List<String>>> countriesStatesCities = streamSupplier.get()
        .filter(cells -> cells.getCell(COUNTRY_LONG) != null && cells.getCell(COUNTRY) != null)
        .map(cells -> new Country(cells.getCell(COUNTRY_LONG).getStringCellValue(), cells.getCell(COUNTRY).getStringCellValue()))
        .sorted(Comparator.comparing(Country::getCode))
        .distinct()
        .collect(Collectors.toMap(o -> o, o -> new HashMap<>()));

    Set<Country> countries = countriesStatesCities.keySet();

    countries.forEach(country -> {
      List<String> states = streamSupplier.get()
          .filter(cells -> cells.getCell(STATE_LONG) != null && cells.getCell(COUNTRY).getStringCellValue().equals(country.getCode()))
          .map(cells -> cells.getCell(STATE_LONG).getStringCellValue())
          .distinct()
          .collect(Collectors.toList());

      states.forEach(state -> countriesStatesCities.get(country).put(state, streamSupplier.get()
          .filter(cells -> cells.getCell(CITY) != null &&
              cells.getCell(CITY).getCellTypeEnum() == CellType.STRING &&
              cells.getCell(STATE_LONG) != null &&
              cells.getCell(STATE_LONG).getStringCellValue().equals(state))
          .map(cells -> cells.getCell(CITY).getStringCellValue()).collect(Collectors.toList())));
    });

    try {
      Files.write(Paths.get("output.txt"), new Gson().toJson(countriesStatesCities).getBytes());
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

  public static class Country {
    private String name;
    private String code;

    public Country(String name, String code) {
      this.name = name;
      this.code = code;
    }

    public String getName() {
      return name;
    }

    public String getCode() {
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
}
