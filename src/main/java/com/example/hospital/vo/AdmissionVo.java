package com.example.hospital.vo;

import static com.example.hospital.util.MapUtils.asGrpcDate;
import static com.example.hospital.util.MapUtils.asTimestamp;

import com.example.hospital.grpc.Admission;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdmissionVo implements Duration {

  private Integer id;
  private Integer patientId;
  private Integer roomId;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDateTime updatedAt;
  private LocalDateTime createdAt;

  public Admission asAdmission() {
    return Admission.newBuilder()
        .setId(id)
        .setPatientId(patientId)
        .setStartDate(asGrpcDate(startDate))
        .setEndDate(asGrpcDate(endDate))
        .setUpdatedTime(asTimestamp(updatedAt))
        .setCreatedTime(asTimestamp(createdAt))
        .build();
  }
}
