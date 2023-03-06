package com.example.hospital.vo;

import static com.example.hospital.util.MapUtils.asGrpcDate;

import com.example.hospital.grpc.RoomDuration;
import com.example.hospital.util.MapUtils;
import java.time.LocalDate;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

@AllArgsConstructor
@Getter
@Setter
public class RoomDurationVo implements Duration {

  private Integer roomId;
  private LocalDate startDate;
  private LocalDate endDate;

  public RoomDuration asRoomDuration() {
    val builder = RoomDuration.newBuilder()
        .setRoomId(roomId)
        .setStartDate(asGrpcDate(startDate));

    Optional.ofNullable(endDate).map(MapUtils::asGrpcDate).ifPresent(builder::setEndDate);

    return builder.build();
  }
}
