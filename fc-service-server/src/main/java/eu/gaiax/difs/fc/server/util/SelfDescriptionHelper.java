package eu.gaiax.difs.fc.server.util;

import eu.gaiax.difs.fc.core.exception.ClientException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import javax.validation.constraints.NotNull;

public class SelfDescriptionHelper {
  public static String[] parseTimeRange(@NotNull String timeRange) {
    if (timeRange != null && timeRange.contains("/")) {
      String[] timeRanges = timeRange.split("/");
      {
        if (timeRanges.length == 2) {
          try {
            Instant.parse(timeRanges[0]);
            Instant.parse(timeRanges[1]);
          } catch (DateTimeParseException exception) {
            throw new ClientException("Please check the format of the time range parameters specified for SD filter!");
          }
          return timeRanges;
        }
      }
    }
    throw new ClientException("Please check the value of the time range parameter specified for SD filter!");
  }
}
