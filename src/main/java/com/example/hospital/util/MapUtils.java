package com.example.hospital.util;

import com.google.protobuf.Timestamp;
import com.google.type.Date;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.val;

public class MapUtils {
  private MapUtils() {
  }

  public static LocalDateTime asLocalDateTime(Timestamp ts) {
    return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos())
        .atZone(ZoneOffset.UTC)
        .toLocalDateTime();
  }

  public static Timestamp asTimestamp(LocalDateTime ldt) {
    val instant = ldt.toInstant(ZoneOffset.UTC);
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

  public static Date asGrpcDate(LocalDate ld) {
    return Date.newBuilder()
        .setYear(ld.getYear())
        .setMonth(ld.getMonthValue())
        .setDay(ld.getDayOfMonth())
        .build();
  }

  public static LocalDate asLocalDate(Date d) {
    return LocalDate.of(d.getYear(), d.getMonth(), d.getDay());
  }

  public static int getIdFromBigInt(Map<String, Object> map) {
    return ((BigInteger) map.get("id")).intValue();
  }
}
