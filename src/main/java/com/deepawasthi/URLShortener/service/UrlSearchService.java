package com.deepawasthi.URLShortener.service;

import com.deepawasthi.URLShortener.model.Url;
import com.deepawasthi.URLShortener.model.UrlSearchDocument;
import com.deepawasthi.URLShortener.repository.UrlSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UrlSearchService {

    private static final Logger log = LoggerFactory.getLogger(UrlSearchService.class);

    private final UrlSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public UrlSearchService(UrlSearchRepository searchRepository,
                            ElasticsearchOperations elasticsearchOperations) {
        this.searchRepository = searchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void index(Url url) {
        try {
            searchRepository.save(new UrlSearchDocument(url));
        } catch (Exception e) {
            log.warn("Failed to index URL in Elasticsearch: {}", e.getMessage());
        }
    }

    public List<UrlSearchDocument> search(String query) {
        try {
            return searchRepository.findByOriginalUrlContainingIgnoreCase(query);
        } catch (Exception e) {
            log.warn("Elasticsearch search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void removeIndex(String id) {
        try {
            searchRepository.deleteById(id);
        } catch (Exception e) {
            log.warn("Failed to remove URL from Elasticsearch index: {}", e.getMessage());
        }
    }
}
