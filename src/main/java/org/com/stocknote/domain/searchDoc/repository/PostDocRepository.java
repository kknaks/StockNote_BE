package org.com.stocknote.domain.searchDoc.repository;

import org.com.stocknote.domain.searchDoc.document.PostDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PostDocRepository extends ElasticsearchRepository<PostDoc, String> {
  @Query("{\"match\": {\"title\": {\"query\": \"?0\"}}}")
  Page<PostDoc> searchByTitle(String keyword, Pageable pageable);

  @Query("{\"match\": {\"body\": {\"query\": \"?0\"}}}")
  Page<PostDoc> searchByContent(String keyword, Pageable pageable);

  @Query("{\"match\": {\"member_doc.name\": {\"query\": \"?0\"}}}")
  Page<PostDoc> searchByUsername(String keyword, Pageable pageable);

  @Query("{\"match\": {\"hashtags\": {\"query\": \"?0\"}}}")
  Page<PostDoc> searchByHashtag(String keyword, Pageable pageable);

  @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"title^3\", \"body\", \"member_doc.name^2\", \"hashtags^2\"]}}")
  Page<PostDoc> searchByAll(String keyword, Pageable pageable);

  @Query("{\"bool\": {\"should\": [{\"match\": {\"title\": \"?0\"}}, {\"match\": {\"hashtags\": \"?0\"}}]}}")
  PostDoc searchByTitleOrHashtag(String keyword);

  @Query("{\"bool\": {\"should\": [" + "{\"match\": {\"title\": \"?0\"}},"
      + "{\"match\": {\"hashtags\": \"?0\"}}" + "]}}")
  boolean existsByTitleOrHashtagsContaining(String keyword);

  @Query("{\"bool\": {\"must\": [" + "{\"match\": {\"title\": \"?0\"}},"
      + "{\"term\": {\"category.keyword\": \"?1\"}}" + "]}}")
  Page<PostDoc> searchByTitleAndCategory(String keyword, String category, Pageable pageable);

  @Query("{\"bool\": {\"must\": [" + "{\"match\": {\"body\": \"?0\"}},"
      + "{\"term\": {\"category.keyword\": \"?1\"}}" + "]}}")
  Page<PostDoc> searchByContentAndCategory(String keyword, String category, Pageable pageable);

  @Query("{\"bool\": {\"must\": [" + "{\"match\": {\"member_doc.name\": \"?0\"}},"
      + "{\"term\": {\"category.keyword\": \"?1\"}}" + "]}}")
  Page<PostDoc> searchByUsernameAndCategory(String keyword, String category, Pageable pageable);

  @Query("{\"bool\": {\"must\": [" + "{\"match\": {\"hashtags\": \"?0\"}},"
      + "{\"term\": {\"category.keyword\": \"?1\"}}" + "]}}")
  Page<PostDoc> searchByHashtagAndCategory(String keyword, String category, Pageable pageable);

  @Query("{\"bool\": {\"must\": [" + "{\"multi_match\": {" + "\"query\": \"?0\","
      + "\"fields\": [\"title^3\", \"body\", \"member_doc.name^2\", \"hashtags^2\"]" + "}},"
      + "{\"term\": {\"category.keyword\": \"?1\"}}" + "]}}")
  Page<PostDoc> searchByAllAndCategory(String keyword, String category, Pageable pageable);
}
