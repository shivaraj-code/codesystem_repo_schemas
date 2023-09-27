package com.io.codesystem.cpt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CptSyncResultsRepository extends JpaRepository<CptSyncResultsModel, Integer> {

	@Query(value = "CALL CptCompareAndSyncAdded(:file_id,:file_name,:user_id)", nativeQuery = true)
	public CptSyncResultsModel cptCompareAndSyncTablesAdded(Integer file_id, String file_name, Integer user_id);

	@Query(value = "CALL CptCompareAndSyncUpdated(:file_id,:file_name,:user_id)", nativeQuery = true)
	public CptSyncResultsModel cptCompareAndSyncTablesUpdated(Integer file_id, String file_name, Integer user_id);

	@Query(value = "CALL CptCompareAndSyncDeleted(:file_id,:file_name,:user_id)", nativeQuery = true)
	public CptSyncResultsModel cptCompareAndSyncTablesDeleted(Integer file_id, String file_name, Integer user_id);

	// public void cptCompareAndSyncTablesAdded(int fileId, String taregtFileName,
	// int userId);

}