package com.io.codesystem.allergies;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AllergiesPostSyncResultsRepo extends JpaRepository<PostSyncAllergiesResultsModel, Integer> {

	@Query(value = "CALL GetAllergiesPostSyncData(:file_id,:status)", nativeQuery = true)
	public List<PostSyncAllergiesResultsModel> allergiesPostSyncDataResults(Integer file_id, String status);

	@Query(value = "CALL GetAllergiesAfterSyncDetails(:fileId,:searchTerm,:status)", nativeQuery = true)
	public List<PostSyncAllergiesResultsModel> getAllergiesSearchByAfterSync(Integer fileId, String searchTerm,
			String status);
	
	@Query("select a from PostSyncAllergiesResultsModel a where(a.damConceptIdDesc like %:searchTerm% or a.damAlrgnGrpDesc like %:searchTerm%) and versionState='Validated'")
	public Page<PostSyncAllergiesResultsModel> getAllerfiesVerificationSearch(String searchTerm, Pageable pageable);

}
