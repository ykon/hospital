package com.example.hospital.mapper;

import com.example.hospital.vo.AdmissionVo;
import com.example.hospital.vo.PatientVo;
import com.example.hospital.vo.RoomMoveVo;
import com.example.hospital.vo.RoomOutsideVo;
import com.example.hospital.vo.RoomVo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface HospitalMapper {

  // --- room

  @Insert("INSERT room (number) VALUES (#{number})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertRoom(Map<String, Object> map);

  @Update("UPDATE room SET number = #{number} WHERE id = #{id} AND updated_at = #{updatedAt}")
  int updateRoom(Map<String, Object> map);

  @Delete("DELETE FROM room WHERE id = #{id} AND updated_at = #{updatedAt}")
  int deleteRoom(Map<String, Object> map);

  @Delete("DELETE FROM room")
  int deleteRoomAll();

  @Delete("DELETE FROM room WHERE number = #{number}")
  int deleteRoomByNumber(String number);

  @Select("SELECT * FROM room WHERE id = #{id}")
  Optional<RoomVo> selectRoomById(int id);

  @Select("SELECT * FROM room ORDER BY id")
  List<RoomVo> selectRooms();

  // --- patient

  @Insert("INSERT patient (name) VALUES (#{name})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertPatient(Map<String, Object> map);

  @Update("UPDATE patient SET name = #{name} WHERE id = #{id} AND updated_at = #{updatedAt}")
  int updatePatient(Map<String, Object> map);

  @Delete("DELETE FROM patient WHERE id = #{id} AND updated_at = #{updatedAt}")
  int deletePatient(Map<String, Object> map);

  @Delete("DELETE FROM patient WHERE name = #{name}")
  int deletePatientByName(String name);

  @Select("SELECT * FROM patient WHERE id = #{id}")
  Optional<PatientVo> selectPatientById(int id);

  @Select("SELECT * FROM patient ORDER BY id")
  List<PatientVo> selectPatients();

  // --- admission

  @Insert("INSERT admission (patient_id, room_id, start_date, end_date) "
      + "VALUES (#{patientId}, #{roomId}, #{startDate}, #{endDate})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertAdmission(Map<String, Object> map);

  @Update("UPDATE admission SET start_date = IFNULL(#{startDate}, start_date), end_date = #{endDate} "
      + "WHERE id = #{id} AND updated_at = #{updatedAt}")
  int updateAdmission(Map<String, Object> map);

  @Delete("DELETE FROM admission WHERE id = #{id} AND updated_at = #{updatedAt}")
  int deleteAdmission(Map<String, Object> map);

  @Select("SELECT * FROM admission WHERE id = #{id}")
  Optional<AdmissionVo> selectAdmissionById(int id);

  @Select("SELECT * FROM admission WHERE patient_id = #{patientId} ORDER BY id")
  List<AdmissionVo> selectAdmissions(int patientId);

  @Select("SELECT * FROM admission WHERE patient_id = #{patientId} AND id < #{id} ORDER BY id DESC LIMIT 1")
  Optional<AdmissionVo> selectAdmissionPrev(@Param("patientId") int patientId, @Param("id") int id);

  @Select("SELECT * FROM admission WHERE patient_id = #{patientId} AND id > #{id} ORDER BY id LIMIT 1")
  Optional<AdmissionVo> selectAdmissionNext(@Param("patientId") int patientId, @Param("id") int id);

  @Select("SELECT * FROM admission WHERE patient_id = #{patientId} ORDER BY id DESC LIMIT 1")
  Optional<AdmissionVo> selectLatestAdmission(int patientId);

  // --- room_outside

  @Insert("INSERT room_outside (admission_id, start_date, end_date) VALUES (#{admissionId}, #{startDate}, #{endDate})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertRoomOutside(Map<String, Object> map);

  @Update("UPDATE room_outside SET start_date = #{startDate}, end_date = #{endDate} "
      + "WHERE id = #{id} AND updated_at = #{updatedAt}")
  int updateRoomOutside(Map<String, Object> map);

  @Select("SELECT * FROM room_outside WHERE id = #{id}")
  Optional<RoomOutsideVo> selectRoomOutsideById(int id);

  @Delete("DELETE FROM room_outside WHERE id = #{id} AND updated_at = #{updatedAt}")
  int deleteRoomOutside(Map<String, Object> map);

  @Select("SELECT * FROM room_outside WHERE admission_id = #{admissionId} ORDER BY id")
  List<RoomOutsideVo> selectRoomOutsides(int admissionId);

  @Select("SELECT * FROM room_outside WHERE admission_id = #{admissionId} ORDER BY id DESC LIMIT 1")
  Optional<RoomOutsideVo> selectLatestRoomOutside(int admissionId);

  @Select("SELECT * FROM room_outside WHERE admission_id = #{admissionId} AND id < #{id} ORDER BY id DESC LIMIT 1")
  Optional<RoomOutsideVo> selectRoomOutsidePrev(@Param("admissionId") int admissionId, @Param("id") int id);

  @Select("SELECT * FROM room_outside WHERE admission_id = #{admissionId} AND id > #{id} ORDER BY id LIMIT 1")
  Optional<RoomOutsideVo> selectRoomOutsideNext(@Param("admissionId") int admissionId, @Param("id") int id);

  // --- room_move

  @Insert("INSERT room_move (admission_id, from_room_id, to_room_id, move_date) "
      + "VALUES (#{admissionId}, #{fromRoomId}, #{toRoomId}, #{moveDate})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertRoomMove(Map<String, Object> map);

  @Update("UPDATE room_move SET from_room_id = #{fromRoomId}, to_room_id = #{toRoomId}, move_date = #{moveDate} "
      + "WHERE id = #{id} AND updated_at = #{updatedAt}")
  int updateRoomMove(Map<String, Object> map);

  @Select("SELECT * FROM room_move WHERE id = #{id}")
  Optional<RoomMoveVo> selectRoomMoveById(int id);

  @Delete("DELETE FROM room_move WHERE id = #{id} AND updated_at = #{updatedAt}")
  int deleteRoomMove(Map<String, Object> map);

  @Select("SELECT * FROM room_move WHERE admission_id = #{admissionId} ORDER BY id")
  List<RoomMoveVo> selectRoomMoves(int admissionId);

  @Select("SELECT * FROM room_move WHERE admission_id = #{admissionId} ORDER by id DESC LIMIT 1")
  Optional<RoomMoveVo> selectLatestRoomMove(int admissionId);

  @Select("SELECT * FROM room_move WHERE admission_id = #{admissionId} AND id < #{id} ORDER BY id DESC LIMIT 1")
  Optional<RoomMoveVo> selectRoomMovePrev(@Param("admissionId") int admissionId, @Param("id") int id);

  @Select("SELECT * FROM room_move WHERE admission_id = #{admissionId} AND id > #{id} ORDER BY id LIMIT 1")
  Optional<RoomMoveVo> selectRoomMoveNext(@Param("admissionId") int admissionId, @Param("id") int id);
}
