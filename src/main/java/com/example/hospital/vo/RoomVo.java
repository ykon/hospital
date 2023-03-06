package com.example.hospital.vo;

import static com.example.hospital.util.MapUtils.asTimestamp;

import com.example.hospital.grpc.Room;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomVo {

  private Integer id;
  private String number;
  private LocalDateTime updatedAt;
  private LocalDateTime createdAt;

  public Room asRoom() {
    return Room.newBuilder()
        .setId(id)
        .setNumber(number)
        .setUpdatedTime(asTimestamp(updatedAt))
        .setCreatedTime(asTimestamp(createdAt))
        .build();
  }
}
