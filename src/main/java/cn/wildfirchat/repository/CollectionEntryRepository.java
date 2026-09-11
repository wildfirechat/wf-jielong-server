package cn.wildfirchat.repository;

import cn.wildfirchat.entity.CollectionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionEntryRepository extends JpaRepository<CollectionEntry, Long> {

    CollectionEntry findByCollectionIdAndUserId(Long collectionId, String userId);

    List<CollectionEntry> findByCollectionIdAndDeletedOrderByCreatedAtAsc(Long collectionId, Integer deleted);

    long countByCollectionIdAndDeleted(Long collectionId, Integer deleted);
}
