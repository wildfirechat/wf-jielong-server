package cn.wildfirchat.service;

import cn.wildfirchat.dto.CollectionDetailResponse;
import cn.wildfirchat.dto.CreateCollectionRequest;
import cn.wildfirchat.dto.EntryVO;
import cn.wildfirchat.dto.ErrorCode;
import cn.wildfirchat.entity.Collection;
import cn.wildfirchat.entity.CollectionEntry;
import cn.wildfirchat.entity.CollectionLog;
import cn.wildfirchat.exception.BizException;
import cn.wildfirchat.repository.CollectionEntryRepository;
import cn.wildfirchat.repository.CollectionLogRepository;
import cn.wildfirchat.repository.CollectionRepository;
import cn.wildfirechat.pojos.PojoGroupMember;
import cn.wildfirechat.sdk.GroupAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CollectionService {

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private CollectionEntryRepository entryRepository;

    @Autowired
    private CollectionLogRepository logRepository;

    @Autowired
    private IMMessageService imMessageService;

    /**
     * 创建接龙
     */
    @Transactional
    public Collection createCollection(String groupId, String creatorId,
                                       CreateCollectionRequest request) {
        // 检查用户是否在群组中
        checkUserInGroup(groupId, creatorId);

        Collection collection = new Collection();
        collection.setGroupId(groupId);
        collection.setCreatorId(creatorId);
        collection.setTitle(request.getTitle());
        collection.setDescription(request.getDescription());
        collection.setTemplate(request.getTemplate());
        collection.setExpireType(request.getExpireType());
        collection.setExpireAt(request.getExpireAt());
        collection.setMaxParticipants(request.getMaxParticipants());
        collection.setStatus(0);

        collectionRepository.save(collection);

        // 记录日志
        saveLog(collection.getId(), creatorId, 1, null);

        // 发送接龙消息到群聊
        List<CollectionEntry> entries = entryRepository
                .findByCollectionIdAndDeletedOrderByCreatedAtAsc(collection.getId(), 0);
        imMessageService.sendCollectionMessage(collection, entries, creatorId);

        return collection;
    }

    /**
     * 参与或编辑接龙（悲观锁版）
     */
    @Transactional
    public void joinOrUpdate(Long collectionId, String userId, String groupId, String content) {
        // 查询并锁定接龙记录（悲观锁，其他事务会阻塞等待）
        Collection collection = collectionRepository.findByIdForUpdate(collectionId)
                .orElseThrow(() -> new BizException(ErrorCode.COLLECTION_NOT_FOUND));

        // 校验状态
        validateCollection(collection);

        // 检查用户是否在群组中
        checkUserInGroup(groupId, userId);

        // 查询用户是否已有记录
        CollectionEntry entry = entryRepository
                .findByCollectionIdAndUserId(collectionId, userId);

        if (entry == null) {
            // 新参与 - 检查人数上限
            if (collection.getMaxParticipants() != null && collection.getMaxParticipants() > 0) {
                long count = entryRepository.countByCollectionIdAndDeleted(collectionId, 0);
                if (count >= collection.getMaxParticipants()) {
                    throw new BizException(ErrorCode.COLLECTION_FULL);
                }
            }

            entry = new CollectionEntry();
            entry.setCollectionId(collectionId);
            entry.setUserId(userId);
            entry.setContent(content);
            entry.setDeleted(0);
            entryRepository.save(entry);

            // 记录日志
            saveLog(collectionId, userId, 2, content);

        } else if (entry.getDeleted() == 1) {
            // 之前删除过，恢复并更新
            entry.setContent(content);
            entry.setDeleted(0);
            entryRepository.save(entry);

            saveLog(collectionId, userId, 2, content);

        } else {
            // 编辑已有记录
            entry.setContent(content);
            entryRepository.save(entry);

            saveLog(collectionId, userId, 3, content);
        }

        // 发送接龙消息到群聊
        List<CollectionEntry> entries = entryRepository
                .findByCollectionIdAndDeletedOrderByCreatedAtAsc(collectionId, 0);
        imMessageService.sendCollectionMessage(collection, entries, userId);
    }

    /**
     * 删除自己的参与（悲观锁版）
     */
    @Transactional
    public void deleteEntry(Long collectionId, String userId) {
        // 查询并锁定接龙记录
        Collection collection = collectionRepository.findByIdForUpdate(collectionId)
                .orElseThrow(() -> new BizException(ErrorCode.COLLECTION_NOT_FOUND));

        // 校验状态
        validateCollection(collection);

        // 查询记录
        CollectionEntry entry = entryRepository
                .findByCollectionIdAndUserId(collectionId, userId);

        if (entry == null || entry.getDeleted() == 1) {
            throw new BizException(ErrorCode.ENTRY_NOT_FOUND);
        }

        // 软删除
        entry.setDeleted(1);
        entryRepository.save(entry);

        // 记录日志
        saveLog(collectionId, userId, 4, null);

        // 发送接龙消息到群聊
        List<CollectionEntry> entries = entryRepository
                .findByCollectionIdAndDeletedOrderByCreatedAtAsc(collectionId, 0);
        imMessageService.sendCollectionMessage(collection, entries, userId);
    }

    /**
     * 关闭接龙（仅创建者）
     */
    @Transactional
    public void closeCollection(Long collectionId, String operatorId) {
        // 查询并锁定
        Collection collection = collectionRepository.findByIdForUpdate(collectionId)
                .orElseThrow(() -> new BizException(ErrorCode.COLLECTION_NOT_FOUND));

        if (!collection.getCreatorId().equals(operatorId)) {
            throw new BizException(ErrorCode.NO_PERMISSION);
        }

        collection.setStatus(1);
        collectionRepository.save(collection);

        // 关闭接龙后也发送消息通知
        List<CollectionEntry> entries = entryRepository
                .findByCollectionIdAndDeletedOrderByCreatedAtAsc(collectionId, 0);
        imMessageService.sendCollectionMessage(collection, entries, operatorId.toString());
    }

    /**
     * 获取接龙详情
     */
    @Transactional(readOnly = true)
    public CollectionDetailResponse getCollection(Long collectionId) {
        // 只读事务不需要加锁
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new BizException(ErrorCode.COLLECTION_NOT_FOUND));

        List<CollectionEntry> entries = entryRepository
                .findByCollectionIdAndDeletedOrderByCreatedAtAsc(collectionId, 0);

        CollectionDetailResponse response = new CollectionDetailResponse();
        response.setId(collection.getId());
        response.setGroupId(collection.getGroupId());
        response.setCreatorId(collection.getCreatorId());
        response.setTitle(collection.getTitle());
        response.setDescription(collection.getDescription());
        response.setTemplate(collection.getTemplate());
        response.setExpireType(collection.getExpireType() != null ? collection.getExpireType() : 0);
        response.setExpireAt(collection.getExpireAt() != null ? collection.getExpireAt() : 0L);
        response.setMaxParticipants(collection.getMaxParticipants() != null ? collection.getMaxParticipants() : 0);
        response.setStatus(collection.getStatus() != null ? collection.getStatus() : 0);
        response.setCreatedAt(collection.getCreatedAt() != null ? collection.getCreatedAt() : 0L);
        response.setUpdatedAt(collection.getUpdatedAt() != null ? collection.getUpdatedAt() : 0L);

        List<EntryVO> entryVOs = entries.stream().map(e -> {
            EntryVO vo = new EntryVO();
            vo.setId(e.getId() != null ? e.getId() : 0L);
            vo.setCollectionId(e.getCollectionId() != null ? e.getCollectionId() : 0L);
            vo.setUserId(e.getUserId());
            vo.setContent(e.getContent());
            vo.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt() : 0L);
            vo.setUpdatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt() : 0L);
            vo.setDeleted(e.getDeleted() != null ? e.getDeleted() : 0);
            return vo;
        }).collect(Collectors.toList());

        response.setEntries(entryVOs);
        response.setParticipantCount(entryVOs.size());

        return response;
    }

    /**
     * 校验接龙是否可操作
     */
    private void validateCollection(Collection collection) {
        if (collection.getStatus() == 1) {
            throw new BizException(ErrorCode.COLLECTION_CLOSED);
        }
        if (collection.getStatus() == 2) {
            throw new BizException(ErrorCode.COLLECTION_CANCELLED);
        }
        if (collection.getExpireType() != null && collection.getExpireType() == 1) {
            if (collection.getExpireAt() != null &&
                    collection.getExpireAt() < System.currentTimeMillis()) {
                throw new BizException(ErrorCode.COLLECTION_EXPIRED);
            }
        }
    }

    /**
     * 保存操作日志
     */
    private void saveLog(Long collectionId, String userId, int actionType, String content) {
        CollectionLog logEntry = new CollectionLog();
        logEntry.setCollectionId(collectionId);
        logEntry.setUserId(userId);
        logEntry.setActionType(actionType);
        logEntry.setContent(content);
        logRepository.save(logEntry);
    }

    /**
     * 检查用户是否在群组中
     * <p>如果IM服务不可用，采取降级策略：允许操作继续（记录警告日志）</p>
     * @param groupId 群组ID
     * @param userId 用户ID
     * @throws BizException 如果用户明确不在群组中
     */
    private void checkUserInGroup(String groupId, String userId) {
        int maxRetries = 2;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= maxRetries) {
            try {
                IMResult<PojoGroupMember> result = GroupAdmin.getGroupMember(groupId, userId);
                
                // IM服务返回成功
                if (result != null && result.getErrorCode() == cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS) {
                    PojoGroupMember member = result.getResult();
                    // 检查成员是否被移除（type 为 4 表示已被移除）
                    if (member == null || member.getType() == 4) {
                        log.warn("用户已被移除群组, groupId: {}, userId: {}", groupId, userId);
                        throw new BizException(ErrorCode.NOT_IN_GROUP);
                    }
                    // 用户正常在群组中
                    return;
                }
                
                // 处理明确的业务错误
                if (result != null) {
                    cn.wildfirechat.common.ErrorCode errorCode = result.getErrorCode();
                    // 用户明确不在群组中（NOT_EXIST 或 NOT_IN_GROUP）
                    if (errorCode == cn.wildfirechat.common.ErrorCode.ERROR_CODE_NOT_EXIST ||
                        errorCode == cn.wildfirechat.common.ErrorCode.ERROR_CODE_NOT_IN_GROUP) {
                        log.warn("用户不在群组中, groupId: {}, userId: {}, errorCode: {}", 
                                groupId, userId, errorCode);
                        throw new BizException(ErrorCode.NOT_IN_GROUP);
                    }
                    // 其他业务错误（如群组不存在等），记录后降级处理
                    log.warn("IM服务返回业务错误, groupId: {}, userId: {}, errorCode: {}", 
                            groupId, userId, errorCode);
                    // 降级：允许操作继续
                    return;
                }
                
                // result为null，可能是网络问题，重试
                log.warn("IM服务返回空结果, 准备重试, groupId: {}, userId: {}, retry: {}/{}", 
                        groupId, userId, retryCount + 1, maxRetries);
                
            } catch (BizException e) {
                // 业务异常直接抛出，不重试
                throw e;
            } catch (Exception e) {
                lastException = e;
                log.warn("调用IM服务发生异常, 准备重试, groupId: {}, userId: {}, retry: {}/{}， error: {}", 
                        groupId, userId, retryCount + 1, maxRetries, e.getMessage());
            }
            
            retryCount++;
            if (retryCount <= maxRetries) {
                // 短暂延迟后重试（指数退避）
                try {
                    Thread.sleep(100 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // 所有重试都失败，采取降级策略：允许操作继续，但记录警告日志
        log.error("IM服务检查群组失败已达最大重试次数，采取降级策略允许操作继续, groupId: {}, userId: {}", 
                groupId, userId, lastException);
        // 降级处理：不阻止用户操作
    }
}
