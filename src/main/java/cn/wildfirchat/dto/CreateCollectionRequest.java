package cn.wildfirchat.dto;

import lombok.Data;

@Data
public class CreateCollectionRequest {
    private String groupId;
    private String title;
    private String description;
    private String template;
    private int expireType; // 0=无限期 1=有限期
    private long expireAt;
    private int maxParticipants;
}
