package com.io.codesystem.allergies;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.io.codesystem.medicine.MedicinePostSyncResultsModel;
import com.io.codesystem.utils.TableRecordCounts;

@RestController
public class AllergiesCodeMaintenanceController {

	@Autowired
	private AllergiesCodeMaintenanceService allergiesCodeMaintenanceService;

	/*
	 * @GetMapping(path = "/verificationsearch/allergys") public
	 * ResponseEntity<Page<AllergysDataVerificationModel>>
	 * getAllergysdamConceptIdDescOrdamAlrgnGrpDesc(
	 * 
	 * @RequestParam Integer fileId,
	 * 
	 * @RequestParam String damConceptIdDesc,
	 * 
	 * @RequestParam String damAlrgnGrpDesc,
	 * 
	 * @RequestParam String status,
	 * 
	 * @RequestParam int pageSize,
	 * 
	 * @RequestParam int pageNumber){
	 * 
	 * HttpHeaders headers = new HttpHeaders(); Page<AllergysDataVerificationModel>
	 * response = allergysCodeMaintenanceService
	 * .getAllergysdamConceptIdDescOrdamAlrgnGrpDesc(fileId, damConceptIdDesc,
	 * damAlrgnGrpDesc, status, pageSize, pageNumber);
	 * 
	 * return new ResponseEntity<>(response, headers, HttpStatus.OK);
	 * 
	 * }
	 */

	@GetMapping(path = "/verificationsearch/allergies")
	public ResponseEntity<Page<AllergiesDataVerificationModel>> getAllergiesdamConceptIdDescOrdamAlrgnGrpDesc(
			@RequestParam Integer fileId, @RequestParam String searchTerm, @RequestParam String status,
			@RequestParam int pageSize, @RequestParam int pageNumber)

	{

		HttpHeaders headers = new HttpHeaders();
		Page<AllergiesDataVerificationModel> response = allergiesCodeMaintenanceService
				.getAllergiesdamConceptIdDescOrdamAlrgnGrpDesc(fileId, searchTerm, status, pageSize, pageNumber);
		return new ResponseEntity<>(response, headers, HttpStatus.OK);

	}

	/*
	 * @GetMapping("/postsyncresults/allergys") public
	 * ResponseEntity<List<PostSyncAllergysResultsModel>>
	 * getAllergysPostSyncResults(@RequestParam int fileId,
	 * 
	 * @RequestParam String status) { List<PostSyncAllergysResultsModel> response =
	 * allergysCodeMaintenanceService.getAllergysPostSyncResults(fileId, status);
	 * HttpHeaders headers = new HttpHeaders(); return new
	 * ResponseEntity<>(response, headers, HttpStatus.OK);
	 * 
	 * }
	 */

	@GetMapping("/postsyncresults/allergies")
	public ResponseEntity<Page<PostSyncAllergiesResultsModel>> getAllergiesPostSyncResults(@RequestParam int fileId,
			@RequestParam String status, @RequestParam int pageSize, @RequestParam int pageNumber) {
		Page<PostSyncAllergiesResultsModel> response = allergiesCodeMaintenanceService
				.getAllergiesPostSyncResults(fileId, status, pageSize, pageNumber);

		System.out.print(response.toString());

		HttpHeaders headers = new HttpHeaders();
		return new ResponseEntity<>(response, headers, HttpStatus.OK);

	}

	@GetMapping("/tableRecordCounts/allergies")
	public ResponseEntity<List<TableRecordCounts>> getTableRecordCounts() {
		List<TableRecordCounts> tableRecordCounts = allergiesCodeMaintenanceService.getTableRecordCounts();
		HttpHeaders headers = new HttpHeaders();
		return new ResponseEntity<>(tableRecordCounts, headers, HttpStatus.OK);

	}

	@GetMapping("/aftersync/search/allergies")
	public ResponseEntity<Page<PostSyncAllergiesResultsModel>> getAllergiesSearchByAfterSync(
			@RequestParam Integer fileId, @RequestParam String searchTerm, @RequestParam String status,
			@RequestParam int pageSize, @RequestParam int pageNumber) {

		HttpHeaders headers = new HttpHeaders();
		Page<PostSyncAllergiesResultsModel> Response = allergiesCodeMaintenanceService
				.getAllergiesSearchByAfterSync(fileId, searchTerm, status, pageSize, pageNumber);
		return new ResponseEntity<>(Response, headers, HttpStatus.OK);
	}
	
	@GetMapping("/verfication/allergiesSearch")
	public ResponseEntity<Page<PostSyncAllergiesResultsModel>> getAllerfiesVerificationSearch(
			@RequestParam String searchTerm, @RequestParam int pageSize, @RequestParam int pageNumber) {
		Pageable pageable = PageRequest.of(pageNumber, pageSize);
		HttpHeaders headers = new HttpHeaders();
		Page<PostSyncAllergiesResultsModel> medResponse = allergiesCodeMaintenanceService
				.getAllerfiesVerificationSearch(searchTerm, pageable);
		return new ResponseEntity<>(medResponse, headers, HttpStatus.OK);

	}

}
