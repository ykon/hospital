package com.example.hospital.controller;

import static com.example.hospital.util.MapUtils.asLocalDate;
import static com.example.hospital.util.MapUtils.asLocalDateTime;

import com.example.hospital.grpc.Admission;
import com.example.hospital.grpc.CreateAdmissionRequest;
import com.example.hospital.grpc.CreateAdmissionResponse;
import com.example.hospital.grpc.CreatePatientRequest;
import com.example.hospital.grpc.CreatePatientResponse;
import com.example.hospital.grpc.CreateRoomMoveRequest;
import com.example.hospital.grpc.CreateRoomMoveResponse;
import com.example.hospital.grpc.CreateRoomOutsideRequest;
import com.example.hospital.grpc.CreateRoomOutsideResponse;
import com.example.hospital.grpc.CreateRoomRequest;
import com.example.hospital.grpc.CreateRoomResponse;
import com.example.hospital.grpc.DeleteAdmissionRequest;
import com.example.hospital.grpc.DeletePatientRequest;
import com.example.hospital.grpc.DeleteRoomMoveRequest;
import com.example.hospital.grpc.DeleteRoomOutsideRequest;
import com.example.hospital.grpc.DeleteRoomRequest;
import com.example.hospital.grpc.GetAdmissionsRequest;
import com.example.hospital.grpc.GetAdmissionsResponse;
import com.example.hospital.grpc.GetPatientsRequest;
import com.example.hospital.grpc.GetPatientsResponse;
import com.example.hospital.grpc.GetRoomDurationsRequest;
import com.example.hospital.grpc.GetRoomDurationsResponse;
import com.example.hospital.grpc.GetRoomMovesRequest;
import com.example.hospital.grpc.GetRoomMovesResponse;
import com.example.hospital.grpc.GetRoomOutsidesRequest;
import com.example.hospital.grpc.GetRoomOutsidesResponse;
import com.example.hospital.grpc.GetRoomsRequest;
import com.example.hospital.grpc.GetRoomsResponse;
import com.example.hospital.grpc.HospitalGrpc;
import com.example.hospital.grpc.Patient;
import com.example.hospital.grpc.Room;
import com.example.hospital.grpc.RoomDuration;
import com.example.hospital.grpc.RoomMove;
import com.example.hospital.grpc.RoomOutside;
import com.example.hospital.grpc.UpdateAdmissionRequest;
import com.example.hospital.grpc.UpdatePatientRequest;
import com.example.hospital.grpc.UpdateRoomMoveRequest;
import com.example.hospital.grpc.UpdateRoomOutsideRequest;
import com.example.hospital.grpc.UpdateRoomRequest;
import com.example.hospital.service.HospitalService;
import com.example.hospital.util.MapUtils;
import com.example.hospital.vo.AdmissionVo;
import com.example.hospital.vo.PatientVo;
import com.example.hospital.vo.RoomDurationVo;
import com.example.hospital.vo.RoomMoveVo;
import com.example.hospital.vo.RoomOutsideVo;
import com.example.hospital.vo.RoomVo;
import com.google.protobuf.Empty;
import com.google.type.Date;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.util.StringUtils;

@GRpcService
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class HospitalController extends HospitalGrpc.HospitalImplBase {

  private final HospitalService service;

  private static StatusRuntimeException makeGrpcException(Status status, Exception e) {
    return status
        .withDescription(e.toString())
        .withCause(e)
        .asRuntimeException();
  }

  private static <V> void callService(StreamObserver<V> responseObserver, Supplier<V> func) {
    try {
      responseObserver.onNext(func.get());
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      responseObserver.onError(makeGrpcException(Status.INVALID_ARGUMENT, e));
    } catch (IllegalStateException e) {
      responseObserver.onError(makeGrpcException(Status.ABORTED, e));
    } catch (NoSuchElementException e) {
      responseObserver.onError(makeGrpcException(Status.NOT_FOUND, e));
    } catch (Exception e) {
      //e.printStackTrace();
      responseObserver.onError(makeGrpcException(Status.INTERNAL, e));
    }
  }

  private static void throwIfEmpty(String value, String msg) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(msg);
    }
  }

  // 病室作成
  @Override
  public void createRoom(CreateRoomRequest request, StreamObserver<CreateRoomResponse> responseObserver) {
    callService(responseObserver, () -> {
      throwIfEmpty(request.getNumber(), "number is empty.");

      val id = service.createRoom(request.getNumber());
      return CreateRoomResponse.newBuilder().setId(id).build();
    });
  }

  // 病室更新
  @Override
  public void updateRoom(UpdateRoomRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      throwIfEmpty(request.getNumber(), "number is empty.");

      service.updateRoom(request.getId(), request.getNumber(), asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  // 病室削除
  @Override
  public void deleteRoom(DeleteRoomRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      service.deleteRoom(request.getId(), asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  private static List<Room> asRoomList(List<RoomVo> maps) {
    return maps.stream()
        .map(RoomVo::asRoom)
        .toList();
  }

  // 病室取得
  @Override
  public void getRooms(GetRoomsRequest request, StreamObserver<GetRoomsResponse> responseObserver) {
    callService(responseObserver, () -> {
      return GetRoomsResponse.newBuilder()
          .addAllRooms(asRoomList(service.getRooms()))
          .build();
    });
  }

  // 患者作成
  @Override
  public void createPatient(CreatePatientRequest request, StreamObserver<CreatePatientResponse> responseObserver) {
    callService(responseObserver, () -> {
      throwIfEmpty(request.getName(), "name is empty.");

      val id = service.createPatient(request.getName());
      return CreatePatientResponse.newBuilder().setId(id).build();
    });
  }

  // 患者更新
  @Override
  public void updatePatient(UpdatePatientRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      throwIfEmpty(request.getName(), "name is empty.");

      service.udpatePatient(request.getId(), request.getName(), asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  // 患者削除
  @Override
  public void deletePatient(DeletePatientRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      service.deletePatient(request.getId(), asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  private static List<Patient> asPatientList(List<PatientVo> patients) {
    return patients.stream()
        .map(PatientVo::asPatient)
        .toList();
  }

  // 患者取得
  @Override
  public void getPatients(GetPatientsRequest request, StreamObserver<GetPatientsResponse> responseObserver) {
    callService(responseObserver, () -> {
      return GetPatientsResponse.newBuilder()
          .addAllPatients(asPatientList(service.getPatients()))
          .build();
    });
  }

  private static Optional<LocalDate> makeOptDate(boolean hasDate, Date date) {
    return (hasDate ? Optional.of(date) : Optional.<Date>empty())
        .map(MapUtils::asLocalDate);
  }

  private static Optional<LocalDate> getOptStartDate(UpdateAdmissionRequest req) {
    return makeOptDate(req.hasStartDate(), req.getStartDate());
  }

  private static Optional<LocalDate> getOptEndDate(CreateAdmissionRequest req) {
    return makeOptDate(req.hasEndDate(), req.getEndDate());
  }

  private static Optional<LocalDate> getOptEndDate(UpdateAdmissionRequest req) {
    return makeOptDate(req.hasEndDate(), req.getEndDate());
  }

  // 入院作成
  @Override
  public void createAdmission(CreateAdmissionRequest request, StreamObserver<CreateAdmissionResponse> responseObserver) {
    callService(responseObserver, () -> {
      val endDate = getOptEndDate(request);
      val id = service.createAdmission(request.getPatientId(), request.getRoomId(),
          asLocalDate(request.getStartDate()), endDate);

      return CreateAdmissionResponse.newBuilder().setId(id).build();
    });
  }

  // 入院更新
  @Override
  public void updateAdmission(UpdateAdmissionRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      service.updateAdmission(request.getId(), getOptStartDate(request),
          getOptEndDate(request), asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  // 入院削除
  @Override
  public void deleteAdmission(DeleteAdmissionRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      service.deleteAdmission(request.getId(), asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  private static List<Admission> asAdmissionList(List<AdmissionVo> admissions) {
    return admissions.stream()
        .map(AdmissionVo::asAdmission)
        .toList();
  }

  // 入院取得
  @Override
  public void getAdmissions(GetAdmissionsRequest request, StreamObserver<GetAdmissionsResponse> responseObserver) {
    callService(responseObserver, () -> {
      return GetAdmissionsResponse.newBuilder()
          .addAllAdmissions(asAdmissionList(service.getAdmissions(request.getPatientId())))
          .build();
    });
  }

  // 外出作成
  @Override
  public void createRoomOutside(CreateRoomOutsideRequest request, StreamObserver<CreateRoomOutsideResponse> responseObserver) {
    callService(responseObserver, () -> {
      val id = service.createRoomOutside(request.getAdmissionId(), asLocalDate(request.getStartDate()), asLocalDate(request.getEndDate()));
      return CreateRoomOutsideResponse.newBuilder().setId(id).build();
    });
  }

  // 外出更新
  @Override
  public void updateRoomOutside(UpdateRoomOutsideRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      service.updateRoomOutside(request.getId(), asLocalDate(request.getStartDate()), asLocalDate(request.getEndDate()),
          asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  // 外出削除
  @Override
  public void deleteRoomOutside(DeleteRoomOutsideRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      service.deleteRoomOutside(request.getId(), asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  private static List<RoomOutside> asRoomOutsideList(List<RoomOutsideVo> roomOutsides) {
    return roomOutsides.stream()
        .map(RoomOutsideVo::asRoomOutside)
        .toList();
  }

  // 外出取得
  @Override
  public void getRoomOutsides(GetRoomOutsidesRequest request, StreamObserver<GetRoomOutsidesResponse> responseObserver) {
    callService(responseObserver, () -> {
      return GetRoomOutsidesResponse.newBuilder()
          .addAllRoomOutsides(asRoomOutsideList(service.getRoomOutsides(request.getAdmissionId())))
          .build();
    });
  }

  // 移動作成
  @Override
  public void createRoomMove(CreateRoomMoveRequest request, StreamObserver<CreateRoomMoveResponse> responseObserver) {
    callService(responseObserver, () -> {
      val id = service.createRoomMove(request.getAdmissionId(), request.getFromRoomId(),
          request.getToRoomId(), asLocalDate(request.getMoveDate()));
      return CreateRoomMoveResponse.newBuilder().setId(id).build();
    });
  }

  // 移動更新
  @Override
  public void updateRoomMove(UpdateRoomMoveRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      service.updateRoomMove(request.getId(), request.getFromRoomId(),
          request.getToRoomId(), asLocalDate(request.getMoveDate()), asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  // 移動削除
  @Override
  public void deleteRoomMove(DeleteRoomMoveRequest request, StreamObserver<Empty> responseObserver) {
    callService(responseObserver, () -> {
      service.deleteRoomMove(request.getId(), asLocalDateTime(request.getUpdatedTime()));
      return Empty.getDefaultInstance();
    });
  }

  private static List<RoomMove> asRoomMovesList(List<RoomMoveVo> roomMoves) {
    return roomMoves.stream()
        .map(RoomMoveVo::asRoomMove)
        .toList();
  }

  // 移動取得
  @Override
  public void getRoomMoves(GetRoomMovesRequest request, StreamObserver<GetRoomMovesResponse> responseObserver) {
    callService(responseObserver, () -> {
      return GetRoomMovesResponse.newBuilder()
          .addAllRoomMoves(asRoomMovesList(service.getRoomMoves(request.getAdmissionId())))
          .build();
    });
  }

  private static List<RoomDuration> asRoomDurationList(List<RoomDurationVo> roomDurations) {
    return roomDurations.stream()
        .map(RoomDurationVo::asRoomDuration)
        .toList();
  }

  // 部屋期間取得
  @Override
  public void getRoomDurations(GetRoomDurationsRequest request, StreamObserver<GetRoomDurationsResponse> responseObserver) {
    callService(responseObserver, () -> {
      return GetRoomDurationsResponse.newBuilder()
          .addAllRoomDurations(asRoomDurationList(service.getRoomDurations(request.getPatientId())))
          .build();
    });
  }
}
