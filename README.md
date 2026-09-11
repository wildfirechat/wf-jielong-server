# wf-collection 接龙功能模块

基于Spring Boot + JPA实现的聊天软件接龙功能模块。

## 项目结构

```
wf-collection/
├── src/main/java/cn/wildfirchat/
│   ├── CollectionApplication.java          # 启动类
│   ├── controller/
│   │   └── CollectionController.java       # 接口控制器
│   ├── service/
│   │   └── CollectionService.java          # 业务逻辑
│   ├── repository/
│   │   ├── CollectionRepository.java       # 接龙数据访问
│   │   ├── CollectionEntryRepository.java  # 参与记录数据访问
│   │   └── CollectionLogRepository.java    # 操作日志数据访问
│   ├── entity/
│   │   ├── Collection.java                 # 接龙实体
│   │   ├── CollectionEntry.java            # 参与记录实体
│   │   └── CollectionLog.java              # 操作日志实体
│   ├── dto/
│   │   ├── CreateCollectionRequest.java    # 创建接龙请求
│   │   ├── JoinCollectionRequest.java      # 参与接龙请求
│   │   ├── CollectionDetailResponse.java   # 接龙详情响应
│   │   ├── EntryVO.java                    # 参与记录VO
│   │   └── Result.java                     # 统一返回结果
│   └── exception/
│       ├── BizException.java               # 业务异常
│       └── GlobalExceptionHandler.java     # 全局异常处理
├── src/main/resources/
│   └── application.yml                     # 配置文件
└── pom.xml                                 # Maven配置
```

## 技术栈

- Spring Boot 2.7.14
- Spring Data JPA
- H2 Database（开发测试）/ MySQL（生产）
- Lombok
- Apache Commons Lang3

## 启动项目

```bash
cd wf-collection
mvn spring-boot:run
```

启动后访问：http://localhost:8088

H2控制台：http://localhost:8088/h2-console
- JDBC URL: `jdbc:h2:mem:wfcollection`
- 用户名: `sa`
- 密码: 空

## API接口

### 1. 创建接龙

```http
POST /api/collections
Headers:
  X-User-Id: 1001
  X-Group-Id: 2001
Body:
{
  "title": "周末聚餐报名",
  "description": "本周六晚上聚餐，请大家报名",
  "template": "姓名-人数-忌口",
  "expireType": 1,
  "expireAt": "2025-02-15T18:00:00",
  "maxParticipants": 20
}
```

### 2. 参与/编辑接龙

```http
POST /api/collections/{collectionId}/join
Headers:
  X-User-Id: 1002
Body:
{
  "content": "张三-2人-不吃辣"
}
```

### 3. 删除参与

```http
POST /api/collections/{collectionId}/delete
Headers:
  X-User-Id: 1002
```

### 4. 关闭接龙

```http
POST /api/collections/{collectionId}/close
Headers:
  X-User-Id: 1001
```

### 5. 获取接龙详情

```http
POST /api/collections/{collectionId}/detail
```

## 业务规则

| 规则 | 说明 |
|------|------|
| 消息聚合 | 每次变更发送一条新的聚合消息（日志输出） |
| 编辑权限 | 只能编辑自己的条目 |
| 删除权限 | 只能删除自己的条目 |
| 用户限制 | 一个用户只能有一条记录 |
| 序号显示 | 仅前端展示用，无实际存储序号 |
| 记录标识 | 每条记录用user_id标识 |
| 创建者权限 | 创建者不能修改他人参与记录 |
| 有效期 | 无限期可随时修改；有限期过期后不能修改 |
| 并发控制 | 使用数据库悲观锁（SELECT FOR UPDATE） |

## 数据库表

- `collection` - 接龙主表
- `collection_entry` - 接龙参与记录（用户唯一）
- `collection_log` - 操作日志记录

## 打包部署

```bash
mvn clean package
java -jar target/wf-collection-1.0.0.jar
```
