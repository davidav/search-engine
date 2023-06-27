package daff.searchengine.repo;


import daff.searchengine.models.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    Optional<SiteEntity> findByUrl(String url);

    @Query("SELECT COUNT(s) FROM SiteEntity s WHERE s.status = 'INDEXING' OR s.status = 'FAILED'")
    int getCountNoIndexedSites();

}


