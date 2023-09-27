package com.io.codesystem.codemaintenancefile;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.io.codesystem.codemaintenancelog.CodeMaintenanceLoggerService;
import com.io.codesystem.search.medicine.Medicine;
import com.io.codesystem.utils.CustomResponse;
import com.io.codesystem.utils.S3Service;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CodeMaintenanceFileService {

	@Autowired
	CodeMaintenanceFileRepository codeMaintenanceFileRepository;

	@Autowired
	CodeMaintenanceLoggerService codeMaintenanceLoggerService;

	@Autowired
	S3Service s3Service;

	public CodeMaintenanceFile saveCodeMaintenanceFile(CodeMaintenanceFile codeMaintenanceFile) {

		return codeMaintenanceFileRepository.save(codeMaintenanceFile);
	}

	public Page<CodeMaintenanceFile> getCodeStandardFileDetails(Pageable pageable) {

		return codeMaintenanceFileRepository.findAll(pageable);
	}

	public Page<CodeMaintenanceFile> getByProcessedState(@RequestParam String processedState, Pageable pageable) {

		return codeMaintenanceFileRepository.findByProcessedState(processedState, pageable);
	}

	public CodeMaintenanceFile getCodeMaintenanceFileById(int fileId) {

		Optional<CodeMaintenanceFile> codeMaintenanceFile = codeMaintenanceFileRepository.findById(fileId);

		if (codeMaintenanceFile.isPresent()) {

			return codeMaintenanceFile.get();
		}

		else
			return null;
	}

	public CustomResponse deleteCodeMaintenanceFile(int fileId, int userId) {

		try {

			Optional<CodeMaintenanceFile> codeMaintenanceFile = codeMaintenanceFileRepository.findById(fileId);

			if (codeMaintenanceFile.isPresent()) {
				codeMaintenanceFileRepository.deleteById(fileId);
				s3Service.deleteFileInS3Bucket(codeMaintenanceFile.get().getFilePath());
				codeMaintenanceLoggerService.saveCodeMaintenanceLog(fileId, "File Deletion",
						"File Deleted Successfully", userId);
				return new CustomResponse("FIle Deleted Successfully", "", HttpStatus.OK);
			}
		} catch (Exception e) {
			codeMaintenanceLoggerService.saveCodeMaintenanceLog(fileId, "File Deletion", "File Deletion Failed",
					userId);
			return new CustomResponse("File Deletion Failed", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	public void updateCodeMaintenanceFileStatusById(int fileId, String processedState, String currentStatus,
			int userId) {

		Optional<CodeMaintenanceFile> codeMaintenanceFile = codeMaintenanceFileRepository.findById(fileId);
		log.info("======CurrentStatus:: " + currentStatus);
		log.info("======codeMaintenanceFile:: " + codeMaintenanceFile);
		if (codeMaintenanceFile.isPresent()) {
			CodeMaintenanceFile file = codeMaintenanceFile.get();
			file.setProcessedState(processedState);
			file.setCurrentStatus(currentStatus);
			Timestamp newModifiedDate = Timestamp.valueOf(LocalDateTime.now());
			// System.out.println(newModifiedDate.toString());
			file.setModifiedDate(newModifiedDate);
			file.setModifiedUserId(userId);
			codeMaintenanceFileRepository.save(file);
		}
	}

	public void updateCodeMaintenanceFilePathById(int fileId, String filePath) {
		Optional<CodeMaintenanceFile> codeMaintenanceFile = codeMaintenanceFileRepository.findById(fileId);
		log.info("======codeMaintenanceFile Path:: " + filePath);
		if (codeMaintenanceFile.isPresent()) {
			CodeMaintenanceFile file = codeMaintenanceFile.get();
			file.setFilePath(filePath);
			Timestamp newModifiedDate = Timestamp.valueOf(LocalDateTime.now());
			// System.out.println(newModifiedDate.toString());
			file.setModifiedDate(newModifiedDate);

			codeMaintenanceFileRepository.save(file);
		}
	}

	public CodeMaintenanceFile getByProcessedState(int fileId) {
		// TODO Auto-generated method stub
		return codeMaintenanceFileRepository.findByProcessedState(fileId);
	}


}