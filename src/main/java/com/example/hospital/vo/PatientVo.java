package com.example.hospital.vo;

import static com.example.hospital.util.MapUtils.asTimestamp;

import com.example.hospital.grpc.Patient;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatientVo {

  private Integer id;
  private String name;
  private LocalDateTime updatedAt;
  private LocalDateTime createdAt;

  public Patient asPatient() {
    return Patient.newBuilder()
        .setId(id)
        .setName(name)
        .setUpdatedTime(asTimestamp(updatedAt))
        .build();
  }
}
