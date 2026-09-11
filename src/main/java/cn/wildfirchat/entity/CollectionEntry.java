package cn.wildfirchat.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "collection_entry", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"collection_id", "user_id"}, name = "uk_collection_user")
})
public class CollectionEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collection_id", nullable = false)
    private Long collectionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column
    private Integer deleted; // 0=正常 1=已删除

    @Column(name = "created_at", updatable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis();
        createdAt = now;
        updatedAt = now;
        if (deleted == null) {
            deleted = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }
}
