package cn.wildfirchat.dto;

import lombok.Data;

@Data
public class EntryVO {
    private long id;
    private long collectionId;
    private String userId;
    private String content;
    private long createdAt;
    private long updatedAt;
    private int deleted;
}
