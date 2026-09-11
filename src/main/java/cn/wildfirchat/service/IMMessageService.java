package cn.wildfirchat.service;

import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.MessageAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirchat.entity.Collection;
import cn.wildfirchat.entity.CollectionEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class IMMessageService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送接龙消息到群聊
     *
     * @param collection 接龙信息
     * @param entries    参与列表
     * @param operatorId 操作者ID
     */
    public void sendCollectionMessage(Collection collection, List<CollectionEntry> entries, String operatorId) {
        try {
            // 构建消息内容
            String title = collection.getTitle();
            String desc = collection.getDescription();

            // 构建参与列表文本（用于推送）
            StringBuilder entriesText = new StringBuilder();
            int showCount = Math.min(entries.size(), 5);
            for (int i = 0; i < showCount; i++) {
                CollectionEntry e = entries.get(i);
                entriesText.append(i + 1).append(". ")
                        .append("用户").append(e.getUserId())
                        .append(" ").append(e.getContent())
                        .append("\n");
            }

            if (entries.size() > 5) {
                entriesText.append("...还有").append(entries.size() - 5).append("人参与\n");
            }

            // 构建完整消息文本（用于推送）
            StringBuilder messageText = new StringBuilder();
            messageText.append("【接龙】").append(title).append("\n");
            if (desc != null && !desc.isEmpty()) {
                messageText.append(desc).append("\n");
            }
            messageText.append(entriesText);
            messageText.append("共 ").append(entries.size()).append(" 人参与");

            // 构建 MessagePayload
            MessagePayload payload = new MessagePayload();
            payload.setType(17);  // 接龙消息类型
            payload.setSearchableContent(title);  // 标题用于搜索
            payload.setPushContent(messageText.toString());  // 推送内容
            payload.setPersistFlag(3);

            // 构建 JSON 数据（与 iOS 客户端 encode 方法格式一致）
            Map<String, Object> dataDict = new HashMap<>();
            dataDict.put("collectionId", String.valueOf(collection.getId()));
            dataDict.put("groupId", collection.getGroupId());
            dataDict.put("creatorId", collection.getCreatorId());
            if (desc != null) {
                dataDict.put("desc", desc);
            }
            if (collection.getTemplate() != null) {
                dataDict.put("template", collection.getTemplate());
            }
            dataDict.put("expireType", collection.getExpireType() == null ? 0 : collection.getExpireType());
            dataDict.put("expireAt", collection.getExpireAt() == null ? 0L : collection.getExpireAt());
            dataDict.put("maxParticipants", collection.getMaxParticipants() == null ? 0 : collection.getMaxParticipants());
            dataDict.put("status", collection.getStatus());
            dataDict.put("createdAt", collection.getCreatedAt());
            dataDict.put("updatedAt", collection.getUpdatedAt());

            // entries 数组
            List<Map<String, Object>> entriesArray = new ArrayList<>();
            for (CollectionEntry entry : entries) {
                Map<String, Object> entryDict = new HashMap<>();
                entryDict.put("userId", entry.getUserId());
                entryDict.put("content", entry.getContent());
                entryDict.put("createdAt", entry.getCreatedAt());
                entriesArray.add(entryDict);
            }
            dataDict.put("entries", entriesArray);

            // 将 JSON 转为字符串放入 content（与 iOS 客户端 binaryContent 对应）
            String jsonStr = objectMapper.writeValueAsString(dataDict);
            payload.setBase64edData(Base64.getEncoder().encodeToString(jsonStr.getBytes(StandardCharsets.UTF_8)));

            // 构建群会话
            Conversation conversation = new Conversation();
            conversation.setType(1);  // 1 = 群聊
            conversation.setTarget(collection.getGroupId());
            conversation.setLine(0);  // 默认线路

            // 发送群消息
            IMResult<SendMessageResult> result = MessageAdmin.sendMessage(
                    operatorId,
                    conversation,
                    payload
            );

            if (result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                log.info("接龙消息发送成功, collectionId: {}, messageUid: {}",
                        collection.getId(), result.getResult().getMessageUid());
            } else {
                log.error("接龙消息发送失败, collectionId: {}, errorCode: {}",
                        collection.getId(), result == null ? "null" : result.getErrorCode());
            }

        } catch (Exception e) {
            log.error("发送接龙消息异常, collectionId: " + collection.getId(), e);
        }
    }
}
