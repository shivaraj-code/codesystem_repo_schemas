package com.io.codesystem.icd;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface IcdPostSyncResultsRepository  extends JpaRepository<IcdPostSyncResultsModel, Integer> {

	@Query(value="CALL GetIcdPostSyncData(:file_id,:status)",nativeQuery=true)
	public List<IcdPostSyncResultsModel> icdPostSyncDataResults(Integer file_id,String status);
	
	@Query(value="CALL GetIcdCodeVerificationDetailsAfterSync(:fileid,:searchterm,:status)",nativeQuery=true)
	public List<IcdPostSyncResultsModel> getIcdSearchByAfterSync(Integer fileid,String searchterm,String status);

	@Query("select m from IcdPostSyncResultsModel m where(m.icd10id like %:searchTerm% or m.icd10code like %:searchTerm%)and versionState='Validated'")
	public Page<IcdPostSyncResultsModel> getIcdVerificationSearch(String searchTerm, Pageable pageable);
}
