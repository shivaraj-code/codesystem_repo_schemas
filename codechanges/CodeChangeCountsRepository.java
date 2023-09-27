package com.io.codesystem.codechanges;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeChangeCountsRepository extends JpaRepository<CodeChangeCounts, Integer> {

	public CodeChangeCounts findByStatus(String status);

}
