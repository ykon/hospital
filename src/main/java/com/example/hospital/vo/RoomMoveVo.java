package com.example.hospital.vo;

import static com.example.hospital.util.MapUtils.asGrpcDate;
import static com.example.hospital.util.MapUtils.asTimestamp;

import com.example.hospital.grpc.RoomMove;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomMoveVo {

  private Integer id;
  private Integer admissionId;
  private Integer fromRoomId;
  private Integer toRoomId;
  private LocalDate moveDate;
  private LocalDateTime updatedAt;
  private LocalDateTime createdAt;

  public RoomMove asRoomMove() {
    return RoomMove.newBuilder()
        .setId(id)
        .setAdmissionId(admissionId)
        .setFromRoomId(fromRoomId)
        .setToRoomId(toRoomId)
        .setMoveDate(asGrpcDate(moveDate))
        .setUpdatedTime(asTimestamp(updatedAt))
        .setCreatedTime(asTimestamp(createdAt))
        .build();
  }
}
