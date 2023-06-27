package daff.searchengine.repo;


import daff.searchengine.models.PageEntity;
import daff.searchengine.models.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Query("SELECT COUNT(p) FROM PageEntity p WHERE p.site.id = :siteId")
    Integer getCountBySite(int siteId);
    void deleteAllBySiteId(int siteId);
    Optional<PageEntity> findByPathAndSite(String path, SiteEntity site);
    List<PageEntity> findByIdIn(List<Integer> ids);
}
