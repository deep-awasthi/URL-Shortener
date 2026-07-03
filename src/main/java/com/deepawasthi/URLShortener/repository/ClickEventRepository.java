package com.deepawasthi.URLShortener.repository;

import com.deepawasthi.URLShortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findTop100ByShortCodeOrderByClickedAtDesc(String shortCode);
    long countByShortCode(String shortCode);
}
