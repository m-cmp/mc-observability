package com.mcmp.o11ymanager.repository;

import com.mcmp.o11ymanager.entity.AccessInfoEntity;
import org.springdoc.core.providers.JavadocProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessInfoJpaRepository extends JpaRepository<AccessInfoEntity, String> {

}
