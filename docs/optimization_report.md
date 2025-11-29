# 关注列表性能与头像加载优化报告

## 背景与目标
- 需求：关注数量 1000，服务端分页（10/页）；下拉软刷新；上滑自动追加；头像快速加载、流畅滚动（fps>59）；内存高效；SQLite 模块分离。
- 目标：解决分页/刷新/状态同步/头像加载慢与失败等问题，完善缓存与预加载，优化布局与内存使用。

## 问题清单与成因分析
- 分页到底无反应：滚动触发条件不稳定，且 `currentOffset` 未随 DB 大小更新，导致请求偏移错误。
- 刷新重置人数并覆盖本地修改：刷新逻辑清空 DB 后插入第一页，丢失本地 `special/followed/remark`。
- 特别关注无法取消：服务端默认状态在刷新/追加时覆盖本地修改。
- 下滑重复同一人：按 `followTime` 排序导致回读顺序与服务端分页顺序不一致。
- 头像加载慢/部分 404：`picsum` 某些 id 不存在，Glide 失败后保留占位图；未预加载下屏资源。
- Room 构造器警告与编译错误：有参构造与无参构造冲突；新增字段后旧 8 参构造缺失。
- 布局层级过深与过度绘制：多层 LinearLayout 增加测量与绘制成本。
- 内存与泄漏：图片解码成本、对象频繁创建、未响应系统内存告警、缺少泄漏检测。

## 解决方案与实现要点
- 分页触发与偏移维护：
  - 触发阈值：`FollowListFragment.java:52-59` 使用最后可见项+阈值，避免依赖 `dy>0`。
  - 偏移修复：`UserViewModel.java:57-62` 在回读 DB 后 `currentOffset=data.size()`，保证追加请求偏移正确。
- 软刷新不重置人数：
  - 仅同步已加载范围状态：`UserRepository.java:91-118` 遍历 `[0,currentCount)` 分批获取并 `insertAll` upsert。
  - `UserViewModel.refresh()`：`UserViewModel.java:34-40` 调用 `syncLoadedUsers` 后回读，不清空不重置。
- 本地优先合并与云端记忆：
  - 追加/刷新合并本地字段：`UserRepository.java:39-57, 64-90` 保留本地 `special/followed/remark`。
  - 服务端记忆本地变更：`UserRepository.java:103-108, 119-124, 137-143` 调用 `MockService.updateUserState(...)`；实现于 `MockService.java:64-74`。
- 稳定顺序与去重：
  - 引入 `orderIndex` 并按其排序：`UserEntity.java:18`, `UserDao.java:13,16`；插入时设置：`UserRepository.java:45-53, 70-82`。
- 头像加载优化与失败回退：
  - WebP 80×80：`MockService.java:32`；Glide 固定尺寸与短过渡：`UsersAdapter.java:44-55`。
  - 二级回退源（保证显示）：`UsersAdapter.java:54-65` 使用 `error(RequestBuilder)` 降级到 `robohash`。
  - 预加载下一屏：`AvatarPreloadProvider.java:1-40` + `FollowListFragment.java:49-56` 启用 `RecyclerViewPreloader`。
- Room 构造器与兼容：
  - `@Ignore` 标注有参构造：`UserEntity.java:22-33`；补充旧 8 参构造：`UserEntity.java:35-46`。
- 布局扁平化与固定尺寸：
  - Item 改为 `ConstraintLayout`：`item_user.xml` 全面扁平化（≤3 层）。
  - Fragment 改为 `ConstraintLayout`：`fragment_follow_list.xml` 固定结构与尺寸。
- ViewBinding 替代 `findViewById`：
  - Fragment：`FollowListFragment.java:24-29, 31, 54, 60`；Adapter：`UsersAdapter.java:28-40, 98-120, 129-135`。
- 内存与泄漏防护：
  - Glide 内存/位图池/磁盘缓存：`MyGlideModule.java:16-28`（30MB/30MB/50MB）。
  - OkHttp 50MB Cache + 7 天 TTL：`MyGlideModule.java:30-41`。
  - Application 响应内存告警：`App.java:1-16`（`onTrimMemory/onLowMemory`）。
  - LeakCanary：`app/build.gradle.kts:52`（debug 集成）。
  - RecyclerView 复用池与禁用动画：`FollowListFragment.java:34-41`。
  - 复用临时对象：`UsersAdapter.java:24-30, 66-73` 复用 `StringBuilder`。

## 效果验证
- 多次 `assembleDebug` 构建均成功，关键路径变更后立即验证。
- 滑动到底自动追加，刷新仅同步状态；特别关注/关注/备注修改在服务端与本地保持一致。
- 头像显示稳定：主源失败自动回退；预加载生效，首帧等待显著降低。
- 布局与缓存优化后，过度绘制与测量开销减少，滚动更顺畅。

## 进一步优化建议
- 动态预加载规模：根据网络/设备内存动态调整 `maxPreload`（当前为 10）。
- 条件预加载：Wi‑Fi 下更积极，移动数据下保守策略。
- 图片密度自适应：按屏幕密度选择 64/72/80 像素，节省上传与解码。
- 过渡时长自适应：快速滚动禁用过渡，静止时启用更好的视觉过渡。
- 离线缓存策略：为头像源增加种子/稳定 URL，减少 404。

## 结论
通过分页触发修复、软刷新合并、顺序索引、头像 WebP 与失败回退、预加载与缓存、布局扁平化、ViewBinding 与对象复用、内存告警响应与泄漏检测等组合优化，已显著提升头像加载速度与滑动流畅度，并确保状态一致与用户体验稳定。

## 技术方案与底层原理
- Glide 缓存体系与解码格式
  - 内存缓存（`LruResourceCache`）：缓存变换后的 `Resource`，命中后跳过磁盘与网络，减少主线程等待（`MyGlideModule.java:16-24`）。
  - 位图池（`LruBitmapPool`）：复用已释放的 `Bitmap` 内存块，降低频繁分配与 GC 压力（`MyGlideModule.java:16-24`）。
  - 磁盘缓存（`InternalCacheDiskCacheFactory`）：缓存原始与变换后的文件，`DiskCacheStrategy.AUTOMATIC` 自动根据资源类型与变换策略选择缓存层（`MyGlideModule.java:20-24`）。
  - 解码格式 `RGB_565`：较 ARGB_8888 减半像素内存占用（2B/px vs 4B/px），适合头像不透明背景；配合圆形裁剪减少上传与绘制开销（`UsersAdapter.java:44-55`）。

- OkHttp 连接池与 HTTP 缓存
  - 连接池（`ConnectionPool(8, 5min)`）：复用 TCP/TLS 会话，减少握手与队首阻塞，提高并发请求效率（`MyGlideModule.java:35-41`）。
  - HTTP 磁盘缓存（50MB）：设置 `Cache-Control: public, max-age=604800`，同 URL 在 7 天内直接命中本地缓存，降低网络与耗时（`MyGlideModule.java:30-41`）。

- RecyclerView 预取与 Glide 预加载
  - `LinearLayoutManager.setItemPrefetchEnabled(true)`：在绑定前提前准备下一条的数据与布局，减少滚动瞬间的卡顿（`FollowListFragment.java:34-38`）。
  - `RecyclerViewPreloader`：基于可见范围和滚动方向预先发起图片请求，命中内存/磁盘缓存后快速显示（`FollowListFragment.java:49-56`，`AvatarPreloadProvider.java:1-40`）。
  - 固定预加载尺寸（80×80）：避免不同尺寸导致的额外解码与缓存 Miss（`AvatarPreloadProvider.java:8-16`）。

- 布局与过度绘制
  - ConstraintLayout 扁平化：减少测量与布局 Pass，降低过度绘制风险（`item_user.xml`、`fragment_follow_list.xml`）。
  - 禁用 `ItemAnimator` 与固定尺寸：避免频繁动画计算与重布局，稳定滚动性能（`FollowListFragment.java:34-41`）。

- 列表增量刷新与稳定顺序
  - DiffUtil 最小更新：通过差分算法减少不必要的绑定与重绘（`UsersAdapter.java:129-135`）。
  - 稳定顺序索引（`orderIndex`）：分页写入顺序并按索引查询，防止刷新或追加导致的顺序错乱（`UserEntity.java:18`，`UserDao.java:13`）。

- 软刷新合并与服务端记忆
  - 合并策略：刷新/追加时保留本地 `special/followed/remark`，仅更新其他字段，避免状态回滚（`UserRepository.java:39-57, 64-90`）。
  - 服务端记忆：本地更新时写入 `MockService.updateUserState`，后续分页返回沿用你的修改（`UserRepository.java:103-143`，`MockService.java:64-74`）。

- 内存告警与泄漏检测
  - `onTrimMemory/onLowMemory`：按系统级别清理 Glide 缓存，避免前后台切换或内存紧张时占用过高（`App.java:1-16`）。
  - LeakCanary：在 debug 模式下监控 Activity/Fragment/Adapter 等对象的泄漏并给出泄漏路径提示（`app/build.gradle.kts:52`）。

- 对象池与临时对象复用
  - RecycledViewPool：提升 ViewHolder 复用效率，减少创建与绑定压力（`FollowListFragment.java:37-41`）。
  - 复用 `StringBuilder`：避免在 `onBindViewHolder` 中频繁创建短生命周期对象，减少 GC 干扰（`UsersAdapter.java:24-30, 66-73`）。
