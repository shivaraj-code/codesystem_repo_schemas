package com.io.codesystem.icd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IcdSyncResultsRepository extends JpaRepository<IcdSyncResultsModel, Integer> {

//	@Query(value="CALL IcdCompareAndSyncTables(:file_id,:file_name,:user_id)",nativeQuery=true)
//	public IcdSyncResultsModel icdCompareAndSyncTables(Integer file_id,String file_name,Integer user_id);

	@Query(value = "CALL IcdAddedRecordsSync(:file_id,:file_name,:user_id)", nativeQuery = true)
	public IcdSyncResultsModel icdAddedRecordsSync(Integer file_id, String file_name, Integer user_id);


	@Query(value = "CALL IcdUpdatedRecordsSync(:file_id,:file_name,:user_id)", nativeQuery = true)
	public IcdSyncResultsModel icdUpdatedRecordsSync(Integer file_id, String file_name, Integer user_id);

	@Query(value = "CALL IcdDeletedRecordsSync(:file_id,:file_name,:user_id)", nativeQuery = true)
	public IcdSyncResultsModel icdDeletedRecordsSync(Integer file_id, String file_name, Integer user_id);
}
