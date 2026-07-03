package com.deepawasthi.URLShortener.repository;

import com.deepawasthi.URLShortener.model.UrlSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlSearchRepository extends ElasticsearchRepository<UrlSearchDocument, String> {
    List<UrlSearchDocument> findByOriginalUrlContainingIgnoreCase(String query);
    List<UrlSearchDocument> findByShortCode(String shortCode);
}
