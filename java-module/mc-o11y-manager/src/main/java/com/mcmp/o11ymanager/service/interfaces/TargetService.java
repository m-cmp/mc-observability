package com.mcmp.o11ymanager.service.interfaces;

import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import java.util.List;

public interface TargetService {

  TargetDTO get(String nsId, String mciId, String targetId);

  TargetDTO getByNsMci(String nsId, String mciId);

  List<TargetDTO> list();

  TargetDTO post(String nsId, String mciId, String targetId, TargetRegisterDTO dto);

  TargetDTO put(String targetId, String nsId, String mciId, TargetUpdateDTO request);

  void delete(String targetId, String nsId, String mciId);

  //getMiningDBs()
  //{
  //  "rs_code": "string",
  //  "rs_msg": "string",
  //  "data": {
  //    "url": "string",
  //    "database": "string",
  //    "retention_policy": "string",
  //    "username": "string",
  //    "password": "string"
  //  },
  //  "error_message": "string"
  //}



}
