package cn.wildfirchat.controller;

import cn.wildfirchat.dto.*;
import cn.wildfirchat.entity.Collection;
import cn.wildfirchat.filter.AuthFilter;
import cn.wildfirchat.service.CollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/collections")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    /**
     * 创建接龙
     */
    @PostMapping
    public Result<Collection> create(@RequestBody CreateCollectionRequest request,
                                      HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        Collection collection = collectionService.createCollection(request.getGroupId(), userId, request);
        return Result.success(collection);
    }

    /**
     * 参与或编辑接龙
     */
    @PostMapping("/{collectionId}/join")
    public Result<Void> joinOrUpdate(@PathVariable Long collectionId,
                                      @RequestBody JoinCollectionRequest request,
                                      HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        collectionService.joinOrUpdate(collectionId, userId, request.getGroupId(), request.getContent());
        return Result.success();
    }

    /**
     * 删除自己的参与
     */
    @PostMapping("/{collectionId}/delete")
    public Result<Void> delete(@PathVariable Long collectionId,
                                @RequestBody DeleteCollectionRequest request,
                                HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        // request.getGroupId() 可用于后续校验用户是否在该群
        collectionService.deleteEntry(collectionId, userId);
        return Result.success();
    }

    /**
     * 关闭接龙（创建者）
     */
    @PostMapping("/{collectionId}/close")
    public Result<Void> close(@PathVariable Long collectionId,
                               @RequestBody CloseCollectionRequest request,
                               HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        // request.getGroupId() 可用于后续校验用户是否在该群
        collectionService.closeCollection(collectionId, userId);
        return Result.success();
    }

    /**
     * 获取接龙详情（POST方法）
     */
    @PostMapping("/{collectionId}/detail")
    public Result<CollectionDetailResponse> getDetail(@PathVariable Long collectionId,
                                                       @RequestBody GetCollectionDetailRequest request) {
        // request.getGroupId() 可用于后续校验用户是否在该群
        CollectionDetailResponse detail = collectionService.getCollection(collectionId);
        return Result.success(detail);
    }
}
