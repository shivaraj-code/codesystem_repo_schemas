package com.io.codesystem.allergies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AllergiesSyncResultsRepo extends JpaRepository<AllergiesSyncResults, Integer> {

	@Query(value = "CALL AllergiesCompareAndSyncTablesAdded(:file_id,:file_name,:user_id)", nativeQuery = true)
	public AllergiesSyncResults allergiesCompareAndSyncTablesForAdded(Integer file_id, String file_name,
			Integer user_id);

	@Query(value = "CALL AllergiesCompareAndSyncTablesBatchUpdatedWithConcat(:file_id,:file_name,:user_id)", nativeQuery = true)
	public AllergiesSyncResults allergiesCompareAndSyncTablesForUpdated(Integer file_id, String file_name,
			Integer user_id);

	// @Query(value="CALL
	// AllergysCompareAndSyncTablesUpdated(:file_id,:file_name,:user_id)",nativeQuery=true)
	// public AllergysSyncResults allergysCompareAndSyncTablesForUpdated(Integer
	// file_id,String file_name,Integer user_id);

	// @Query(value="CALL
	// AllergysCompareAndSyncTablesForFastUpdatedWithConcat(:file_id,:file_name,:user_id)",nativeQuery=true)
	// public AllergysSyncResults allergysCompareAndSyncTablesForUpdated(Integer
	// file_id,String file_name,Integer user_id);

	// @Query(value="CALL
	// AllergysCompareAndSyncTablesUpdatedWithConcat(:file_id,:file_name,:user_id)",nativeQuery=true)
	// public AllergysSyncResults allergysCompareAndSyncTablesForUpdated(Integer
	// file_id,String file_name,Integer user_id);

	@Query(value = "CALL AllergiesCompareAndSyncTablesDeleted(:file_id,:file_name,:user_id)", nativeQuery = true)
	public AllergiesSyncResults allergiesCompareAndSyncTablesForDeleted(Integer file_id, String file_name,
			Integer user_id);

}
