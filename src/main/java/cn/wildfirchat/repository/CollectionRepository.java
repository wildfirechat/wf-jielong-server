package cn.wildfirchat.repository;

import cn.wildfirchat.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {

    /**
     * 查询并加排他锁（悲观锁）
     * SELECT FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Collection c WHERE c.id = :id")
    Optional<Collection> findByIdForUpdate(@Param("id") Long id);
}
