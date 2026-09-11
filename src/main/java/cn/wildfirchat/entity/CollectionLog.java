package cn.wildfirchat.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "collection_log")
public class CollectionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collection_id", nullable = false)
    private Long collectionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "action_type", nullable = false)
    private Integer actionType; // 1=创建 2=参与 3=编辑 4=删除

    @Column(length = 500)
    private String content;

    @Column(name = "created_at", updatable = false)
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = System.currentTimeMillis();
    }
}
