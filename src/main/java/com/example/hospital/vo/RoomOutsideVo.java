package com.example.hospital.vo;

import static com.example.hospital.util.MapUtils.asGrpcDate;
import static com.example.hospital.util.MapUtils.asTimestamp;

import com.example.hospital.grpc.RoomOutside;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomOutsideVo implements Duration {

  private Integer id;
  private Integer admissionId;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDateTime updatedAt;
  private LocalDateTime createdAt;

  public RoomOutside asRoomOutside() {
    return RoomOutside.newBuilder()
        .setId(id)
        .setStartDate(asGrpcDate(startDate))
        .setEndDate(asGrpcDate(endDate))
        .setUpdatedTime(asTimestamp(updatedAt))
        .setCreatedTime(asTimestamp(createdAt))
        .build();
  }
}
