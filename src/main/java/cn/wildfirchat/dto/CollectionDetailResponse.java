package cn.wildfirchat.dto;

import lombok.Data;

import java.util.List;

@Data
public class CollectionDetailResponse {
    private long id;
    private String groupId;
    private String creatorId;
    private String title;
    private String description;
    private String template;
    private int expireType;
    private long expireAt;
    private int maxParticipants;
    private int status;
    private long createdAt;
    private long updatedAt;
    private int participantCount;
    private List<EntryVO> entries;
}
