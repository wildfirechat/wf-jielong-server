package cn.wildfirchat.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "collection")
public class Collection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false, length = 64)
    private String groupId;

    @Column(name = "creator_id", nullable = false, length = 64)
    private String creatorId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String template;

    @Column(name = "expire_type")
    private Integer expireType; // 0=无限期 1=有限期

    @Column(name = "expire_at")
    private Long expireAt;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column
    private Integer status; // 0=进行中 1=已结束 2=已取消

    @Column(name = "created_at", updatable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = 0;
        }
        if (expireType == null) {
            expireType = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }
}
