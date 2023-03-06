package com.example.hospital.service;

import static com.example.hospital.util.MapUtils.getIdFromBigInt;

import com.example.hospital.exception.DbException;
import com.example.hospital.mapper.HospitalMapper;
import com.example.hospital.vo.AdmissionVo;
import com.example.hospital.vo.Duration;
import com.example.hospital.vo.PatientVo;
import com.example.hospital.vo.RoomDurationVo;
import com.example.hospital.vo.RoomMoveVo;
import com.example.hospital.vo.RoomOutsideVo;
import com.example.hospital.vo.RoomVo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import one.util.streamex.StreamEx;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Service
public class HospitalService {

  private final HospitalMapper mapper;

  private static void throwIfZero(int count, String msg) {
    if (count == 0) {
      throw new DbException(msg);
    }
  }

  private static Map<String, Object> makeDeleteParams(int id, LocalDateTime updatedAt) {
    return Map.of("id", id, "updatedAt", updatedAt);
  }

  @Transactional
  public int createRoom(String number) {
    val map = new HashMap<String, Object>(Map.of("number", number));
    val count = mapper.insertRoom(map);
    throwIfZero(count, "Failed to insert room.");

    return getIdFromBigInt(map);
  }

  private void throwIfInvalidRoomUpdate(int id, LocalDateTime updatedAt) {
    val room = mapper.selectRoomById(id);
    if (room.isEmpty()) {
      throw new NoSuchElementException("room.id not found: " + id);
    }
    if (!room.get().getUpdatedAt().equals(updatedAt)) {
      throw new IllegalArgumentException("Invalid updatedAt: " + updatedAt);
    }
  }

  @Transactional
  public void updateRoom(int id, String number, LocalDateTime updatedAt) {
    throwIfInvalidRoomUpdate(id, updatedAt);

    val map = Map.<String, Object>of("id", id, "number", number, "updatedAt", updatedAt);
    val count = mapper.updateRoom(map);
    throwIfZero(count, "Failed to update room.");
  }

  @Transactional
  public void deleteRoom(int id, LocalDateTime updatedAt) {
    throwIfInvalidRoomUpdate(id, updatedAt);

    val map = makeDeleteParams(id, updatedAt);
    val count = mapper.deleteRoom(map);
    throwIfZero(count, "Failed to delete room.");
  }

  public List<RoomVo> getRooms() {
    return mapper.selectRooms();
  }

  @Transactional
  public int createPatient(String name) {
    val map = new HashMap<String, Object>(Map.of("name", name));
    val count = mapper.insertPatient(map);
    throwIfZero(count, "Failed to insert patient.");

    return getIdFromBigInt(map);
  }

  private void throwIfInvalidPatientUpdate(int id, LocalDateTime updatedAt) {
    val pat = mapper.selectPatientById(id);
    if (pat.isEmpty()) {
      throw new NoSuchElementException("patient.id not found: " + id);
    }
    if (!pat.get().getUpdatedAt().equals(updatedAt)) {
      throw new IllegalArgumentException("Invalid updatedAt: " + updatedAt);
    }
  }

  @Transactional
  public void udpatePatient(int id, String name, LocalDateTime updatedAt) {
    throwIfInvalidPatientUpdate(id, updatedAt);

    val map = Map.<String, Object>of("id", id, "name", name, "updatedAt", updatedAt);
    val count = mapper.updatePatient(map);
    throwIfZero(count, "Failed to update patient.");
  }

  @Transactional
  public void deletePatient(int id, LocalDateTime updatedAt) {
    throwIfInvalidPatientUpdate(id, updatedAt);

    val map = makeDeleteParams(id, updatedAt);
    val count = mapper.deletePatient(map);
    throwIfZero(count, "Failed to delete patient.");
  }

  public List<PatientVo> getPatients() {
    return mapper.selectPatients();
  }

  private static Map<String, Object> makeInsertAdmissionParams(int patientId, int roomId, LocalDate startDate, Optional<LocalDate> endDate) {
    val map = new HashMap<String, Object>(Map.of("patientId", patientId, "roomId", roomId, "startDate", startDate)); // Map.of: non-null
    map.put("endDate", endDate.orElse(null));

    return map;
  }

  private static boolean validateDateOrder(LocalDate startDate, LocalDate endDate) {
    return startDate.isBefore(endDate);
  }

  private static boolean validateDateOrder(LocalDate startDate, Optional<LocalDate> endDate) {
    return endDate.isEmpty() || validateDateOrder(startDate, endDate.get());
  }

  private static boolean validateDateOrder(Optional<LocalDate> startDate, Optional<LocalDate> endDate) {
    if (startDate.isEmpty() && endDate.isEmpty()) {
      return false;
    }
    if (startDate.isEmpty() || endDate.isEmpty()) {
      return true;
    }

    return validateDateOrder(startDate.get(), endDate.get());
  }

  private static boolean validateCurrentDateOrder(Duration current, Optional<LocalDate> startDate, Optional<LocalDate> endDate) {
    return validateDateOrder(startDate.orElse(current.getStartDate()), endDate);
  }

  private static boolean validateMoveDateOrderForAdm(List<RoomMoveVo> moves, Optional<LocalDate> startDate, Optional<LocalDate> endDate) {
    if (startDate.isEmpty() && endDate.isEmpty()) {
      return false;
    }
    if (moves.isEmpty()) {
      return true;
    }

    val first = moves.get(0).getMoveDate();
    val last = moves.get(moves.size() - 1).getMoveDate();

    if (startDate.isEmpty()) {
      return endDate.get().isAfter(last);
    }

    if (endDate.isEmpty()) {
      return startDate.get().isBefore(first);
    }

    return startDate.get().isBefore(first) && endDate.get().isAfter(last);
  }

  private static <T extends Duration> boolean validatePrevDateOrder(Optional<T> prev, LocalDate startDate) {
    if (prev.isEmpty()) {
      return true;
    }

    val prevEndDate = prev.get().getEndDate();
    return prevEndDate != null && startDate.isAfter(prevEndDate);
  }

  private static <T extends Duration> boolean validateNextDateOrder(Optional<T> next, LocalDate endDate) {
    if (next.isEmpty()) {
      return true;
    }

    return next.get().getStartDate().isAfter(endDate);
  }

  private static <T extends Duration> boolean validateExistingDateOrder(Optional<T> prev, Optional<T> next, LocalDate startDate, LocalDate endDate) {
    return validatePrevDateOrder(prev, startDate) && validateNextDateOrder(next, endDate);
  }

  private static <T extends Duration> boolean validateExistingDateOrder(Optional<T> prev, Optional<T> next, Optional<LocalDate> startDate, Optional<LocalDate> endDate) {
    if (startDate.isEmpty() && endDate.isEmpty()) {
      return false;
    }
    if (startDate.isEmpty()) {
      return validateNextDateOrder(next, endDate.get());
    }
    if (endDate.isEmpty()) {
      return validatePrevDateOrder(prev, startDate.get());
    }

    return validateExistingDateOrder(prev, next, startDate.get(), endDate.get());
  }

  private static boolean validateExistingDateOrder(Optional<RoomMoveVo> prev, Optional<RoomMoveVo> next, LocalDate moveDate) {
    if (prev.isEmpty() && next.isEmpty()) {
      return true;
    }
    if (prev.isEmpty()) {
      return next.get().getMoveDate().isAfter(moveDate);
    }

    return prev.get().getMoveDate().isBefore(moveDate);
  }

  private static boolean validateOutDateOrderForAdm(List<RoomOutsideVo> outsides, Optional<LocalDate> startDate, Optional<LocalDate> endDate) {
    if (startDate.isEmpty() && endDate.isEmpty()) {
      return false;
    }
    if (outsides.isEmpty()) {
      return true;
    }

    val last = outsides.get(outsides.size() - 1);
    val outStartDate = last.getStartDate();
    val outEndDate = last.getEndDate();

    if (startDate.isPresent() && !startDate.get().isBefore(outStartDate)) {
      return false;
    }
    if (endDate.isPresent() && !endDate.get().isAfter(outEndDate)) {
      return false;
    }

    return true;
  }

  private static boolean validateOutDateOrderForMove(List<RoomOutsideVo> outsides, LocalDate moveDate) {
    return outsides.stream()
        .allMatch(o -> moveDate.isBefore(o.getStartDate()) || moveDate.isAfter(o.getEndDate()));
  }

  private static void throwInvalidDateOrder(LocalDate startDate, LocalDate endDate, String errorType) {
    throw new IllegalArgumentException(String.format("Invalid date order: %s, %s (%s)", startDate, endDate, errorType));
  }

  private static void throwInvalidDateOrder(LocalDate startDate, Optional<LocalDate> endDate, String errorType) {
    throw new IllegalArgumentException(String.format("Invalid date order: %s, %s (%s)", startDate, endDate, errorType));
  }

  private static void throwInvalidDateOrder(Optional<LocalDate> startDate, Optional<LocalDate> endDate, String errorType) {
    throw new IllegalArgumentException(String.format("Invalid date order: %s, %s (%s)", startDate, endDate, errorType));
  }

  private static void throwInvalidDateOrder(LocalDate moveDate, String errorType) {
    throw new IllegalArgumentException(String.format("Invalid move date: %s (%s)", moveDate, errorType));
  }

  @Transactional
  public int createAdmission(int patientId, int roomId, LocalDate startDate, Optional<LocalDate> endDate) {
    if (!validateDateOrder(startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "args");
    }

    val prevAdm = mapper.selectLatestAdmission(patientId);
    if (!validatePrevDateOrder(prevAdm, startDate)) {
      throwInvalidDateOrder(startDate, endDate, "prev");
    }

    val map = makeInsertAdmissionParams(patientId, roomId, startDate, endDate);
    val count = mapper.insertAdmission(map);
    throwIfZero(count, "Failed to insert admission.");

    return getIdFromBigInt(map);
  }

  private static Map<String, Object> makeUpdateAdmissionParams(int id, Optional<LocalDate> startDate,
      Optional<LocalDate> endDate, LocalDateTime updatedAt) {

    val map = new HashMap<String, Object>(Map.of("id", id, "updatedAt", updatedAt));
    map.put("startDate", startDate.orElse(null));
    map.put("endDate", endDate.orElse(null));

    return map;
  }

  private void throwIfInvalidAdmissionUpdate(int id, LocalDateTime updatedAt) {
    val adm = mapper.selectAdmissionById(id);
    if (adm.isEmpty()) {
      throw new NoSuchElementException("admission.id not found: " + id);
    }
    if (!adm.get().getUpdatedAt().equals(updatedAt)) {
      throw new IllegalArgumentException("Invalid updatedAt: " + updatedAt);
    }
  }

  @Transactional
  public void updateAdmission(int id, Optional<LocalDate> startDate, Optional<LocalDate> endDate, LocalDateTime updatedAt) {
    throwIfInvalidAdmissionUpdate(id, updatedAt);

    if (!validateDateOrder(startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "args");
    }

    val adm = mapper.selectAdmissionById(id).get();
    val prevAdm = mapper.selectAdmissionPrev(adm.getPatientId(), id);
    val nextAdm = mapper.selectAdmissionNext(adm.getPatientId(), id);
    if (!validateExistingDateOrder(prevAdm, nextAdm, startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "existing");
    }

    if (!validateCurrentDateOrder(adm, startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "current");
    }

    val moves = mapper.selectRoomMoves(id);
    if (!validateMoveDateOrderForAdm(moves, startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "move");
    }

    val outs = mapper.selectRoomOutsides(id);
    if (!validateOutDateOrderForAdm(outs, startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "out");
    }

    val map = makeUpdateAdmissionParams(id, startDate, endDate, updatedAt);
    val count = mapper.updateAdmission(map);
    throwIfZero(count, "Failed to update admission.");
  }

  @Transactional
  public void deleteAdmission(int id, LocalDateTime updatedAt) {
    throwIfInvalidAdmissionUpdate(id, updatedAt);

    if (!mapper.selectRoomMoves(id).isEmpty()) {
      throw new IllegalStateException("Invalid move state");
    }
    if (!mapper.selectRoomOutsides(id).isEmpty()) {
      throw new IllegalStateException("Invalid out state");
    }
    if (mapper.selectAdmissionById(id).get().getEndDate() != null) {
      throw new IllegalStateException("Invalid end state");
    }

    val map = makeDeleteParams(id, updatedAt);
    val count = mapper.deleteAdmission(map);
    throwIfZero(count, "Failed to delete admission.");
  }

  public List<AdmissionVo> getAdmissions(int patientId) {
    return mapper.selectAdmissions(patientId);
  }

  private static Map<String, Object> makeInsertRoomOutsideParams(int admissionId, LocalDate startDate, LocalDate endDate) {
    return new HashMap<>(Map.of("admissionId", admissionId, "startDate", startDate, "endDate", endDate));
  }

  private static boolean validateAdmDateOrder(AdmissionVo adm, LocalDate startDate, LocalDate endDate) {
    if (!adm.getStartDate().isBefore(startDate)) {
      return false;
    }

    val admEndDate = adm.getEndDate();
    if (admEndDate == null) {
      return true;
    }

    return admEndDate.isAfter(endDate);
  }

  private static boolean validateMoveDateOrderForOut(List<RoomMoveVo> moves, LocalDate startDate, LocalDate endDate) {
    return moves.stream().map(RoomMoveVo::getMoveDate)
        .allMatch(m -> m.isBefore(startDate) || m.isAfter(endDate));
  }

  @Transactional
  public int createRoomOutside(int admissionId, LocalDate startDate, LocalDate endDate) {
    val adm = mapper.selectAdmissionById(admissionId);
    if (adm.isEmpty()) {
      throw new NoSuchElementException("admission.id not found: " + admissionId);
    }
    if (!validateDateOrder(startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "args");
    }
    if (!validateAdmDateOrder(adm.get(), startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "adm");
    }

    val prev = mapper.selectLatestRoomOutside(admissionId);
    if (!validatePrevDateOrder(prev, startDate)) {
      throwInvalidDateOrder(startDate, endDate, "prev");
    }
    val moves = mapper.selectRoomMoves(admissionId);
    if (!validateMoveDateOrderForOut(moves, startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "move");
    }

    val map = makeInsertRoomOutsideParams(admissionId, startDate, endDate);
    val count = mapper.insertRoomOutside(map);
    throwIfZero(count, "Failed to insert room_outside.");

    return getIdFromBigInt(map);
  }

  private void throwIfInvalidRoomOutsideUpdate(int id, LocalDateTime updatedAt) {
    val out = mapper.selectRoomOutsideById(id);
    if (out.isEmpty()) {
      throw new NoSuchElementException("room_outside.id not found: " + id);
    }
    if (!out.get().getUpdatedAt().equals(updatedAt)) {
      throw new IllegalArgumentException("Invalid updatedAt: " + updatedAt);
    }
  }

  private static Map<String, Object> makeUpdateRoomOutsideParams(int id, LocalDate startDate, LocalDate endDate, LocalDateTime updatedAt) {
    return Map.of("id", id, "startDate", startDate, "endDate", endDate, "updatedAt", updatedAt);
  }

  @Transactional
  public void updateRoomOutside(int id, LocalDate startDate, LocalDate endDate, LocalDateTime updatedAt) {
    throwIfInvalidRoomOutsideUpdate(id, updatedAt);

    if (!validateDateOrder(startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "args");
    }

    val out = mapper.selectRoomOutsideById(id).get();
    val prev = mapper.selectRoomOutsidePrev(out.getAdmissionId(), id);
    val next = mapper.selectRoomOutsideNext(out.getAdmissionId(), id);
    if (!validateExistingDateOrder(prev, next, startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "existing");
    }

    val moves = mapper.selectRoomMoves(out.getAdmissionId());
    if (!validateMoveDateOrderForOut(moves, startDate, endDate)) {
      throwInvalidDateOrder(startDate, endDate, "move");
    }

    val map = makeUpdateRoomOutsideParams(id, startDate, endDate, updatedAt);
    val count = mapper.updateRoomOutside(map);
    throwIfZero(count, "Failed to update room_outside.");
  }

  @Transactional
  public void deleteRoomOutside(int id, LocalDateTime updatedAt) {
    throwIfInvalidRoomOutsideUpdate(id, updatedAt);

    val map = makeDeleteParams(id, updatedAt);
    val count = mapper.deleteRoomOutside(map);
    throwIfZero(count, "Failed to delete room_outside.");
  }

  public List<RoomOutsideVo> getRoomOutsides(int admissionId) {
    return mapper.selectRoomOutsides(admissionId);
  }

  private static Map<String, Object> makeCreateRoomMoveParams(int admissionId, int fromRoomId, int toRoomId, LocalDate moveDate) {
    return new HashMap<>(Map.of("admissionId", admissionId, "fromRoomId", fromRoomId, "toRoomId", toRoomId, "moveDate", moveDate));
  }

  private static boolean validateAdmMoveDateOrder(AdmissionVo adm, LocalDate moveDate) {
    val endDate = adm.getEndDate();
    return adm.getStartDate().isBefore(moveDate) && (endDate == null || endDate.isAfter(moveDate));
  }

  private static boolean validatePrevMoveDateOrder(Optional<RoomMoveVo> prev, LocalDate moveDate) {
    return prev.isEmpty() || prev.get().getMoveDate().isBefore(moveDate);
  }

  @Transactional
  public int createRoomMove(int admissionId, int fromRoomId, int toRoomId, LocalDate moveDate) {
    val adm = mapper.selectAdmissionById(admissionId);
    if (adm.isEmpty()) {
      throw new NoSuchElementException("admission.id not found: " + admissionId);
    }
    if (mapper.selectRoomById(fromRoomId).isEmpty()) {
      throw new NoSuchElementException("From room.id not found: " + fromRoomId);
    }
    if (mapper.selectRoomById(toRoomId).isEmpty()) {
      throw new NoSuchElementException("To room.id not found: " + toRoomId);
    }

    if (!validateAdmMoveDateOrder(adm.get(), moveDate)) {
      throwInvalidDateOrder(moveDate, "adm");
    }

    val prev = mapper.selectLatestRoomMove(admissionId);
    if (!validatePrevMoveDateOrder(prev, moveDate)) {
      throwInvalidDateOrder(moveDate, "prev");
    }
    if (getFromRoomId(adm.get(), prev) != fromRoomId) {
      throw new IllegalArgumentException("Invalid fromRoomId: " + fromRoomId);
    }

    val outs = mapper.selectRoomOutsides(admissionId);
    if (!validateOutDateOrderForMove(outs, moveDate)) {
      throwInvalidDateOrder(moveDate, "out");
    }

    val map = makeCreateRoomMoveParams(admissionId, fromRoomId, toRoomId, moveDate);
    val count = mapper.insertRoomMove(map);
    throwIfZero(count, "Failed to create room_move.");

    return getIdFromBigInt(map);
  }

  private void throwIfInvalidRoomMoveUpdate(int id, LocalDateTime updatedAt) {
    val move = mapper.selectRoomMoveById(id);
    if (move.isEmpty()) {
      throw new NoSuchElementException("room_move.id not found: " + id);
    }
    if (!move.get().getUpdatedAt().equals(updatedAt)) {
      throw new IllegalArgumentException("Invalid updatedAt: " + updatedAt);
    }
  }

  private static Map<String, Object> makeUpdateRoomMoveParams(int id, int fromRoomId, int toRoomId, LocalDate moveDate, LocalDateTime updatedAt) {
    return Map.of("id", id, "fromRoomId", fromRoomId, "toRoomId", toRoomId, "moveDate", moveDate, "updatedAt", updatedAt);
  }

  private static int getFromRoomId(AdmissionVo adm, Optional<RoomMoveVo> prev) {
    if (prev.isEmpty()) {
      return adm.getRoomId();
    }

    return prev.get().getToRoomId();
  }

  @Transactional
  public void updateRoomMove(int id, int fromRoomId, int toRoomId, LocalDate moveDate, LocalDateTime updatedAt) {
    throwIfInvalidRoomMoveUpdate(id, updatedAt);

    if (mapper.selectRoomById(fromRoomId).isEmpty()) {
      throw new NoSuchElementException("From room.id not found: " + fromRoomId);
    }
    if (mapper.selectRoomById(toRoomId).isEmpty()) {
      throw new NoSuchElementException("To room.id not found: " + toRoomId);
    }

    val out = mapper.selectRoomMoveById(id).get();
    val adm = mapper.selectAdmissionById(out.getAdmissionId()).get();
    if (!validateAdmMoveDateOrder(adm, moveDate)) {
      throwInvalidDateOrder(moveDate, "adm");
    }

    val prev = mapper.selectRoomMovePrev(out.getAdmissionId(), id);
    val next = mapper.selectRoomMoveNext(out.getAdmissionId(), id);
    if (!validateExistingDateOrder(prev, next, moveDate)) {
      throwInvalidDateOrder(moveDate, "existing");
    }
    if (getFromRoomId(adm, prev) != fromRoomId) {
      throw new IllegalArgumentException("Invalid fromRoomId: " + fromRoomId);
    }

    val outs = mapper.selectRoomOutsides(out.getAdmissionId());
    if (!validateOutDateOrderForMove(outs, moveDate)) {
      throwInvalidDateOrder(moveDate, "out");
    }

    val map = makeUpdateRoomMoveParams(id, fromRoomId, toRoomId, moveDate, updatedAt);
    val count = mapper.updateRoomMove(map);
    throwIfZero(count, "Failed to update room_move.");
  }

  @Transactional
  public void deleteRoomMove(int id, LocalDateTime updatedAt) {
    throwIfInvalidRoomMoveUpdate(id, updatedAt);

    val map = makeDeleteParams(id, updatedAt);
    val count = mapper.deleteRoomMove(map);
    throwIfZero(count, "Failed to delete room_move.");
  }

  public List<RoomMoveVo> getRoomMoves(int admissionId) {
    return mapper.selectRoomMoves(admissionId);
  }

  private static RoomDurationVo makeOneDuration(AdmissionVo adm) {
    return new RoomDurationVo(adm.getRoomId(), adm.getStartDate(), adm.getEndDate());
  }

  private static RoomDurationVo makeOneDuration(List<RoomMoveVo> moves) {
    return new RoomDurationVo(moves.get(0).getToRoomId(), moves.get(0).getMoveDate().plusDays(1), moves.get(1).getMoveDate().minusDays(1));
  }

  private static RoomDurationVo makeFirstDuration(AdmissionVo adm, List<RoomMoveVo> moves) {
    return new RoomDurationVo(adm.getRoomId(), adm.getStartDate(), moves.get(0).getMoveDate().minusDays(1));
  }

  private static RoomDurationVo makeLastDuration(AdmissionVo adm, List<RoomMoveVo> moves) {
    val last = moves.get(moves.size() - 1);
    return new RoomDurationVo(last.getToRoomId(), last.getMoveDate().plusDays(1), adm.getEndDate());
  }

  private static List<RoomDurationVo> makeDurations(AdmissionVo adm, List<RoomMoveVo> moves) {
    return StreamEx.ofSubLists(moves, 2, 1)
        .map(HospitalService::makeOneDuration)
        .prepend(makeFirstDuration(adm, moves))
        .append(makeLastDuration(adm, moves))
        .toList();
  }

  public List<RoomDurationVo> getRoomDurations(int patientId) {
    val adm = mapper.selectLatestAdmission(patientId);
    if (adm.isEmpty()) {
      return List.of();
    }

    val admId = adm.get().getId();
    val moves = mapper.selectRoomMoves(admId);
    if (moves.isEmpty()) {
      return List.of(makeOneDuration(adm.get()));
    }

    return makeDurations(adm.get(), moves);
  }
}
