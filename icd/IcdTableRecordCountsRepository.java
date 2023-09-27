package com.io.codesystem.icd;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.io.codesystem.utils.TableRecordCounts;

@Repository
public interface IcdTableRecordCountsRepository  extends JpaRepository<TableRecordCounts, Integer>{
	
	@Query(value="select 1 as id, 'Existing Table' as table_name,count(*) as records_count from icdnew "
			+ "union "
			+ "select 2 as id, 'Dump Table' as table_name,count(*) as records_count from icd_standard_versions",nativeQuery=true)
	public List<TableRecordCounts> getTableRecordCounts();

}
