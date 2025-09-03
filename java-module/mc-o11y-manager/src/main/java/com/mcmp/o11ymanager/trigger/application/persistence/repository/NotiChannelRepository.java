package com.mcmp.o11ymanager.trigger.application.persistence.repository;

import com.mcmp.o11ymanager.trigger.application.persistence.model.NotiChannel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotiChannelRepository extends JpaRepository<NotiChannel, Long> {
    List<NotiChannel> findByNameIn(List<String> names);
}
