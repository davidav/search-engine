package daff.searchengine.repo;

import daff.searchengine.models.IndexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Query("SELECT SUM(i.rank) FROM IndexEntity i")
    Integer getSumRank();
    @Query("SELECT SUM(i.rank) FROM IndexEntity i WHERE i.pageId IN " +
            "(SELECT p.id FROM PageEntity p WHERE p.site.id = :id) AND i.lemmaId IN" +
            "(SELECT l.id FROM LemmaEntity l WHERE l.site.id = :id)")
    Integer getSumRankBySite(int id);
    List<IndexEntity> findAllByPageId(int id);
    List<IndexEntity> findAllByLemmaIdIn(List<Integer> ids);
    void deleteAllByPageId(int id);

}

