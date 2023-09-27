package com.io.codesystem.cpt;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CptPostSyncResultsRepository extends JpaRepository<CptPostSyncResultsModel, Integer> {

	@Query(value = "CALL GetCptPostSyncData(:file_id,:status)", nativeQuery = true)
	public List<CptPostSyncResultsModel> cptPostSyncDataResults(Integer file_id, String status);

	@Query(value= "CALL GetCptAfterSyncDetails(:fileId,:searchTerm,:status)",nativeQuery = true)
	public List<CptPostSyncResultsModel> getCptSearchByAfterSync(Integer fileId, String searchTerm,
			String status);

	@Query("select c from CptPostSyncResultsModel c where(c.code like %:searchTerm% or c.shortDesc like %:searchTerm% )and versionState='Validated'")
	public Page<CptPostSyncResultsModel> getCptVerificationSearch(String searchTerm, Pageable pageable);

}