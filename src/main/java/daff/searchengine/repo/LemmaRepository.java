package daff.searchengine.repo;


import daff.searchengine.models.LemmaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    List<LemmaEntity> findAllByIdIn(List<Integer> lemmaIds);
    List<LemmaEntity> findAllByLemmaInAndSiteId(Set<String> lemmasRequest, int siteId);
    void deleteAllBySiteId(int id);
}

