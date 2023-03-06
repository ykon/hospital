package com.example.hospital.vo;

import static com.example.hospital.util.MapUtils.asGrpcDate;
import static com.example.hospital.util.MapUtils.asTimestamp;

import com.example.hospital.grpc.Admission;
import com.example.hospital.util.MapUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

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
    val builder = Admission.newBuilder()
        .setId(id)
        .setPatientId(patientId)
        .setStartDate(asGrpcDate(startDate))
        .setUpdatedTime(asTimestamp(updatedAt))
        .setCreatedTime(asTimestamp(createdAt));

    Optional.ofNullable(endDate).map(MapUtils::asGrpcDate).ifPresent(builder::setEndDate);

    return builder.build();
  }
}
