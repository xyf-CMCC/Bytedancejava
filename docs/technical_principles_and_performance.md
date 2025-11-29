# 技术原理说明与性能分析方案

## 目标与范围
- 详细阐述项目使用到的底层技术方案与优化原理（网络、图片、列表、数据库、内存）。
- 提供系统化的性能分析方法与度量指标，支撑持续优化与回归验证。

## 图片加载管线与优化原理
- 请求路径
  - Glide → OkHttp（连接池/HTTP缓存）→ 服务器（WebP 80×80）。
  - 命中策略：内存缓存优先，其次磁盘缓存，再次网络请求。
- Glide 三层缓存
  - 内存缓存（`LruResourceCache`）：缓存转换后的 `Resource`，命中后直接渲染，避免磁盘/网络延迟（`app/src/main/java/com/example/java/glide/MyGlideModule.java:16-24`）。
  - 位图池（`LruBitmapPool`）：复用 `Bitmap` 内存块，降低 GC 压力与频繁分配（`MyGlideModule.java:16-24`）。
  - 磁盘缓存（`InternalCacheDiskCacheFactory`）：缓存原始/转换文件，`DiskCacheStrategy.AUTOMATIC` 自动选择合适的缓存层（`MyGlideModule.java:20-24`）。
- 解码格式优化
  - `RGB_565`：每像素 2 字节，较 `ARGB_8888`（4 字节/像素）减半内存；适合头像场景（透明性要求低）（`MyGlideModule.java:20-24`）。
  - 固定尺寸 `80×80`：解码与上传成本稳定，避免尺寸不一致导致的缓存 Miss（`app/src/main/java/com/example/java/UsersAdapter.java:44-55`）。
- 失败回退策略
  - 主源：`picsum.photos/id/<seed>/80/80.webp`（可能偶发 404）。
  - 回退：`robohash.org/<id>.png?size=80x80&set=set3` 保证有图（`app/src/main/java/com/example/java/UsersAdapter.java:54-65`）。
- 预加载机制
  - RecyclerViewPreloader：根据滚动方向与可见范围，预先发起下一屏图片请求，缩短首帧（`app/src/main/java/com/example/java/FollowListFragment.java:49-56`；`app/src/main/java/com/example/java/glide/AvatarPreloadProvider.java:1-40`）。

## 网络层与缓存原理
- OkHttp 连接池
  - `ConnectionPool(8, 5min)`：复用 TCP/TLS 会话，降低握手开销，提高并发吞吐（`MyGlideModule.java:35-41`）。
- HTTP 磁盘缓存
  - 50MB Cache + `Cache-Control: public, max-age=604800`：同 URL 7 天内命中本地缓存，显著降低网络往返（`MyGlideModule.java:30-41`）。
- 请求降载
  - 固定尺寸 + 预加载 + 本地回退：减少无效重试与长时间占位图。

## 列表渲染与滚动原理
- 预取与复用
  - `setItemPrefetchEnabled(true)` + `setInitialPrefetchItemCount(10)`：在绑定前准备下一条数据与布局（`app/src/main/java/com/example/java/FollowListFragment.java:34-38`）。
  - `RecycledViewPool`：提升 ViewHolder 复用效率，减少创建绑定成本（`FollowListFragment.java:37-41`）。
- 布局扁平化
  - ConstraintLayout 替代多层 LinearLayout，≤3 层结构，固定尺寸，降低测量与绘制开销（`app/src/main/res/layout/item_user.xml`，`app/src/main/res/layout/fragment_follow_list.xml`）。
- 最小刷新
  - DiffUtil：只更新变化项，避免整列表重绘（`app/src/main/java/com/example/java/UsersAdapter.java:129-135`）。

## 分页、软刷新与顺序稳定
- 分页触发
  - 使用“最后可见项 + 阈值”判断到底部（`FollowListFragment.java:52-59`）。
  - `currentOffset = data.size()`：回读后偏移与条数一致，避免覆盖（`app/src/main/java/com/example/java/UserViewModel.java:57-62`）。
- 顺序稳定
  - `orderIndex`：插入时写入顺序索引，查询按索引排序（`app/src/main/java/com/example/java/db/UserEntity.java:18`；`app/src/main/java/com/example/java/db/UserDao.java:13`；`app/src/main/java/com/example/java/UserRepository.java:45-53`）。
- 软刷新合并
  - `syncLoadedUsers(currentCount)`：遍历 `[0, currentCount)` 分批 upsert，仅同步状态与头像数据，人数不变（`app/src/main/java/com/example/java/UserRepository.java:91-118`；`app/src/main/java/com/example/java/UserViewModel.java:34-40`）。
- 本地优先与服务端记忆
  - 合并本地 `special/followed/remark`，更新时写入 `MockService.updateUserState`，刷新不会覆盖（`app/src/main/java/com/example/java/UserRepository.java:39-57, 64-90, 103-143`；`app/src/main/java/com/example/java/network/MockService.java:64-74`）。

## 数据库与实体构造
- Room 实体
  - 无参构造 + 有参构造 `@Ignore`，避免 Room 选择错误构造器；兼容 8/9 参数（`app/src/main/java/com/example/java/db/UserEntity.java:20-46`）。
- 索引字段
  - `orderIndex` 用于稳定展示顺序，避免时间排序导致的乱序与“重复观感”。

## 内存管理与泄漏防护
- Glide 内存告警响应
  - `onTrimMemory/onLowMemory`：按系统级别清理内存与缓存（`app/src/main/java/com/example/java/App.java:1-16`）。
- LeakCanary
  - Debug 集成，监控 Activity/Fragment/Adapter 泄漏并给出泄漏路径（`app/build.gradle.kts:52`）。
- 临时对象复用
  - Adapter 中复用 `StringBuilder`，减少 GC 压力（`app/src/main/java/com/example/java/UsersAdapter.java:24-30, 66-73`）。

## 性能分析方案
- 目标指标（KPI）
  - 滑动帧率：`> 59 fps`（无明显掉帧）。
  - 头像首帧时间：可见后 `<= 120ms`；预加载命中时 `<= 60ms`。
  - 内存占用：滚动场景稳定，峰值不突增；GC 次数合理。
  - 网络请求：同一头像 URL 在 7 天 TTL 内基本命中缓存；失败回退率低。
- 工具与方法
  - Android Profiler：CPU/GPU/Memory、Network 面板采样滚动阶段与加载阶段。
  - LeakCanary：运行交互后观察是否存在典型泄漏（Activity/Fragment 持有）。
  - 日志埋点：分页触发次数、刷新同步耗时、图片加载耗时（Glide Listener 可选）。
- 采样步骤
  1. 首次进入关注页：记录首屏加载时间与头像首帧时间。
  2. 连续上滑 5 屏：记录每屏平均帧率、头像首帧、GC 次数与耗时分布。
  3. 下拉软刷新：记录同步耗时与 UI 恢复时间；验证人数/顺序不变。
  4. 弱网/移动网络：观察 HTTP 缓存命中率与回退触发比例。
- 调优参数（按采样结果微调）
  - 预加载规模：`maxPreload` 10 → 15/20；网络/内存压力增大时回调。
  - Glide 缓存大小：内存/位图池/磁盘（30/30/50MB）按设备能力调整。
  - 图片尺寸与过渡：`80×80` → `64×64`；过渡时间 `80ms` → `50ms` 或快速滚动禁用过渡。
  - RecyclerView 缓存：`setItemViewCacheSize(20)` 与 `setInitialPrefetchItemCount(10)` 适度调整。

## 风险与保障
- 风险：过度预加载导致内存与网络压力升高；过渡动画在弱机上可能造成绘制抖动。
- 保障：
  - 条件预加载（Wi‑Fi 更积极、移动网络保守）。
  - 统一固定图片尺寸，避免缓存碎片化。
  - 按采样数据逐步微调参数，保持稳定体验。

