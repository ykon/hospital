package com.example.hospital;

import static com.example.hospital.util.MapUtils.asGrpcDate;
import static com.example.hospital.util.MapUtils.asTimestamp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.example.hospital.controller.HospitalController;
import com.example.hospital.grpc.CreateAdmissionRequest;
import com.example.hospital.grpc.CreatePatientRequest;
import com.example.hospital.grpc.CreateRoomMoveRequest;
import com.example.hospital.grpc.CreateRoomOutsideRequest;
import com.example.hospital.grpc.CreateRoomRequest;
import com.example.hospital.grpc.DeleteAdmissionRequest;
import com.example.hospital.grpc.DeletePatientRequest;
import com.example.hospital.grpc.DeleteRoomMoveRequest;
import com.example.hospital.grpc.DeleteRoomOutsideRequest;
import com.example.hospital.grpc.DeleteRoomRequest;
import com.example.hospital.grpc.GetAdmissionsRequest;
import com.example.hospital.grpc.GetPatientsRequest;
import com.example.hospital.grpc.GetRoomDurationsRequest;
import com.example.hospital.grpc.GetRoomMovesRequest;
import com.example.hospital.grpc.GetRoomOutsidesRequest;
import com.example.hospital.grpc.GetRoomsRequest;
import com.example.hospital.grpc.HospitalGrpc;
import com.example.hospital.grpc.HospitalGrpc.HospitalBlockingStub;
import com.example.hospital.grpc.Patient;
import com.example.hospital.grpc.Room;
import com.example.hospital.grpc.UpdateAdmissionRequest;
import com.example.hospital.grpc.UpdatePatientRequest;
import com.example.hospital.grpc.UpdateRoomMoveRequest;
import com.example.hospital.grpc.UpdateRoomOutsideRequest;
import com.example.hospital.grpc.UpdateRoomRequest;
import com.example.hospital.mapper.HospitalMapper;
import com.example.hospital.util.MapUtils;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HospitalTests extends GrpcTestBase {

  private static HospitalBlockingStub blockingStub;

  @Autowired
  private HospitalMapper mapper;

  private static void setBlockingStub() {
    blockingStub = HospitalGrpc.newBlockingStub(buildChannel());
  }

  @BeforeAll
  static void setup(@Autowired HospitalController pc) throws IOException {
    initServer(pc, HospitalTests::setBlockingStub);
  }

  private void deleteRooms(String... numbers) {
    Arrays.stream(numbers).forEach(mapper::deleteRoomByNumber);
  }

  private int createRoom(String number) {
    val req = CreateRoomRequest.newBuilder().setNumber(number).build();
    return blockingStub.createRoom(req).getId();
  }

  @Test
  void createRoom() {
    val number = "Test@createRoom";

    try {
      val id = createRoom(number);
      val room = mapper.selectRoomById(id).get();
      assertThat(room.getNumber()).isEqualTo(number);
    } finally {
      deleteRooms(number);
    }
  }

  private static UpdateRoomRequest makeUpdateRoomRequest(int id, String number, LocalDateTime updatedAt) {
    return UpdateRoomRequest.newBuilder()
        .setId(id)
        .setNumber(number)
        .setUpdatedTime(asTimestamp(updatedAt))
        .build();
  }

  @Test
  void updateRoom() {
    val number1 = "Test@updateRoom/1";
    val number2 = "Test@updateRoom/2";

    try {
      val id = createRoom(number1);
      val room1 = mapper.selectRoomById(id).get();
      assertThat(room1.getNumber()).isEqualTo(number1);

      val req = makeUpdateRoomRequest(id, number2, room1.getUpdatedAt());
      blockingStub.updateRoom(req);

      val room2 = mapper.selectRoomById(id).get();
      assertThat(room2.getNumber()).isEqualTo(number2);
    } finally {
      deleteRooms(number1, number2);
    }
  }

  private static void assertThatGrpcCode(StatusRuntimeException e, Status.Code code) {
    assertThat(e.getStatus().getCode()).isEqualTo(code);
  }

  private static void assertThatGrpcException(Runnable func, Status.Code code) {
    assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> {
      func.run();
    }).satisfies(e -> assertThatGrpcCode(e, code));
  }

  private static void assertThatInvalidArgumentException(Runnable func) {
    assertThatGrpcException(func, Status.Code.INVALID_ARGUMENT);
  }

  private static void assertThatNotFoundException(Runnable func) {
    assertThatGrpcException(func, Status.Code.NOT_FOUND);
  }

  private static void assertThatAbortedException(Runnable func) {
    assertThatGrpcException(func, Status.Code.ABORTED);
  }

  @Test
  void updateRoomInvalidArgs() {
    val number1 = "Test@updateRoomInvalidArgs/1";
    val number2 = "Test@updateRoomInvalidArgs/2";

    try {
      val id = createRoom(number1);
      val room = mapper.selectRoomById(id).get();
      assertThat(room.getNumber()).isEqualTo(number1);

      val req1 = makeUpdateRoomRequest(id, number2, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.updateRoom(req1));

      val req2 = makeUpdateRoomRequest(id + 1, number2, room.getUpdatedAt());
      assertThatNotFoundException(() -> blockingStub.updateRoom(req2));
    } finally {
      deleteRooms(number1, number2);
    }
  }

  private static DeleteRoomRequest makeDeleteRoomRequest(int id, LocalDateTime updatedAt) {
    return DeleteRoomRequest.newBuilder()
        .setId(id)
        .setUpdatedTime(asTimestamp(updatedAt))
        .build();
  }

  @Test
  void deleteRoom() {
    val number = "Test@deleteRoom";

    try {
      val id = createRoom(number);
      val room = mapper.selectRoomById(id).get();
      assertThat(room.getNumber()).isEqualTo(number);

      val req = makeDeleteRoomRequest(id, room.getUpdatedAt());
      blockingStub.deleteRoom(req);
      assertThat(mapper.selectRoomById(id)).isEmpty();
    } finally {
      deleteRooms(number);
    }
  }

  @Test
  void deleteRoomInvalidArgs() {
    val number = "Test@deleteRoomInvalidArgs";

    try {
      val id = createRoom(number);
      val room = mapper.selectRoomById(id).get();
      assertThat(room.getNumber()).isEqualTo(number);

      val req1 = makeDeleteRoomRequest(id, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.deleteRoom(req1));
      assertThat(mapper.selectRoomById(id)).isNotEmpty();

      val req2 = makeDeleteRoomRequest(id + 1, room.getUpdatedAt());
      assertThatNotFoundException(() -> blockingStub.deleteRoom(req2));
      assertThat(mapper.selectRoomById(id)).isNotEmpty();
    } finally {
      deleteRooms(number);
    }
  }

  @Test
  void getRooms() {
    val number1 = "Test@getRooms/1";
    val number2 = "Test@getRooms/2";
    val number3 = "Test@getRooms/3";

    try {
      createRoom(number1);
      createRoom(number2);
      createRoom(number3);

      val req = GetRoomsRequest.newBuilder().build();
      val resp = blockingStub.getRooms(req);
      val list = resp.getRoomsList().stream().map(Room::getNumber).toList();
      assertThat(list).isEqualTo(List.of(number1, number2, number3));
    } finally {
      deleteRooms(number1, number2, number3);
    }
  }

  private void deletePatients(String... names) {
    Arrays.stream(names).forEach(mapper::deletePatientByName);
  }

  private int createPatient(String name) {
    val req = CreatePatientRequest.newBuilder().setName(name).build();
    return blockingStub.createPatient(req).getId();
  }

  @Test
  void createPatient() {
    val name = "Test@createPatient";

    try {
      val id = createPatient(name);
      val pat = mapper.selectPatientById(id).get();
      assertThat(pat.getName()).isEqualTo(name);
    } finally {
      deletePatients(name);
    }
  }

  private static UpdatePatientRequest makeUpdatePatientRequest(int id, String name, LocalDateTime updatedAt) {
    return UpdatePatientRequest.newBuilder()
        .setId(id)
        .setName(name)
        .setUpdatedTime(asTimestamp(updatedAt))
        .build();
  }

  @Test
  void updatePatient() {
    val name1 = "Test@updatePatient/1";
    val name2 = "Test@updatePatient/2";

    try {
      val id = createPatient(name1);
      val pat1 = mapper.selectPatientById(id).get();
      assertThat(pat1.getName()).isEqualTo(name1);

      val req = makeUpdatePatientRequest(id, name2, pat1.getUpdatedAt());
      blockingStub.updatePatient(req);

      val pat2 = mapper.selectPatientById(id).get();
      assertThat(pat2.getName()).isEqualTo(name2);
    } finally {
      deletePatients(name1, name2);
    }
  }

  @Test
  void updatePatientInvalidArgs() {
    val name1 = "Test@updatePatientInvalid/1";
    val name2 = "Test@updatePatientInvalid/2";

    try {
      val id = createPatient(name1);
      val pat = mapper.selectPatientById(id).get();
      assertThat(pat.getName()).isEqualTo(name1);

      val req1 = makeUpdatePatientRequest(id, name2, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.updatePatient(req1));

      val req2 = makeUpdatePatientRequest(id + 1, name2, pat.getUpdatedAt());
      assertThatNotFoundException(() -> blockingStub.updatePatient(req2));
    } finally {
      deletePatients(name1, name2);
    }
  }

  private static DeletePatientRequest makeDeletePatientRequest(int id, LocalDateTime updatedAt) {
    return DeletePatientRequest.newBuilder()
        .setId(id)
        .setUpdatedTime(asTimestamp(updatedAt))
        .build();
  }

  @Test
  void deletePatient() {
    val name = "Test@deletePatient";

    try {
      val id = createPatient(name);
      val pat = mapper.selectPatientById(id).get();
      assertThat(pat.getName()).isEqualTo(name);

      val req = makeDeletePatientRequest(id, pat.getUpdatedAt());
      blockingStub.deletePatient(req);
      assertThat(mapper.selectPatientById(id)).isEmpty();
    } finally {
      deletePatients(name);
    }
  }

  @Test
  void deletePatientInvalidArgs() {
    val name = "Test@deletePatientInvalid";

    try {
      val id = createPatient(name);
      val pat = mapper.selectPatientById(id).get();
      assertThat(pat.getName()).isEqualTo(name);

      val req1 = makeDeletePatientRequest(id, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.deletePatient(req1));
      assertThat(mapper.selectPatientById(id)).isNotEmpty();

      val req2 = makeDeletePatientRequest(id + 1, pat.getUpdatedAt());
      assertThatNotFoundException(() -> blockingStub.deletePatient(req2));
      assertThat(mapper.selectPatientById(id)).isNotEmpty();
    } finally {
      deletePatients(name);
    }
  }

  @Test
  void getPatients() {
    val name1 = "Test@getPatients/1";
    val name2 = "Test@getPatients/2";
    val name3 = "Test@getPatients/3";

    try {
      createPatient(name1);
      createPatient(name2);
      createPatient(name3);

      val req = GetPatientsRequest.newBuilder().build();
      val resp = blockingStub.getPatients(req);
      val list = resp.getPatientsList().stream().map(Patient::getName).toList();
      assertThat(list).isEqualTo(List.of(name1, name2, name3));
    } finally {
      deletePatients(name1, name2, name3);
    }
  }

  private int createAdmission(int patientId, int roomId, LocalDate startDate, Optional<LocalDate> endDate) {
    val builder = CreateAdmissionRequest.newBuilder()
        .setPatientId(patientId)
        .setRoomId(roomId)
        .setStartDate(asGrpcDate(startDate));

    endDate.map(MapUtils::asGrpcDate).ifPresent(builder::setEndDate);

    val resp = blockingStub.createAdmission(builder.build());
    return resp.getId();
  }

  @Test
  void createAdmission() {
    val number = "Test@createAdmission/R";
    val name = "Test@createAdmission/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val id = createAdmission(patId, roomId, startDate, endDate);

      val adm = mapper.selectAdmissionById(id).get();
      assertThat(adm.getPatientId()).isEqualTo(patId);
      assertThat(adm.getStartDate()).isEqualTo(startDate);
      assertThat(adm.getEndDate()).isNull();
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createAdmissionWithEndDate() {
    val number = "Test@createAdmissionWithEndDate/R";
    val name = "Test@createAdmissionWithEndDate/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = LocalDate.now().plusDays(10);
      val id = createAdmission(patId, roomId, startDate, Optional.of(endDate));

      val adm = mapper.selectAdmissionById(id).get();
      assertThat(adm.getPatientId()).isEqualTo(patId);
      assertThat(adm.getStartDate()).isEqualTo(startDate);
      assertThat(adm.getEndDate()).isEqualTo(endDate);
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createAdmissionInvalidDateOrder() {
    val number = "Test@createAdmissionInvalidDateOrder/R";
    val name = "Test@createAdmissionInvalidDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = LocalDate.now().minusDays(10);

      assertThatInvalidArgumentException(() -> {
        createAdmission(patId, roomId, startDate, Optional.of(endDate));
      });
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createAdmissionInvalidPrevDateOrder() {
    val number = "Test@createAdmissionInvalidPrevDateOrder/R";
    val name = "Test@createAdmissionInvalidPrevDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = LocalDate.now().plusDays(10);
      createAdmission(patId, roomId, startDate, Optional.of(endDate));

      assertThatInvalidArgumentException(() -> {
        createAdmission(patId, roomId, endDate.minusDays(5), Optional.empty());
      });
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createAdmissionInvalidNullDateOrder() {
    val number = "Test@createAdmissionInvalidNullDateOrder/R";
    val name = "Test@createAdmissionInvalidNullDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      createAdmission(patId, roomId, startDate, Optional.empty());

      assertThatInvalidArgumentException(() -> {
        createAdmission(patId, roomId, startDate.plusDays(10), Optional.empty());
      });
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  private static UpdateAdmissionRequest makeUpdateAdmissionRequest(int id, Optional<LocalDate> startDate,
      Optional<LocalDate> endDate, LocalDateTime updatedAt) {

    val builder = UpdateAdmissionRequest.newBuilder()
        .setId(id)
        .setUpdatedTime(asTimestamp(updatedAt));

    startDate.map(MapUtils::asGrpcDate).ifPresent(builder::setStartDate);
    endDate.map(MapUtils::asGrpcDate).ifPresent(builder::setEndDate);

    return builder.build();
  }

  @Test
  void updateAdmission() {
    val number = "Test@updateAdmission/R";
    val name = "Test@updateAdmission/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate1, endDate1);

      val adm1 = mapper.selectAdmissionById(admId).get();
      assertThat(adm1.getPatientId()).isEqualTo(patId);
      assertThat(adm1.getStartDate()).isEqualTo(startDate1);
      assertThat(adm1.getEndDate()).isNull();

      val startDate2 = Optional.of(startDate1.plusDays(10));
      val endDate2 = Optional.of(startDate1.plusDays(20));
      val updatedAt = adm1.getUpdatedAt();
      val req = makeUpdateAdmissionRequest(admId, startDate2, endDate2, updatedAt);

      blockingStub.updateAdmission(req);

      val adm2 = mapper.selectAdmissionById(admId).get();
      assertThat(adm2.getPatientId()).isEqualTo(patId);
      assertThat(adm2.getStartDate()).isEqualTo(startDate2.get());
      assertThat(adm2.getEndDate()).isEqualTo(endDate2.get());
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateAdmissionWithoutStartDate() {
    val number = "Test@updateAdmissionWithoutStartDate/R";
    val name = "Test@updateAdmissionWithoutStartDate/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate1, endDate1);

      val adm1 = mapper.selectAdmissionById(admId).get();
      assertThat(adm1.getPatientId()).isEqualTo(patId);
      assertThat(adm1.getStartDate()).isEqualTo(startDate1);
      assertThat(adm1.getEndDate()).isNull();

      val startDate2 = Optional.<LocalDate>empty();
      val endDate2 = Optional.of(startDate1.plusDays(20));
      val updatedAt = adm1.getUpdatedAt();
      val req = makeUpdateAdmissionRequest(admId, startDate2, endDate2, updatedAt);

      blockingStub.updateAdmission(req);

      val adm2 = mapper.selectAdmissionById(admId).get();
      assertThat(adm2.getPatientId()).isEqualTo(patId);
      assertThat(adm2.getStartDate()).isEqualTo(startDate1);
      assertThat(adm2.getEndDate()).isEqualTo(endDate2.get());
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateAdmissionWithoutEndDate() {
    val number = "Test@updateAdmissionWithoutEndDate/R";
    val name = "Test@updateAdmissionWithoutEndDate/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate1, endDate1);

      val adm1 = mapper.selectAdmissionById(admId).get();
      assertThat(adm1.getPatientId()).isEqualTo(patId);
      assertThat(adm1.getStartDate()).isEqualTo(startDate1);
      assertThat(adm1.getEndDate()).isNull();

      val startDate2 = Optional.of(startDate1.plusDays(10));
      val endDate2 = Optional.<LocalDate>empty();
      val updatedAt = adm1.getUpdatedAt();
      val req = makeUpdateAdmissionRequest(admId, startDate2, endDate2, updatedAt);

      blockingStub.updateAdmission(req);

      val adm2 = mapper.selectAdmissionById(admId).get();
      assertThat(adm2.getPatientId()).isEqualTo(patId);
      assertThat(adm2.getStartDate()).isEqualTo(startDate2.get());
      assertThat(adm2.getEndDate()).isNull();
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateAdmissionInvalidArgs() {
    val number = "Test@updateAdmissionInvalidArgs/R";
    val name = "Test@updateAdmissionInvalidArgs/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate1, endDate1);

      val startDate2 = Optional.of(startDate1.plusDays(10));
      val endDate2 = Optional.of(startDate1.plusDays(20));
      val req1 = makeUpdateAdmissionRequest(admId, startDate2, endDate2, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.updateAdmission(req1));

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val req2 = makeUpdateAdmissionRequest(admId + 1, startDate2, endDate2, updatedAt);
      assertThatNotFoundException(() -> blockingStub.updateAdmission(req2));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateAdmissionInvalidDateOrder() {
    val number = "Test@updateAdmissionInvalidDateOrder/R";
    val name = "Test@updateAdmissionInvalidDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate1, endDate1);

      val startDate2 = Optional.of(startDate1.plusDays(10));
      val endDate2 = Optional.of(startDate1.minusDays(10));
      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val req = makeUpdateAdmissionRequest(admId, startDate2, endDate2, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateAdmission(req));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateAdmissionExistingDateOrder() {
    val number = "Test@updateAdmissionExistingDateOrder/R";
    val name = "Test@updateAdmissionExistingDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.of(startDate1.plusDays(10));
      createAdmission(patId, roomId, startDate1, endDate1);

      val startDate2 = startDate1.plusDays(20);
      val endDate2 = Optional.of(startDate1.plusDays(40));
      val admId = createAdmission(patId, roomId, startDate2, endDate2);

      val startDate3 = startDate1.plusDays(60);
      val endDate3 = Optional.<LocalDate>empty();
      createAdmission(patId, roomId, startDate3, endDate3);

      val startDate4 = Optional.of(endDate1.get().plusDays(15));
      val endDate4 = Optional.of(endDate1.get().plusDays(30));
      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val req = makeUpdateAdmissionRequest(admId, startDate4, endDate4, updatedAt);
      blockingStub.updateAdmission(req);

      val adm = mapper.selectAdmissionById(admId).get();
      assertThat(adm.getPatientId()).isEqualTo(patId);
      assertThat(adm.getStartDate()).isEqualTo(startDate4.get());
      assertThat(adm.getEndDate()).isEqualTo(endDate4.get());
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateAdmissionInvalidExistingDateOrder() {
    val number = "Test@updateAdmissionInvalidExistingDateOrder/R";
    val name = "Test@updateAdmissionInvalidExistingDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.of(startDate1.plusDays(10));
      createAdmission(patId, roomId, startDate1, endDate1);

      val startDate2 = startDate1.plusDays(20);
      val endDate2 = Optional.of(startDate1.plusDays(40));
      val admId = createAdmission(patId, roomId, startDate2, endDate2);

      val startDate3 = startDate1.plusDays(60);
      val endDate3 = Optional.of(startDate1.plusDays(90));
      createAdmission(patId, roomId, startDate3, endDate3);

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val startDate4 = Optional.of(startDate1.plusDays(5));
      val endDate4 = Optional.of(startDate1.plusDays(30));
      val req1 = makeUpdateAdmissionRequest(admId, startDate4, endDate4, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateAdmission(req1));

      val startDate5 = Optional.of(startDate1.plusDays(30));
      val endDate5 = Optional.of(startDate1.plusDays(70));
      val req2 = makeUpdateAdmissionRequest(admId, startDate5, endDate5, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateAdmission(req2));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateAdmissionOutDateOrder() {
    val number = "Test@updateAdmissionOutDateOrder/R";
    val name = "Test@updateAdmissionOutDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate1, endDate1);

      val outStartDate = startDate1.plusDays(20);
      val outEndDate = startDate1.plusDays(30);
      createRoomOutside(admId, outStartDate, outEndDate);

      val startDate2 = Optional.of(startDate1.plusDays(10));
      val endDate2 = Optional.of(startDate1.plusDays(40));
      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val req = makeUpdateAdmissionRequest(admId, startDate2, endDate2, updatedAt);
      blockingStub.updateAdmission(req);

      val adm = mapper.selectAdmissionById(admId).get();
      assertThat(adm.getPatientId()).isEqualTo(patId);
      assertThat(adm.getStartDate()).isEqualTo(startDate2.get());
      assertThat(adm.getEndDate()).isEqualTo(endDate2.get());
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateAdmissionInvalidOutDateOrder() {
    val number = "Test@updateAdmissionInvalidOutDateOrder/R";
    val name = "Test@updateAdmissionInvalidOutDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate1, endDate1);

      val outStartDate = startDate1.plusDays(20);
      val outEndDate = startDate1.plusDays(30);
      createRoomOutside(admId, outStartDate, outEndDate);

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val startDate2 = Optional.of(startDate1.plusDays(30));
      val endDate2 = Optional.of(startDate1.plusDays(40));
      val req1 = makeUpdateAdmissionRequest(admId, startDate2, endDate2, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateAdmission(req1));

      assertThat(mapper.selectAdmissionById(admId).get().getUpdatedAt()).isEqualTo(updatedAt);
      val startDate3 = Optional.of(startDate1.plusDays(10));
      val endDate3 = Optional.of(startDate1.plusDays(20));
      val req2 = makeUpdateAdmissionRequest(admId, startDate3, endDate3, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateAdmission(req2));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateAdmissionMoveDateOrder() {
    val number1 = "Test@updateAdmissionMoveDateOrder/R1";
    val number2 = "Test@updateAdmissionMoveDateOrder/R2";
    val number3 = "Test@updateAdmissionMoveDateOrder/R3";
    val name = "Test@updateAdmissionMoveDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate1, endDate1);

      val moveDate1 = startDate1.plusDays(10);
      createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate1.plusDays(30);
      createRoomMove(admId, roomId2, roomId3, moveDate2);

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val startDate2 = Optional.of(startDate1.plusDays(5));
      val endDate2 = Optional.of(startDate1.plusDays(40));
      val req = makeUpdateAdmissionRequest(admId, startDate2, endDate2, updatedAt);
      blockingStub.updateAdmission(req);

      val adm = mapper.selectAdmissionById(admId).get();
      assertThat(adm.getPatientId()).isEqualTo(patId);
      assertThat(adm.getStartDate()).isEqualTo(startDate2.get());
      assertThat(adm.getEndDate()).isEqualTo(endDate2.get());
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void updateAdmissionInvalidMoveDateOrder() {
    val number1 = "Test@updateAdmissionInvalidMoveDateOrder/R1";
    val number2 = "Test@updateAdmissionInvalidMoveDateOrder/R2";
    val number3 = "Test@updateAdmissionInvalidMoveDateOrder/R3";
    val name = "Test@updateAdmissionInvalidMoveDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate1, endDate1);

      val moveDate1 = startDate1.plusDays(10);
      createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate1.plusDays(30);
      createRoomMove(admId, roomId2, roomId3, moveDate2);

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val startDate2 = Optional.of(startDate1.plusDays(20));
      val endDate2 = Optional.<LocalDate>empty();
      val req1 = makeUpdateAdmissionRequest(admId, startDate2, endDate2, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateAdmission(req1));

      assertThat(mapper.selectAdmissionById(admId).get().getUpdatedAt()).isEqualTo(updatedAt);
      val startDate3 = Optional.<LocalDate>empty();
      val endDate3 = Optional.of(startDate1.plusDays(20));
      val req2 = makeUpdateAdmissionRequest(admId, startDate3, endDate3, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateAdmission(req2));

      assertThat(mapper.selectAdmissionById(admId).get().getUpdatedAt()).isEqualTo(updatedAt);
      val startDate4 = Optional.of(startDate1.plusDays(10));
      val endDate4 = Optional.of(startDate1.plusDays(20));
      val req3 = makeUpdateAdmissionRequest(admId, startDate4, endDate4, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateAdmission(req3));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  private static DeleteAdmissionRequest makeDeleteAdmissionRequest(int id, LocalDateTime updatedAt) {
    return DeleteAdmissionRequest.newBuilder()
        .setId(id)
        .setUpdatedTime(asTimestamp(updatedAt))
        .build();
  }

  @Test
  void deleteAdmission() {
    val number = "Test@deleteAdmission/R";
    val name = "Test@deleteAdmission/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val req = makeDeleteAdmissionRequest(admId, updatedAt);
      blockingStub.deleteAdmission(req);
      assertThat(mapper.selectAdmissionById(admId)).isEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void deleteAdmissionInvalidArgs() {
    val number = "Test@deleteAdmissionInvalidArgs/R";
    val name = "Test@deleteAdmissionInvalidArgs/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val req1 = makeDeleteAdmissionRequest(admId, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.deleteAdmission(req1));
      assertThat(mapper.selectAdmissionById(admId)).isNotEmpty();

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val req2 = makeDeleteAdmissionRequest(admId + 1, updatedAt);
      assertThatNotFoundException(() -> blockingStub.deleteAdmission(req2));
      assertThat(mapper.selectAdmissionById(admId)).isNotEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void deleteAdmissionInvalidMoveState() {
    val number1 = "Test@deleteAdmissionInvalidMoveState/R1";
    val number2 = "Test@deleteAdmissionInvalidMoveState/R2";
    val name = "Test@deleteAdmissionInvalidMoveState/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate = startDate.plusDays(10);
      createRoomMove(admId, roomId1, roomId2, moveDate);

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val req = makeDeleteAdmissionRequest(admId, updatedAt);
      assertThatAbortedException(() -> blockingStub.deleteAdmission(req));
      assertThat(mapper.selectAdmissionById(admId)).isNotEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2);
    }
  }

  @Test
  void deleteAdmissionInvalidOutState() {
    val number = "Test@deleteAdmissionInvalidOutState/R";
    val name = "Test@deleteAdmissionInvalidOutState/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate = startDate.plusDays(10);
      val outEndDate = startDate.plusDays(20);
      createRoomOutside(admId, outStartDate, outEndDate);

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val req = makeDeleteAdmissionRequest(admId, updatedAt);
      assertThatAbortedException(() -> blockingStub.deleteAdmission(req));
      assertThat(mapper.selectAdmissionById(admId)).isNotEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  void deleteAdmissionInvalidEndState() {
    val number = "Test@deleteAdmissionInvalidEndState/R";
    val name = "Test@deleteAdmissionInvalidEndState/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.of(startDate.plusDays(10));
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val updatedAt = mapper.selectAdmissionById(admId).get().getUpdatedAt();
      val req = makeDeleteAdmissionRequest(admId, updatedAt);
      assertThatAbortedException(() -> blockingStub.deleteAdmission(req));
      assertThat(mapper.selectAdmissionById(admId)).isNotEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void getAdmissions() {
    val number = "Test@updateAdmissions/R";
    val name = "Test@updateAdmissions/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate1 = LocalDate.now();
      val endDate1 = Optional.of(startDate1.plusDays(10));
      createAdmission(patId, roomId, startDate1, endDate1);

      val startDate2 = startDate1.plusDays(30);
      val endDate2 = Optional.of(startDate1.plusDays(40));
      createAdmission(patId, roomId, startDate2, endDate2);

      val startDate3 = startDate1.plusDays(60);
      val endDate3 = Optional.of(startDate1.plusDays(80));
      createAdmission(patId, roomId, startDate3, endDate3);

      val req = GetAdmissionsRequest.newBuilder().setPatientId(patId).build();
      val resp = blockingStub.getAdmissions(req);
      val list = resp.getAdmissionsList();

      assertThat(list).hasSize(3);
      assertThat(list.get(0).getStartDate()).isEqualTo(asGrpcDate(startDate1));
      assertThat(list.get(0).getEndDate()).isEqualTo(asGrpcDate(endDate1.get()));

      assertThat(list.get(1).getStartDate()).isEqualTo(asGrpcDate(startDate2));
      assertThat(list.get(1).getEndDate()).isEqualTo(asGrpcDate(endDate2.get()));

      assertThat(list.get(2).getStartDate()).isEqualTo(asGrpcDate(startDate3));
      assertThat(list.get(2).getEndDate()).isEqualTo(asGrpcDate(endDate3.get()));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  private int createRoomOutside(int admissionId, LocalDate startDate, LocalDate endDate) {
    val req = CreateRoomOutsideRequest.newBuilder()
        .setAdmissionId(admissionId)
        .setStartDate(asGrpcDate(startDate))
        .setEndDate(asGrpcDate(endDate))
        .build();

    return blockingStub.createRoomOutside(req).getId();
  }

  @Test
  void createRoomOutside() {
    val number = "Test@createRoomOutside/R";
    val name = "Test@createRoomOutside/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate = startDate.plusDays(10);
      val outEndDate = startDate.plusDays(20);
      val outId = createRoomOutside(admId, outStartDate, outEndDate);

      val out = mapper.selectRoomOutsideById(outId).get();
      assertThat(out.getAdmissionId()).isEqualTo(admId);
      assertThat(out.getStartDate()).isEqualTo(outStartDate);
      assertThat(out.getEndDate()).isEqualTo(outEndDate);
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createRoomOutsideNotFound() {
    val number = "Test@createRoomOutsideNotFound/R";
    val name = "Test@createRoomOutsideNotFound/P";

    try {
      val roomId1 = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      assertThatNotFoundException(() -> createRoomOutside(admId + 1, outStartDate1, outEndDate1));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createRoomOutsideInvalidDateOrder() {
    val number = "Test@createRoomOutsideInvalidDateOrder/R";
    val name = "Test@createRoomOutsideInvalidDateOrder/P";

    try {
      val roomId1 = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val outStartDate1 = startDate.minusDays(10);
      val outEndDate1 = startDate.minusDays(5);
      assertThatInvalidArgumentException(() -> createRoomOutside(admId, outStartDate1, outEndDate1));

      val outStartDate2 = startDate.plusDays(20);
      val outEndDate2 = startDate.plusDays(10);
      assertThatInvalidArgumentException(() -> createRoomOutside(admId, outStartDate2, outEndDate2));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createRoomOutsideInvalidEndDateOrder() {
    val number = "Test@createRoomOutsideInvalidEndDateOrder/R";
    val name = "Test@createRoomOutsideInvalidEndDateOrder/P";

    try {
      val roomId1 = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.of(startDate.plusDays(30));
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val outStartDate = startDate.plusDays(10);
      val outEndDate = startDate.plusDays(40);
      assertThatInvalidArgumentException(() -> createRoomOutside(admId, outStartDate, outEndDate));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createRoomOutsidePrevDateOrder() {
    val number = "Test@createRoomOutsidePrevDateOrder/R";
    val name = "Test@createRoomOutsidePrevDateOrder/P";

    try {
      val roomId1 = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(30);
      val outId1 = createRoomOutside(admId, outStartDate1, outEndDate1);
      assertThat(mapper.selectRoomOutsideById(outId1)).isNotEmpty();

      val outStartDate2 = startDate.plusDays(40);
      val outEndDate2 = startDate.plusDays(50);
      val outId2 = createRoomOutside(admId, outStartDate2, outEndDate2);
      assertThat(mapper.selectRoomOutsideById(outId2)).isNotEmpty();

      val outStartDate3 = startDate.plusDays(60);
      val outEndDate3 = startDate.plusDays(70);
      val outId3 = createRoomOutside(admId, outStartDate3, outEndDate3);
      assertThat(mapper.selectRoomOutsideById(outId3)).isNotEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createRoomOutsideInvalidPrevDateOrder() {
    val number = "Test@createRoomOutsideInvalidPrevDateOrder/R";
    val name = "Test@createRoomOutsideInvalidPrevDateOrder/P";

    try {
      val roomId1 = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(30);
      val outId1 = createRoomOutside(admId, outStartDate1, outEndDate1);
      assertThat(mapper.selectRoomOutsideById(outId1)).isNotEmpty();

      val outStartDate2 = startDate.plusDays(20);
      val outEndDate2 = startDate.plusDays(25);
      assertThatInvalidArgumentException(() -> createRoomOutside(admId, outStartDate2, outEndDate2));

      val outStartDate3 = startDate.plusDays(5);
      val outEndDate3 = startDate.plusDays(40);
      assertThatInvalidArgumentException(() -> createRoomOutside(admId, outStartDate3, outEndDate3));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void createRoomOutsideMoveDateOrder() {
    val number1 = "Test@createRoomOutsideMoveDateOrder/R1";
    val number2 = "Test@createRoomOutsideMoveDateOrder/R2";
    val number3 = "Test@createRoomOutsideMoveDateOrder/R3";
    val name = "Test@createRoomOutsideMoveDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(20);
      createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate.plusDays(50);
      createRoomMove(admId, roomId2, roomId3, moveDate2);

      val outStartDate = startDate.plusDays(10);
      val outEndDate = startDate.plusDays(15);
      val outId1 = createRoomOutside(admId, outStartDate, outEndDate);
      assertThat(mapper.selectRoomOutsideById(outId1)).isNotEmpty();

      val outStartDate2 = startDate.plusDays(25);
      val outEndDate2 = startDate.plusDays(40);
      val outId2 = createRoomOutside(admId, outStartDate2, outEndDate2);
      assertThat(mapper.selectRoomOutsideById(outId2)).isNotEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void createRoomOutsideInvalidMoveDateOrder() {
    val number1 = "Test@createRoomOutsideInvalidMoveDateOrder/R1";
    val number2 = "Test@createRoomOutsideInvalidMoveDateOrder/R2";
    val number3 = "Test@createRoomOutsideInvalidMoveDateOrder/R3";
    val name = "Test@createRoomOutsideInvalidMoveDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(20);
      createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate.plusDays(50);
      createRoomMove(admId, roomId2, roomId3, moveDate2);

      val outStartDate = startDate.plusDays(10);
      val outEndDate = startDate.plusDays(30);
      assertThatInvalidArgumentException(() -> createRoomOutside(admId, outStartDate, outEndDate));

      val outStartDate2 = startDate.plusDays(40);
      val outEndDate2 = startDate.plusDays(60);
      assertThatInvalidArgumentException(() -> createRoomOutside(admId, outStartDate2, outEndDate2));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  private static UpdateRoomOutsideRequest makeUpdateRoomOutsideRequest(int id, LocalDate startDate, LocalDate endDate, LocalDateTime updatedAt) {
    return UpdateRoomOutsideRequest.newBuilder()
        .setId(id)
        .setUpdatedTime(asTimestamp(updatedAt))
        .setStartDate(asGrpcDate(startDate))
        .setEndDate(asGrpcDate(endDate))
        .build();
  }

  @Test
  void updateRoomOutside() {
    val number = "Test@updateRoomOutside/R";
    val name = "Test@updateRoomOutside/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      val outId = createRoomOutside(admId, outStartDate1, outEndDate1);

      val outStartDate2 = startDate.plusDays(30);
      val outEndDate2 = startDate.plusDays(60);

      val updatedAt = mapper.selectRoomOutsideById(outId).get().getUpdatedAt();
      val req = makeUpdateRoomOutsideRequest(outId, outStartDate2, outEndDate2, updatedAt);
      blockingStub.updateRoomOutside(req);

      val out = mapper.selectRoomOutsideById(outId).get();
      assertThat(out.getAdmissionId()).isEqualTo(admId);
      assertThat(out.getStartDate()).isEqualTo(outStartDate2);
      assertThat(out.getEndDate()).isEqualTo(outEndDate2);
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateRoomOutsideInvalidArgs() {
    val number = "Test@updateRoomOutsideInvalidArgs/R";
    val name = "Test@updateRoomOutsideInvalidArgs/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      val outId = createRoomOutside(admId, outStartDate1, outEndDate1);

      val outStartDate2 = startDate.plusDays(30);
      val outEndDate2 = startDate.plusDays(60);
      val req1 = makeUpdateRoomOutsideRequest(outId, outStartDate2, outEndDate2, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomOutside(req1));

      val updatedAt = mapper.selectRoomOutsideById(outId).get().getUpdatedAt();
      val req2 = makeUpdateRoomOutsideRequest(outId + 1, outStartDate2, outEndDate2, updatedAt);
      assertThatNotFoundException(() -> blockingStub.updateRoomOutside(req2));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateRoomOutsideInvalidDateOrder() {
    val number = "Test@updateRoomOutsideInvalidDateOrder/R";
    val name = "Test@updateRoomOutsideInvalidDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      val outId = createRoomOutside(admId, outStartDate1, outEndDate1);

      val outStartDate2 = startDate.plusDays(30);
      val outEndDate2 = startDate.plusDays(20);
      val updatedAt = mapper.selectRoomOutsideById(outId).get().getUpdatedAt();
      val req = makeUpdateRoomOutsideRequest(outId, outStartDate2, outEndDate2, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomOutside(req));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateRoomOutsideExistingDateOrder() {
    val number = "Test@updateRoomOutsideExistingDateOrder/R";
    val name = "Test@updateRoomOutsideExistingDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      createRoomOutside(admId, outStartDate1, outEndDate1);

      val outStartDate2 = startDate.plusDays(30);
      val outEndDate2 = startDate.plusDays(50);
      val outId = createRoomOutside(admId, outStartDate2, outEndDate2);

      val outStartDate3 = startDate.plusDays(60);
      val outEndDate3 = startDate.plusDays(90);
      createRoomOutside(admId, outStartDate3, outEndDate3);

      val outStartDate4 = startDate.plusDays(30);
      val outEndDate4 = startDate.plusDays(40);
      val updatedAt = mapper.selectRoomOutsideById(outId).get().getUpdatedAt();
      val req = makeUpdateRoomOutsideRequest(outId, outStartDate4, outEndDate4, updatedAt);
      blockingStub.updateRoomOutside(req);

      val out = mapper.selectRoomOutsideById(outId).get();
      assertThat(out.getAdmissionId()).isEqualTo(admId);
      assertThat(out.getStartDate()).isEqualTo(outStartDate4);
      assertThat(out.getEndDate()).isEqualTo(outEndDate4);
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateRoomOutsideInvalidExistingDateOrder() {
    val number = "Test@updateRoomOutsideInvalidExistingDateOrder/R";
    val name = "Test@updateRoomOutsideInvalidExistingDateOrder/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      createRoomOutside(admId, outStartDate1, outEndDate1);

      val outStartDate2 = startDate.plusDays(30);
      val outEndDate2 = startDate.plusDays(50);
      val outId = createRoomOutside(admId, outStartDate2, outEndDate2);

      val outStartDate3 = startDate.plusDays(60);
      val outEndDate3 = startDate.plusDays(90);
      createRoomOutside(admId, outStartDate3, outEndDate3);

      val updatedAt = mapper.selectRoomOutsideById(outId).get().getUpdatedAt();
      val outStartDate4 = startDate.plusDays(15);
      val outEndDate4 = startDate.plusDays(30);
      val req1 = makeUpdateRoomOutsideRequest(outId, outStartDate4, outEndDate4, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomOutside(req1));

      val outStartDate5 = startDate.plusDays(30);
      val outEndDate5 = startDate.plusDays(70);
      val req2 = makeUpdateRoomOutsideRequest(outId, outStartDate5, outEndDate5, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomOutside(req2));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void updateRoomOutsideMoveDateOrder() {
    val number1 = "Test@updateRoomOutsideMoveDateOrder/R1";
    val number2 = "Test@updateRoomOutsideMoveDateOrder/R2";
    val number3 = "Test@updateRoomOutsideMoveDateOrder/R3";
    val name = "Test@updateRoomOutsideMoveDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      val outId = createRoomOutside(admId, outStartDate1, outEndDate1);

      val moveDate1 = startDate.plusDays(30);
      createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate.plusDays(50);
      createRoomMove(admId, roomId2, roomId3, moveDate2);

      val updatedAt = mapper.selectRoomOutsideById(outId).get().getUpdatedAt();
      val outStartDate2 = startDate.plusDays(15);
      val outEndDate2 = startDate.plusDays(25);
      val req1 = makeUpdateRoomOutsideRequest(outId, outStartDate2, outEndDate2, updatedAt);
      blockingStub.updateRoomOutside(req1);

      val out1 = mapper.selectRoomOutsideById(outId).get();
      assertThat(out1.getStartDate()).isEqualTo(outStartDate2);
      assertThat(out1.getEndDate()).isEqualTo(outEndDate2);

      val outStartDate3 = startDate.plusDays(40);
      val outEndDate3 = startDate.plusDays(45);
      val req2 = makeUpdateRoomOutsideRequest(outId, outStartDate3, outEndDate3, updatedAt);
      blockingStub.updateRoomOutside(req2);

      val out2 = mapper.selectRoomOutsideById(outId).get();
      assertThat(out2.getStartDate()).isEqualTo(outStartDate3);
      assertThat(out2.getEndDate()).isEqualTo(outEndDate3);
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void updateRoomOutsideInvalidMoveDateOrder() {
    val number1 = "Test@updateRoomOutsideInvalidMoveDateOrder/R1";
    val number2 = "Test@updateRoomOutsideInvalidMoveDateOrder/R2";
    val number3 = "Test@updateRoomOutsideInvalidMoveDateOrder/R3";
    val name = "Test@updateRoomOutsideInvalidMoveDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      val outId = createRoomOutside(admId, outStartDate1, outEndDate1);

      val moveDate1 = startDate.plusDays(30);
      createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate.plusDays(50);
      createRoomMove(admId, roomId2, roomId3, moveDate2);

      val updatedAt = mapper.selectRoomOutsideById(outId).get().getUpdatedAt();
      val outStartDate2 = startDate.plusDays(15);
      val outEndDate2 = startDate.plusDays(35);
      val req1 = makeUpdateRoomOutsideRequest(outId, outStartDate2, outEndDate2, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomOutside(req1));

      val outStartDate3 = startDate.plusDays(40);
      val outEndDate3 = startDate.plusDays(55);
      val req2 = makeUpdateRoomOutsideRequest(outId, outStartDate3, outEndDate3, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomOutside(req2));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  private static DeleteRoomOutsideRequest makeDeleteRoomOutsideRequest(int id, LocalDateTime updatedAt) {
    return DeleteRoomOutsideRequest.newBuilder()
        .setId(id)
        .setUpdatedTime(asTimestamp(updatedAt))
        .build();
  }

  @Test
  void deleteRoomOutside() {
    val number = "Test@deleteRoomOutside/R";
    val name = "Test@deleteRoomOutside/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      val outId = createRoomOutside(admId, outStartDate1, outEndDate1);

      val updatedAt = mapper.selectRoomOutsideById(outId).get().getUpdatedAt();
      val req = makeDeleteRoomOutsideRequest(outId, updatedAt);
      blockingStub.deleteRoomOutside(req);
      assertThat(mapper.selectRoomOutsideById(outId)).isEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void deleteRoomOutsideInvalidArgs() {
    val number = "Test@deleteRoomOutsideInvalidArgs/R";
    val name = "Test@deleteRoomOutsideInvalidArgs/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      val outId = createRoomOutside(admId, outStartDate1, outEndDate1);

      val req1 = makeDeleteRoomOutsideRequest(outId, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.deleteRoomOutside(req1));
      assertThat(mapper.selectRoomOutsideById(outId)).isNotEmpty();

      val updatedAt = mapper.selectRoomOutsideById(outId).get().getUpdatedAt();
      val req2 = makeDeleteRoomOutsideRequest(outId + 1, updatedAt);
      assertThatNotFoundException(() -> blockingStub.deleteRoomOutside(req2));
      assertThat(mapper.selectRoomOutsideById(outId)).isNotEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  @Test
  void getRoomOutsides() {
    val number = "Test@getRoomOutsides/R";
    val name = "Test@getRoomOutsides/P";

    try {
      val roomId = createRoom(number);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId, startDate, endDate);

      val outStartDate1 = startDate.plusDays(10);
      val outEndDate1 = startDate.plusDays(20);
      createRoomOutside(admId, outStartDate1, outEndDate1);

      val outStartDate2 = startDate.plusDays(30);
      val outEndDate2 = startDate.plusDays(60);
      createRoomOutside(admId, outStartDate2, outEndDate2);

      val outStartDate3 = startDate.plusDays(70);
      val outEndDate3 = startDate.plusDays(90);
      createRoomOutside(admId, outStartDate3, outEndDate3);

      val req = GetRoomOutsidesRequest.newBuilder().setAdmissionId(admId).build();
      val resp = blockingStub.getRoomOutsides(req);
      val list = resp.getRoomOutsidesList();

      assertThat(list).hasSize(3);
      assertThat(list.get(0).getStartDate()).isEqualTo(asGrpcDate(outStartDate1));
      assertThat(list.get(0).getEndDate()).isEqualTo(asGrpcDate(outEndDate1));

      assertThat(list.get(1).getStartDate()).isEqualTo(asGrpcDate(outStartDate2));
      assertThat(list.get(1).getEndDate()).isEqualTo(asGrpcDate(outEndDate2));

      assertThat(list.get(2).getStartDate()).isEqualTo(asGrpcDate(outStartDate3));
      assertThat(list.get(2).getEndDate()).isEqualTo(asGrpcDate(outEndDate3));
    } finally {
      deletePatients(name);
      deleteRooms(number);
    }
  }

  private static int createRoomMove(int admissionId, int fromRoomId, int toRoomId, LocalDate moveDate) {
    val req = CreateRoomMoveRequest.newBuilder()
        .setAdmissionId(admissionId)
        .setFromRoomId(fromRoomId)
        .setToRoomId(toRoomId)
        .setMoveDate(asGrpcDate(moveDate))
        .build();

    return blockingStub.createRoomMove(req).getId();
  }

  @Test
  void createRoomMove() {
    val number1 = "Test@createRoomMove/R1";
    val number2 = "Test@createRoomMove/R2";
    val name = "Test@createRoomMove/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate = startDate.plusDays(10);
      val moveId = createRoomMove(admId, roomId1, roomId2, moveDate);

      val move = mapper.selectRoomMoveById(moveId).get();
      assertThat(move.getAdmissionId()).isEqualTo(admId);
      assertThat(move.getFromRoomId()).isEqualTo(roomId1);
      assertThat(move.getToRoomId()).isEqualTo(roomId2);
      assertThat(move.getMoveDate()).isEqualTo(moveDate);
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2);
    }
  }

  @Test
  void createRoomMoveInvalidFromRoomId() {
    val number1 = "Test@createRoomMoveInvalidFromRoomId/R1";
    val number2 = "Test@createRoomMoveInvalidFromRoomId/R2";
    val number3 = "Test@createRoomMoveInvalidFromRoomId/R3";
    val name = "Test@createRoomMoveInvalidFromRoomId/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate = startDate.plusDays(10);
      assertThatInvalidArgumentException(() -> createRoomMove(admId, roomId2, roomId3, moveDate));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void createRoomMoveNotFound() {
    val number1 = "Test@createRoomMoveNotFound/R1";
    val number2 = "Test@createRoomMoveNotFound/R2";
    val name = "Test@createRoomMoveNotFound/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate = startDate.plusDays(10);
      assertThatNotFoundException(() -> createRoomMove(admId + 1, roomId1, roomId2, moveDate));
      assertThatNotFoundException(() -> createRoomMove(admId, roomId1 + 10, roomId2, moveDate));
      assertThatNotFoundException(() -> createRoomMove(admId, roomId1, roomId2 + 10, moveDate));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2);
    }
  }

  @Test
  void createRoomMoveAdmDateOrder() { // and PrevDateOrder
    val number1 = "Test@createRoomMoveAdmDateOrder/R1";
    val number2 = "Test@createRoomMoveAdmDateOrder/R2";
    val number3 = "Test@createRoomMoveAdmDateOrder/R3";
    val name = "Test@createRoomMoveAdmDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.of(startDate.plusDays(30));
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(10);
      val moveId1 = createRoomMove(admId, roomId1, roomId2, moveDate1);
      assertThat(mapper.selectRoomMoveById(moveId1)).isNotEmpty();

      val moveDate2 = startDate.plusDays(20);
      val moveId2 = createRoomMove(admId, roomId2, roomId3, moveDate2);
      assertThat(mapper.selectRoomMoveById(moveId2)).isNotEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void createRoomMoveInvalidAdmDateOrder() {
    val number1 = "Test@createRoomMoveInvalidAdmDateOrder/R1";
    val number2 = "Test@createRoomMoveInvalidAdmDateOrder/R2";
    val name = "Test@createRoomMoveInvalidAdmDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.of(startDate.plusDays(30));
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.minusDays(10);
      assertThatInvalidArgumentException(() -> createRoomMove(admId, roomId1, roomId2, moveDate1));

      val moveDate2 = startDate.plusDays(40);
      assertThatInvalidArgumentException(() -> createRoomMove(admId, roomId1, roomId2, moveDate2));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2);
    }
  }

  @Test
  void createRoomMoveInvalidPrevDateOrder() {
    val number1 = "Test@createRoomMoveInvalidPrevDateOrder/R1";
    val number2 = "Test@createRoomMoveInvalidPrevDateOrder/R2";
    val name = "Test@createRoomMoveInvalidPrevDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.of(startDate.plusDays(30));
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(20);
      val moveId = createRoomMove(admId, roomId1, roomId2, moveDate1);
      assertThat(mapper.selectRoomMoveById(moveId)).isNotEmpty();

      val moveDate2 = startDate.plusDays(10);
      assertThatInvalidArgumentException(() -> createRoomMove(admId, roomId1, roomId2, moveDate2));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2);
    }
  }

  private static UpdateRoomMoveRequest makeUpdateRoomMoveRequest(int id, int fromRoomId, int toRoomId, LocalDate moveDate, LocalDateTime updatedAt) {
    return UpdateRoomMoveRequest.newBuilder()
        .setId(id)
        .setFromRoomId(fromRoomId)
        .setToRoomId(toRoomId)
        .setMoveDate(asGrpcDate(moveDate))
        .setUpdatedTime(asTimestamp(updatedAt))
        .build();
  }

  @Test
  void updateRoomMove() {
    val number1 = "Test@updateRoomMove/R1";
    val number2 = "Test@updateRoomMove/R2";
    val number3 = "Test@updateRoomMove/R3";
    val name = "Test@updateRoomMove/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(10);
      val moveId = createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate.plusDays(20);
      val updatedAt = mapper.selectRoomMoveById(moveId).get().getUpdatedAt();
      val req = makeUpdateRoomMoveRequest(moveId, roomId1, roomId3, moveDate2, updatedAt);
      blockingStub.updateRoomMove(req);

      val move = mapper.selectRoomMoveById(moveId).get();
      assertThat(move.getAdmissionId()).isEqualTo(admId);
      assertThat(move.getFromRoomId()).isEqualTo(roomId1);
      assertThat(move.getToRoomId()).isEqualTo(roomId3);
      assertThat(move.getMoveDate()).isEqualTo(moveDate2);
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void updateRoomMoveInvalidArgs() {
    val number1 = "Test@updateRoomMoveInvalidArgs/R1";
    val number2 = "Test@updateRoomMoveInvalidArgs/R2";
    val number3 = "Test@updateRoomMoveInvalidArgs/R3";
    val name = "Test@updateRoomMoveInvalidArgs/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(10);
      val moveId = createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate.plusDays(20);
      val req1 = makeUpdateRoomMoveRequest(moveId, roomId1, roomId3, moveDate2, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomMove(req1));

      val updatedAt = mapper.selectRoomMoveById(moveId).get().getUpdatedAt();
      val moveDate3 = startDate.plusDays(30);
      val req2 = makeUpdateRoomMoveRequest(moveId, roomId3, roomId1, moveDate3, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomMove(req2));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void updateRoomMoveNotFound() {
    val number1 = "Test@updateRoomMoveNotFound/R1";
    val number2 = "Test@updateRoomMoveNotFound/R2";
    val number3 = "Test@updateRoomMoveNotFound/R3";
    val name = "Test@updateRoomMoveNotFound/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(10);
      val moveId = createRoomMove(admId, roomId1, roomId2, moveDate1);

      val updatedAt = mapper.selectRoomMoveById(moveId).get().getUpdatedAt();
      val moveDate2 = startDate.plusDays(20);
      val req1 = makeUpdateRoomMoveRequest(moveId + 1, roomId1, roomId3, moveDate2, updatedAt);
      assertThatNotFoundException(() -> blockingStub.updateRoomMove(req1));

      val req2 = makeUpdateRoomMoveRequest(moveId, roomId1 + 10, roomId3, moveDate2, updatedAt);
      assertThatNotFoundException(() -> blockingStub.updateRoomMove(req2));

      val req3 = makeUpdateRoomMoveRequest(moveId, roomId1, roomId3 + 10, moveDate2, updatedAt);
      assertThatNotFoundException(() -> blockingStub.updateRoomMove(req3));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void updateRoomMoveAdmDateOrder() { // and ExistingDateOrder
    val number1 = "Test@updateRoomMoveAdmDateOrder/R1";
    val number2 = "Test@updateRoomMoveAdmDateOrder/R2";
    val number3 = "Test@updateRoomMoveAdmDateOrder/R3";
    val name = "Test@updateRoomMoveAdmDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.of(startDate.plusDays(30));
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(10);
      val moveId = createRoomMove(admId, roomId1, roomId2, moveDate1);

      val updatedAt = mapper.selectRoomMoveById(moveId).get().getUpdatedAt();
      val moveDate2 = startDate.plusDays(20);
      val req1 = makeUpdateRoomMoveRequest(moveId, roomId1, roomId3, moveDate2, updatedAt);
      blockingStub.updateRoomMove(req1);

      val move1 = mapper.selectRoomMoveById(moveId).get();
      assertThat(move1.getFromRoomId()).isEqualTo(roomId1);
      assertThat(move1.getToRoomId()).isEqualTo(roomId3);
      assertThat(move1.getMoveDate()).isEqualTo(moveDate2);

      val moveDate3 = startDate.plusDays(25);
      val req2 = makeUpdateRoomMoveRequest(moveId, roomId1, roomId3, moveDate3, updatedAt);
      blockingStub.updateRoomMove(req2);

      val move2 = mapper.selectRoomMoveById(moveId).get();
      assertThat(move2.getFromRoomId()).isEqualTo(roomId1);
      assertThat(move2.getToRoomId()).isEqualTo(roomId3);
      assertThat(move2.getMoveDate()).isEqualTo(moveDate3);
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void updateRoomMoveInvalidAdmDateOrder() {
    val number1 = "Test@updateRoomMoveInvalidAdmDateOrder/R1";
    val number2 = "Test@updateRoomMoveInvalidAdmDateOrder/R2";
    val number3 = "Test@updateRoomMoveInvalidAdmDateOrder/R3";
    val name = "Test@updateRoomMoveInvalidAdmDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.of(startDate.plusDays(30));
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(10);
      val moveId = createRoomMove(admId, roomId1, roomId2, moveDate1);

      val updatedAt = mapper.selectRoomMoveById(moveId).get().getUpdatedAt();
      val moveDate2 = startDate.minusDays(20);
      val req1 = makeUpdateRoomMoveRequest(moveId, roomId1, roomId3, moveDate2, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomMove(req1));

      val moveDate3 = startDate.plusDays(40);
      val req2 = makeUpdateRoomMoveRequest(moveId, roomId1, roomId3, moveDate3, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomMove(req2));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  @Test
  void updateRoomMoveInvalidExistingDateOrder() {
    val number1 = "Test@updateRoomMoveExistingDateOrder/R1";
    val number2 = "Test@updateRoomMoveExistingDateOrder/R2";
    val number3 = "Test@updateRoomMoveExistingDateOrder/R3";
    val name = "Test@updateRoomMoveExistingDateOrder/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(10);
      createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate.plusDays(20);
      val moveId = createRoomMove(admId, roomId2, roomId3, moveDate2);

      val updatedAt = mapper.selectRoomMoveById(moveId).get().getUpdatedAt();
      val moveDate3 = startDate.plusDays(10);
      val req = makeUpdateRoomMoveRequest(moveId, roomId3, roomId1, moveDate3, updatedAt);
      assertThatInvalidArgumentException(() -> blockingStub.updateRoomMove(req));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3);
    }
  }

  private static DeleteRoomMoveRequest makeDeleteRoomMoveRequest(int id, LocalDateTime updatedAt) {
    return DeleteRoomMoveRequest.newBuilder()
        .setId(id)
        .setUpdatedTime(asTimestamp(updatedAt))
        .build();
  }

  @Test
  void deleteRoomMove() {
    val number1 = "Test@deleteRoomMove/R1";
    val number2 = "Test@deleteRoomMove/R2";
    val name = "Test@deleteRoomMove/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate = startDate.plusDays(10);
      val moveId = createRoomMove(admId, roomId1, roomId2, moveDate);
      val updatedAt = mapper.selectRoomMoveById(moveId).get().getUpdatedAt();
      val req = makeDeleteRoomMoveRequest(moveId, updatedAt);
      blockingStub.deleteRoomMove(req);
      assertThat(mapper.selectRoomMoveById(moveId)).isEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2);
    }
  }

  @Test
  void deleteRoomMoveInvalidArgs() {
    val number1 = "Test@deleteRoomMove/R1";
    val number2 = "Test@deleteRoomMove/R2";
    val name = "Test@deleteRoomMove/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate = startDate.plusDays(10);
      val moveId = createRoomMove(admId, roomId1, roomId2, moveDate);

      val req1 = makeDeleteRoomMoveRequest(moveId, LocalDateTime.now());
      assertThatInvalidArgumentException(() -> blockingStub.deleteRoomMove(req1));
      assertThat(mapper.selectRoomMoveById(moveId)).isNotEmpty();

      val updatedAt = mapper.selectRoomMoveById(moveId).get().getUpdatedAt();
      val req2 = makeDeleteRoomMoveRequest(moveId + 1, updatedAt);
      assertThatNotFoundException(() -> blockingStub.deleteRoomMove(req2));
      assertThat(mapper.selectRoomMoveById(moveId)).isNotEmpty();
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2);
    }
  }

  @Test
  void getRoomMoves() {
    val number1 = "Test@getRoomMoves/R1";
    val number2 = "Test@getRoomMoves/R2";
    val number3 = "Test@getRoomMoves/R3";
    val number4 = "Test@getRoomMoves/R4";
    val name = "Test@getRoomMoves/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val roomId3 = createRoom(number3);
      val roomId4 = createRoom(number4);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(10);
      createRoomMove(admId, roomId1, roomId2, moveDate1);

      val moveDate2 = startDate.plusDays(30);
      createRoomMove(admId, roomId2, roomId3, moveDate2);

      val moveDate3 = startDate.plusDays(60);
      createRoomMove(admId, roomId3, roomId4, moveDate3);

      val req = GetRoomMovesRequest.newBuilder().setAdmissionId(admId).build();
      val list = blockingStub.getRoomMoves(req).getRoomMovesList();

      assertThat(list).hasSize(3);
      assertThat(list.get(0).getFromRoomId()).isEqualTo(roomId1);
      assertThat(list.get(0).getToRoomId()).isEqualTo(roomId2);
      assertThat(list.get(0).getMoveDate()).isEqualTo(asGrpcDate(moveDate1));

      assertThat(list.get(1).getFromRoomId()).isEqualTo(roomId2);
      assertThat(list.get(1).getToRoomId()).isEqualTo(roomId3);
      assertThat(list.get(1).getMoveDate()).isEqualTo(asGrpcDate(moveDate2));

      assertThat(list.get(2).getFromRoomId()).isEqualTo(roomId3);
      assertThat(list.get(2).getToRoomId()).isEqualTo(roomId4);
      assertThat(list.get(2).getMoveDate()).isEqualTo(asGrpcDate(moveDate3));
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3, number4);
    }
  }

  @Test
  void getRoomDurations() {
    val number1 = "Test@getRoomMoves/R1";
    val number2 = "Test@getRoomMoves/R2";
    val number3 = "Test@getRoomMoves/R3";
    val number4 = "Test@getRoomMoves/R4";
    val number5 = "Test@getRoomMoves/R5";
    val name = "Test@getRoomMoves/P";

    try {
      val roomId1 = createRoom(number1);
      val roomId2 = createRoom(number2);
      val patId = createPatient(name);
      val startDate = LocalDate.now();
      val endDate = Optional.<LocalDate>empty();
      val admId = createAdmission(patId, roomId1, startDate, endDate);

      val moveDate1 = startDate.plusDays(10);
      createRoomMove(admId, roomId1, roomId2, moveDate1);

      val roomId3 = createRoom(number3);
      val moveDate2 = startDate.plusDays(20);
      createRoomMove(admId, roomId2, roomId3, moveDate2);

      val roomId4 = createRoom(number4);
      val moveDate3 = startDate.plusDays(30);
      createRoomMove(admId, roomId3, roomId4, moveDate3);

      val roomId5 = createRoom(number5);
      val moveDate4 = startDate.plusDays(40);
      createRoomMove(admId, roomId4, roomId5, moveDate4);

      val req = GetRoomDurationsRequest.newBuilder().setPatientId(patId).build();
      val resp = blockingStub.getRoomDurations(req);
      val list = resp.getRoomDurationsList();

      assertThat(list).hasSize(5);
      assertThat(list.get(0).getRoomId()).isEqualTo(roomId1);
      assertThat(list.get(0).getStartDate()).isEqualTo(asGrpcDate(startDate));
      assertThat(list.get(0).getEndDate()).isEqualTo(asGrpcDate(moveDate1.minusDays(1)));

      assertThat(list.get(1).getRoomId()).isEqualTo(roomId2);
      assertThat(list.get(1).getStartDate()).isEqualTo(asGrpcDate(moveDate1.plusDays(1)));
      assertThat(list.get(1).getEndDate()).isEqualTo(asGrpcDate(moveDate2.minusDays(1)));

      assertThat(list.get(2).getRoomId()).isEqualTo(roomId3);
      assertThat(list.get(2).getStartDate()).isEqualTo(asGrpcDate(moveDate2.plusDays(1)));
      assertThat(list.get(2).getEndDate()).isEqualTo(asGrpcDate(moveDate3.minusDays(1)));

      assertThat(list.get(3).getRoomId()).isEqualTo(roomId4);
      assertThat(list.get(3).getStartDate()).isEqualTo(asGrpcDate(moveDate3.plusDays(1)));
      assertThat(list.get(3).getEndDate()).isEqualTo(asGrpcDate(moveDate4.minusDays(1)));

      assertThat(list.get(4).getRoomId()).isEqualTo(roomId5);
      assertThat(list.get(4).getStartDate()).isEqualTo(asGrpcDate(moveDate4.plusDays(1)));
      assertThat(list.get(4).hasEndDate()).isFalse();
    } finally {
      deletePatients(name);
      deleteRooms(number1, number2, number3, number4, number5);
    }
  }
}
