package com.io.codesystem.icd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.io.codesystem.CodeMaintenanceService;
import com.io.codesystem.asynctasks.AsyncTasksStatusService;
import com.io.codesystem.codemaintenancefile.CodeMaintenanceFile;
import com.io.codesystem.codemaintenancefile.CodeMaintenanceFileRepository;
import com.io.codesystem.codemaintenancefile.CodeMaintenanceFileService;
import com.io.codesystem.codemaintenancelog.CodeMaintenanceLoggerService;
import com.io.codesystem.search.icd.IcdCodeSearchService;
import com.io.codesystem.utils.CodeVerification;
import com.io.codesystem.utils.CustomResponse;
import com.io.codesystem.utils.S3Service;
import com.io.codesystem.utils.TableRecordCounts;
import com.io.codesystem.utils.UtilsService;
import com.io.codesystem.utils.ValidationCheck;
import com.io.codesystem.verificationlog.CodeVerificationLogModel;
import com.io.codesystem.verificationlog.CodeVerificationLogRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IcdCodeMaintenanceService extends CodeMaintenanceService {

	@Autowired
	S3Service s3Service;

	@Autowired
	UtilsService utilsService;

	@Autowired
	CodeMaintenanceFileService codeMaintenanceFileService;

	@Autowired
	CodeMaintenanceLoggerService codeMaintenanceLoggerService;

	@Autowired
	IcdCodeStandardRepository icdCodeStandardRepository;

	@Autowired
	IcdSyncResultsRepository icdSynchResultsRepository;

	@Autowired
	IcdDataVerificationRepository icdDataVerificationRepository;

	@Autowired
	IcdPostSyncResultsRepository icdPostSyncResultsRepository;

	@Autowired
	IcdTableRecordCountsRepository icdTableRecordCountsRepository;

	@Autowired
	CodeVerificationLogRepository codeVerificationLogRepository;

	@Autowired
	AsyncTasksStatusService asyncTasksStatusService;

	@Autowired
	CodeMaintenanceFileRepository codeMaintenanceFileRepository;
	
	@Autowired
	IcdCodeSearchService icdCodeSearchService;

	@Value("${aws.s3.root-folder}")
	private String rootFolderName;

	@Value("${aws.s3.upload-folder}")
	private String uploadFolderName;

	@Value("${aws.s3.inprocess-folder}")
	private String inprocessFolderName;

	@Value("${aws.s3.processed-folder}")
	private String processedFolderName;

	@Value("${aws.s3.rejected-folder}")
	private String rejectedFolderName;

	@Override
	protected CustomResponse uploadFileToS3(String codeType, String releaseVersion, Date releaseDate,
			MultipartFile releaseFile, int userId, String effectiveFrom, String effectiveTo) {

		String zipFileName = releaseFile.getOriginalFilename();
		String targetCodeTypeFolderName = utilsService.getTargetCodeTypeFolderName(codeType);
		if (utilsService.prepareVerificationStatus("version-validation", codeType, releaseDate))
			return new CustomResponse("Zip File Uploading Failed",
					"Error:Current Uploading File Version is olderthan already existing version",
					HttpStatus.INTERNAL_SERVER_ERROR);

		String savedFilePath = "";
		try {
			// Saving Zip File to Target Folder
			savedFilePath = s3Service.saveCodeZipFile(zipFileName, targetCodeTypeFolderName,
					releaseFile.getInputStream());
			System.out.println("<>>>>>>>><<<<" + releaseDate);
			CodeMaintenanceFile codeMaintenanceFile = utilsService.prepareCodeMaintenaceFile(codeType, zipFileName,
					savedFilePath, releaseVersion, releaseDate, userId, effectiveFrom, effectiveTo);
			codeMaintenanceFile = codeMaintenanceFileService.saveCodeMaintenanceFile(codeMaintenanceFile);
			codeMaintenanceLoggerService.saveCodeMaintenanceLog(codeMaintenanceFile.getId(), "File Uploading",
					"File Uploaded Successfully", userId);

		} catch (IOException e) {
			e.printStackTrace();
			log.error("Error while Saving Zip File");
			log.error(e.getLocalizedMessage());
			return new CustomResponse("Zip File Uploading Failed", "Error:" + e.getLocalizedMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);

		}

		return new CustomResponse("Zip File Uploaded Successfully", "", HttpStatus.OK);
	}

	@Override
	@Async
	protected void processData(int fileId, int userId) {

		processAndPrepareVerificationData(fileId, userId);

	}

	protected CustomResponse processAndPrepareVerificationData(int fileId, int userId) {

		// TODO Auto-generated method stub

		CodeMaintenanceFile zipFile = codeMaintenanceFileService.getCodeMaintenanceFileById(fileId);

		if (zipFile != null & zipFile.getProcessedState().equalsIgnoreCase("Uploaded")) {
			// log.info("IN IF>>>>>>>>>>>>>>>>>>");

//			if (utilsService.prepareVerificationStatus("version-validation", zipFile.getCodeStandard(),
//					zipFile.getReleaseDate())) {
//				return new CustomResponse("Zip File Processing Failed",
//						"Error:Current Processing File Version is olderthan already existing version",
//						HttpStatus.INTERNAL_SERVER_ERROR);
//			}
			ValidationCheck validationCheck = utilsService.getInprocessFileId("icd",
					java.sql.Date.valueOf(LocalDate.now()));
			if (validationCheck.getId() != 0) {
				utilsService.resetCodeFileSystem(validationCheck.getId(), userId);
			}
			asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Processed", "File Processed Started",
					"In Process", 10, userId);
			zipFile.setProcessedState("InProcess");
			zipFile.setNextState("Verification");
			codeMaintenanceFileService.saveCodeMaintenanceFile(zipFile);
			asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Processed", "Zip File Extracted", "In Process",
					25, userId);
			InputStream zipInputStream = s3Service.getS3FileStream(zipFile.getFilePath());
			Map<String, String> targetCodeDataDetailsMap = getTargetCodeDataFilePath(zipFile.getFileName());
			asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Processed",
					"Zip File Extracted And Read Text File", "In Process", 40, userId);
			parseTargetCodeDataFromFile(zipInputStream, targetCodeDataDetailsMap, fileId, userId);
			// Move the file from upload folder to inprocess folder
			asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Processed",
					"Dump Table Created And Verification Data Prepared ", "In Process", 60, userId);
			String targetCodeStandardFolder = "icd";
			String uploadFilePath = zipFile.getFilePath();
			System.out.println("<<<<<<<<>>>>>>>>>>" + uploadFilePath);

			String dateFormatPath = utilsService.getDateInStringFormat(zipFile.getReleaseDate(), "default");
			String inProcessFilePath = rootFolderName + "/" + inprocessFolderName + "/" + targetCodeStandardFolder + "/"
					+ dateFormatPath + "/" + zipFile.getFileName();
			System.out.println("===========" + inProcessFilePath);
			s3Service.moveFile(uploadFilePath, inProcessFilePath, false);
			asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Processed", "File Moved to Inprocess Folder ",
					"In Process", 80, userId);
			codeMaintenanceFileService.updateCodeMaintenanceFilePathById(fileId, inProcessFilePath);
			asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Processed", "File Processed Completed",
					"Completed", 100, userId);
			return new CustomResponse("ICD Code Maintenance File Processed Successfully", "", HttpStatus.OK);
		}

		return new CustomResponse("ICD Code Maintenance File Process Failed", "", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Override
	protected CustomResponse markAsVerified(int fileId, String verifiedType, int userId) {

		if (verifiedType.equalsIgnoreCase("All")) {
			CodeMaintenanceFile codeMaintenanceFile = codeMaintenanceFileService.getCodeMaintenanceFileById(fileId);
			codeMaintenanceFile.setProcessedState("Verified");
			codeMaintenanceFile.setCurrentStatus("Verified");
			codeMaintenanceFile.setModifiedUserId(userId);
			Timestamp newModifiedDate = Timestamp.valueOf(LocalDateTime.now());
			codeMaintenanceFile.setModifiedDate(newModifiedDate);
			codeMaintenanceFileService.saveCodeMaintenanceFile(codeMaintenanceFile);
		}

		String verificationMessage = verifiedType + " Dataset";
		codeMaintenanceLoggerService.saveCodeMaintenanceLog(fileId, verificationMessage + " marked as Verified",
				"Code Maintenance Data (" + verifiedType + ") Verified", userId);

		return new CustomResponse(verificationMessage + " marked as Verified", "", HttpStatus.OK);
	}

	@Override
	@Async
	protected void syncVerifiedData(int fileId, int userId) {

		syncIcdCodeMaintenanceDataWithExistingData(fileId, userId);
	}

	public Map<String, String> getTargetCodeDataFilePath(String zipFileName) {

		Map<String, String> targetCodeDataFileDetailsMap = new HashMap<>();

		// String tempName = zipFileName.replace("icd10", "icd10cm_").replace(".zip",
		// ""); // icd10cm_2022
		String tempName = "icd10cm_";

		try {
			String pattern = "icd10(\\d{4})"; // Regular expression pattern
			Pattern r = Pattern.compile(pattern);
			Matcher matcher = r.matcher(zipFileName);
			if (matcher.find()) {
				String year = matcher.group(1);
				System.out.println("Extracted year from ICD Zip File Name: " + year);
				tempName = tempName + year;
			} else {
				throw new Exception("Unable to get Target filename from zip");
			}

		} catch (Exception e) {
			System.out.println("Unable to get Target filename from zip");
			e.printStackTrace();
		}
		String targetCodeDataFilePath = zipFileName.replace(".zip", "") + "/" + "dot/" + tempName + "/" + tempName
				+ "_tab.txt";

		targetCodeDataFileDetailsMap.put("targetCodeDataFilePath", targetCodeDataFilePath);
		targetCodeDataFileDetailsMap.put("tempTableName", tempName + "_tab");

		return targetCodeDataFileDetailsMap;
	}

	public void parseTargetCodeDataFromFile(InputStream zipInputStream, Map<String, String> targetCodeDataDetailsMap,
			int fileId, int userId) {

		InputStream targetDataFileStream = null;
		try {
			targetDataFileStream = utilsService.getTargetFileStreamFromZipFile(zipInputStream,
					targetCodeDataDetailsMap.get("targetCodeDataFilePath"));
			List<IcdCodeStandardModel> icdCodeStandardList = prepareTargetCodeEntityFromInputStream(
					targetDataFileStream);
			saveIcdCodeStandardList(icdCodeStandardList, targetCodeDataDetailsMap.get("tempTableName"), fileId, userId);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public List<IcdCodeStandardModel> prepareTargetCodeEntityFromInputStream(InputStream targetDataFileStream)
			throws IOException {

		List<IcdCodeStandardModel> icdCodeStandardList = new LinkedList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(targetDataFileStream))) {
			String line;
			reader.readLine();
			while ((line = reader.readLine()) != null) {

				String[] data = line.split("\t");
				IcdCodeStandardModel entity = new IcdCodeStandardModel();
				entity.setIcdOrder(data[0]);
				entity.setIcdCode(data[1]);
				entity.setIcdId(Integer.parseInt(data[2]));
				entity.setType(data[3].charAt(0));
				entity.setShortDesc(data[4]);
				entity.setMedDesc(data[5]);
				entity.setLongDesc(data[6]);

				icdCodeStandardList.add(entity);

			}
		}
		return icdCodeStandardList;
	}

	public void saveIcdCodeStandardList(List<IcdCodeStandardModel> icdCodeStandardList, String newTableName, int fileId,
			int userId) {

		try {

			utilsService.truncateTable("icd_standard_versions");
			CodeMaintenanceFile codeMaintenanceFile = codeMaintenanceFileService.getCodeMaintenanceFileById(fileId);
			String releaseDate = utilsService.getDateInStringFormat(codeMaintenanceFile.getReleaseDate(), "default");
			newTableName = newTableName + "_" + releaseDate;
			utilsService.dropTable(newTableName);
			icdCodeStandardRepository.saveAll(icdCodeStandardList);
			utilsService.createNewTableFromExisting(newTableName, "icd_standard_versions");
			codeMaintenanceFileService.updateCodeMaintenanceFileStatusById(fileId, "In Process", "Dump Table Created",
					userId);
			codeMaintenanceLoggerService.saveCodeMaintenanceLog(fileId, "Dump Table Created",
					"Dump Table Created Successfully", userId);
//    	    asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "Dump Table Created", "Dump Table Created Successfully", "In Process", 60, userId);
			icdCodeStandardRepository.prepareIcdDataForVerification(fileId, newTableName, userId);
//    	    asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "ICD Verification Data Prepared", "ICD Verification Data Prepared Successfully", "Process Completed", 100, userId);
			codeMaintenanceFileService.updateCodeMaintenanceFileStatusById(fileId, "Pending For Verification",
					"Pending For Verification", userId);
			codeMaintenanceLoggerService.saveCodeMaintenanceLog(fileId, "ICD Verification Data Prepared",
					"ICD Verification Data Prepared Successfully", userId);

		} catch (Exception e) {

			codeMaintenanceLoggerService.saveCodeMaintenanceLog(fileId, "ICD Verification Data Prepared Failed",
					"ICD Verification Data Preparation Failed", userId);
			System.out.println(e.getMessage());

		}

	}

	public CustomResponse syncIcdCodeMaintenanceDataWithExistingData(int fileId, int userId) {

		// IcdSyncResults icdSyncResults=new IcdSyncResults();
		CodeMaintenanceFile codeMaintenanceFile = codeMaintenanceFileService.getCodeMaintenanceFileById(fileId);

		asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Sync", "Code Sync Started", "In Process", 5,
				userId);

		if (codeMaintenanceFile != null) {
			String fileName = codeMaintenanceFile.getFileName();
			String tempName = fileName.replace("icd10", "icd10cm_").replace(".zip", "");
			String targetCodeStandardFolder = "icd";
			String taregtFileName = tempName + "_tab";
			String dateFormatPath = utilsService.getDateInStringFormat(codeMaintenanceFile.getReleaseDate(), "default");
			String inProcessFilePath = rootFolderName + "/" + inprocessFolderName + "/" + targetCodeStandardFolder + "/"
					+ dateFormatPath + "/" + codeMaintenanceFile.getFileName();
			asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Sync", "Code Sync Procedure started",
					"In Process", 30, userId);
//	        icdSynchResultsRepository.icdCompareAndSyncTables(fileId, taregtFileName, userId);
			codeMaintenanceFile.setProcessedState("Syncing InProcess");
			//codeMaintenanceFile.setNextState("");
			codeMaintenanceFileService.saveCodeMaintenanceFile(codeMaintenanceFile);
			try {
				System.out.println("===============Before Adding");
				icdSynchResultsRepository.icdAddedRecordsSync(fileId, inProcessFilePath, userId);
				asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Sync", "Added Code Sync Process Completed",
						"In Process", 50, userId);
				System.out.println("==================After Adding");
				icdSynchResultsRepository.icdUpdatedRecordsSync(fileId, inProcessFilePath, userId);
				asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Sync",
						"Updated Code Sync Process Completed", "In Process", 70, userId);
				System.out.println("===================After Updating");
				icdSynchResultsRepository.icdDeletedRecordsSync(fileId, inProcessFilePath, userId);
				asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Sync",
						"Deleted Code Sync Process Completed", "In Process", 90, userId);
				System.out.println("====================After Deleting");
				asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Sync", "Code Sync process completed",
						"In Process", 95, userId);
				codeMaintenanceLoggerService.saveCodeMaintenanceLog(fileId, "Icd10 Data Synching Completed",
						"Icd10 Data Synching Completed Successfully", userId);
				
				
			} catch (Exception e) {

				System.out.println(e.getMessage());
			}

			utilsService.truncateTable("icd_standard_versions");
			codeMaintenanceFileService.updateCodeMaintenanceFileStatusById(fileId, "Synced", "Syncing completed",
					userId);
			
			codeMaintenanceFile = codeMaintenanceFileService.getCodeMaintenanceFileById(fileId);
			codeMaintenanceFile.setComments("ICD 10 File Proceessed Successfully");
			Timestamp newModifiedDate = Timestamp.valueOf(LocalDateTime.now());
			codeMaintenanceFile.setNextState("View Details");
			codeMaintenanceFile.setModifiedDate(newModifiedDate);
			codeMaintenanceFileService.saveCodeMaintenanceFile(codeMaintenanceFile);
			asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Sync", "Code Sync process Completed",
					"Completed", 100, userId);
			String processedFilePath = rootFolderName + "/" + processedFolderName + "/" + targetCodeStandardFolder + "/"
					+ dateFormatPath + "/" + codeMaintenanceFile.getFileName();
			System.out.println("Source Key:" + inProcessFilePath + "......... Destination Key:" + processedFilePath);
			s3Service.moveFile(inProcessFilePath, processedFilePath, false);
			codeMaintenanceFileService.updateCodeMaintenanceFilePathById(fileId, processedFilePath);
			
			try{
				 System.out.println("Before index====");
				 icdCodeSearchService.createIcdIndex();
				 
			 }
			 catch(Exception e) {
				 e.printStackTrace();
			 }
			return new CustomResponse("ICD Code File Synced Successfully", "", HttpStatus.OK);
		} else {
			// Handle case where codeStandardFile is not found
			asyncTasksStatusService.saveAsyncTaskStatusLog(fileId, "File Sync", "Code Sync process Failed", "Failed", 0,
					userId);
			return new CustomResponse("ICD Code File Sync Failed", "", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public Page<IcdDataVerificationModel> getIcdCodeOrDescOrstatus(Integer fileId, String searchTerm, String status,
			int pageSize, int pageNumber) {
		Pageable paging = PageRequest.of(pageNumber, pageSize);
		List<IcdDataVerificationModel> icdVerificationList = icdDataVerificationRepository
				.getIcdCodeVerificationDetails(fileId, searchTerm, status);
		// System.out.println(">>>>>>>>>>>>>>TESTING>>>>>>>>>>>");
		Page<IcdDataVerificationModel> pagedResult = new PageImpl<>(
				icdVerificationList.subList(Math.min(pageNumber * pageSize, icdVerificationList.size()),
						Math.min((pageNumber + 1) * pageSize, icdVerificationList.size())),
				paging, icdVerificationList.size());
		return pagedResult;
	}

	public Page<IcdPostSyncResultsModel> getIcdPostSyncresults(int fileId, String status, int pageSize,
			int pageNumber) {

		Pageable paging = PageRequest.of(pageNumber, pageSize);
		List<IcdPostSyncResultsModel> icdPostSyncList = icdPostSyncResultsRepository.icdPostSyncDataResults(fileId,
				status);
		Page<IcdPostSyncResultsModel> pagedResult = new PageImpl<>(
				icdPostSyncList.subList(Math.min(pageNumber * pageSize, icdPostSyncList.size()),
						Math.min((pageNumber + 1) * pageSize, icdPostSyncList.size())),
				paging, icdPostSyncList.size());
		return pagedResult;

	}

	public List<TableRecordCounts> getTableRecordCounts() {
		return icdTableRecordCountsRepository.getTableRecordCounts();
	}

	/*
	 * public CustomResponse
	 * saveIcdVerificationLogDetails(List<IcdDataVerificationModel>
	 * icdDataVerificationModel,int userId) {
	 * 
	 * int id=0; List<IcdDataVerificationModel> verifiedList=new LinkedList<>();
	 * //System.out.println(">>>>>>>>>>>>>>>>>TESTING"+icdDataVerificationModel.
	 * toString()); try { for(IcdDataVerificationModel
	 * icdData:icdDataVerificationModel) {
	 * 
	 * id=icdData.getId(); IcdDataVerificationModel model=
	 * icdDataVerificationRepository.getReferenceById(id);
	 * model.setVerificationStatus((short)1);
	 * //icdDataVerificationRepository.save(model); verifiedList.add(model);
	 * 
	 * } }catch(Exception e) { return new
	 * CustomResponse("ICD Verification Log Failed Id:"+id, e.getLocalizedMessage(),
	 * HttpStatus.INTERNAL_SERVER_ERROR); }
	 * //System.out.println("AFTER>>>>"+verifiedList); try {
	 * for(IcdDataVerificationModel verifiedModel: verifiedList) {
	 * 
	 * icdDataVerificationRepository.save(verifiedModel);
	 * 
	 * IcdVerificationLogModel logModel = new IcdVerificationLogModel();
	 * 
	 * logModel.setFileId(verifiedModel.getFileId());
	 * logModel.setCodeset(verifiedModel.getCodeset());
	 * logModel.setCode(verifiedModel.getCode());
	 * 
	 * icdVerificationLogRepository.save(logModel);
	 * 
	 * codeMaintenanceLoggerService.saveCodeMaintenanceLog(verifiedModel.getFileId()
	 * , "code verification",
	 * "Following icd code verified:"+verifiedModel.getIcdCode(), userId); }
	 * }catch(Exception e) { return new
	 * CustomResponse("ICD Verification Log Saving Failed", e.getLocalizedMessage(),
	 * HttpStatus.INTERNAL_SERVER_ERROR); }
	 * 
	 * 
	 * //icdVerificationLogRepository.saveAll(icdDataVerificationModel); return new
	 * CustomResponse("ICD Verification Log Saving Success","", HttpStatus.OK); }
	 */
	public CustomResponse saveCodeVerificationLogDetails(CodeVerification codes, String codeset, int fileId, int userId,
			String notes) {
		CodeMaintenanceFile zipFile = codeMaintenanceFileService.getCodeMaintenanceFileById(fileId);
//		int id=0;
		List<IcdDataVerificationModel> acceptedList = new LinkedList<>();
		List<IcdDataVerificationModel> rejectedList = new LinkedList<>();

		List<String> accepted = codes.getAcceptedCodes();
		List<String> rejected = codes.getRejectedCodes();

		List<IcdDataVerificationModel> verifiedAcceptedList = acceptedVerification(accepted, acceptedList, fileId);
		List<IcdDataVerificationModel> verifiedRejectedList = rejectedVerification(rejected, rejectedList, fileId);

		try {
			saveAccepted(verifiedAcceptedList, codeset, fileId, userId, notes, zipFile);
			saveRejected(verifiedRejectedList, codeset, fileId, userId, notes, zipFile);

		} catch (Exception e) {
			return new CustomResponse("Icd Verification Log Saving Failed", e.getLocalizedMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (rejected.size() > 0) {
			zipFile.setProcessedState("Verification Rejected");
			zipFile.setStatus("Failed");
			zipFile.setNextState("");
			zipFile.setCurrentStatus("Verification Rejected");
			zipFile.setActive(0);
			String targetCodeStandardFolder = "icd";
			String dateFormatPath = utilsService.getDateInStringFormat(zipFile.getReleaseDate(), "default");
			String rejectedFilePath = rootFolderName + "/" + rejectedFolderName + "/" + targetCodeStandardFolder + "/"
					+ dateFormatPath + "/" + zipFile.getFileName();
			System.out.println(">>>>>>>>>>>>>>>>>>>>" + rejectedFilePath);
			String targetFolderPath = rootFolderName + "/" + uploadFolderName + "/" + targetCodeStandardFolder + "/"
					+ zipFile.getFileName();
			System.out.println("+++++++++++" + targetFolderPath);
			s3Service.moveFile(targetFolderPath, rejectedFilePath, true);

			zipFile.setFilePath(rejectedFilePath);
			codeMaintenanceFileService.saveCodeMaintenanceFile(zipFile);
			return new CustomResponse("Icd Verification Rejected",
					"Due to some codes are marked as rejected while verification ", HttpStatus.INTERNAL_SERVER_ERROR);
		} else if (accepted.size() > 0) {
			zipFile.setProcessedState("Verified");
			zipFile.setCurrentStatus("Verification Completed");
			zipFile.setNextState("Sync");
			codeMaintenanceFileService.saveCodeMaintenanceFile(zipFile);
		}

		return new CustomResponse("Icd Verification Success", "", HttpStatus.OK);
	}

	public List<IcdDataVerificationModel> acceptedVerification(List<String> accepted,
			List<IcdDataVerificationModel> acceptedList, int fileId) {

		for (String icdid : accepted) {

			IcdDataVerificationModel model = icdDataVerificationRepository.findByIcdId(Integer.parseInt(icdid));

			if (model != null) {

				model.setVerificationState("Accepted");

				acceptedList.add(model);
//		    		
			}

		}
		return acceptedList;
	}

	public List<IcdDataVerificationModel> rejectedVerification(List<String> rejected,
			List<IcdDataVerificationModel> rejectedList, int fileId) {

//			CodeMaintenanceFile zipFile = codeMaintenanceFileService.getCodeMaintenanceFileById(fileId);

		for (String icdid : rejected) {

			IcdDataVerificationModel model = icdDataVerificationRepository.findByIcdId(Integer.parseInt(icdid));

			if (model != null) {

				model.setVerificationState("Rejected");
				rejectedList.add(model);
			}

		}
		return rejectedList;
	}

	public void saveAccepted(List<IcdDataVerificationModel> verifiedAcceptedList, String codeset, int fileId,
			int userId, String notes, CodeMaintenanceFile zipFile) {
		for (IcdDataVerificationModel verifiedModel : verifiedAcceptedList) {

			icdDataVerificationRepository.save(verifiedModel);

			CodeVerificationLogModel logModel = new CodeVerificationLogModel();

			logModel.setFileId(fileId);
			logModel.setCodeset(codeset);
			if (!verifiedModel.getIcdCode().equalsIgnoreCase("")) {

				logModel.setCode(verifiedModel.getIcdCode());
				System.out.println("IN IFFFF ACCC>>>>>>>>>>>" + verifiedModel.getIcdCode() + ">>>>>>");
				System.out.println(verifiedModel.toString());
			} else {
				logModel.setCode("icdid:" + verifiedModel.getIcdId().toString());
			}

			logModel.setUserId(userId);
			Timestamp newModifiedDate = Timestamp.valueOf(LocalDateTime.now());
			logModel.setInsertedDate(newModifiedDate);
			logModel.setNotes(notes);
			logModel.setVerifiedState("Accepted");
			codeVerificationLogRepository.save(logModel);

			codeMaintenanceLoggerService.saveCodeMaintenanceLog(verifiedModel.getFileId(), "code verification",
					"Following Icd Code verified:" + logModel.getCode(), userId);

		}
	}

	public void saveRejected(List<IcdDataVerificationModel> verifiedRejectedList, String codeset, int fileId,
			int userId, String notes, CodeMaintenanceFile zipFile) {
		for (IcdDataVerificationModel verifiedModel : verifiedRejectedList) {

			icdDataVerificationRepository.save(verifiedModel);
			CodeVerificationLogModel logModel = new CodeVerificationLogModel();
			logModel.setUserId(userId);
			logModel.setFileId(fileId);
			logModel.setCodeset(codeset);
			if (!verifiedModel.getIcdCode().equalsIgnoreCase("")) {
				logModel.setCode(verifiedModel.getIcdCode());
				System.out.println("in rejecttt ifff>>>>>>>>>>>" + verifiedModel.getIcdCode());
			} else {
				logModel.setCode(verifiedModel.getIcdId().toString());
			}
			Timestamp newModifiedDate = Timestamp.valueOf(LocalDateTime.now());
			logModel.setInsertedDate(newModifiedDate);
			logModel.setNotes(notes);
			logModel.setVerifiedState("Rejected");
			codeVerificationLogRepository.save(logModel);

			codeMaintenanceLoggerService.saveCodeMaintenanceLog(verifiedModel.getFileId(), "code verification",
					"Following Icd Code rejected:" + logModel.getCode(), userId);
		}
	}

	public Page<IcdPostSyncResultsModel> getIcdSearchByAfterSync(Integer fileId, String searchTerm, String status,
			int pageSize, int pageNumber) {
		Pageable paging = PageRequest.of(pageNumber, pageSize);
		// TODO Auto-generated method stub
		List<IcdPostSyncResultsModel> icdSyncList = icdPostSyncResultsRepository.getIcdSearchByAfterSync(fileId,
				searchTerm, status);
		// System.out.println(">>>>>>>>>>>>>>TESTING>>>>>>>>>>>");
		Page<IcdPostSyncResultsModel> pagedResult = new PageImpl<>(
				icdSyncList.subList(Math.min(pageNumber * pageSize, icdSyncList.size()),
						Math.min((pageNumber + 1) * pageSize, icdSyncList.size())),
				paging, icdSyncList.size());
		return pagedResult;

	}

	public Page<IcdPostSyncResultsModel> getIcdVerificationSearch(String searchTerm, Pageable pageable) {
		// TODO Auto-generated method stub
		return icdPostSyncResultsRepository.getIcdVerificationSearch(searchTerm, pageable);
	}

}
