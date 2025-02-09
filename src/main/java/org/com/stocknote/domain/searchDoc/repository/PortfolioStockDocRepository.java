package org.com.stocknote.domain.searchDoc.repository;

import org.com.stocknote.domain.member.entity.Member;
import org.com.stocknote.domain.searchDoc.document.MemberDoc;
import org.com.stocknote.domain.searchDoc.document.PortfolioDoc;
import org.com.stocknote.domain.searchDoc.document.PortfolioStockDoc;
import org.com.stocknote.domain.searchDoc.document.StockDoc;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PortfolioStockDocRepository extends ElasticsearchRepository<PortfolioStockDoc, String> {

  @Query("""
      {
        "match": {
          "member_id": ?0
        }
      }
  """)
  List<PortfolioStockDoc> findByMemberId(Long id);
}
