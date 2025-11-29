# 关注列表 App 概览

## 项目概览
- 目标：实现 1000 人关注列表，服务端分页（10/页），下拉软刷新、上滑自动加载更多；头像快速加载与稳定显示；流畅滚动（fps>59）；内存高效；数据库模块化。
- 平台：Android（Java, Room, Glide, OkHttp）。

## 核心功能
- 分页加载：上滑到底自动追加下一页数据（`UserViewModel.java:43-55`）。
- 下拉软刷新：不重置人数与列表，仅同步已加载范围状态（`UserViewModel.java:34-40`）。
- 头像加载：WebP 固定尺寸，缓存与预加载，失败自动回退，淡入过渡与圆形裁剪（`UsersAdapter.java:44-55`，`FollowListFragment.java:49-56`）。
- 状态管理：关注、特别关注、备注更新后本地优先与服务端记忆（`UserRepository.java:103-108, 119-124, 137-143`）。

## 架构与代码结构
- 数据层
  - `network/MockService.java`：模拟分页 API 与服务端状态记忆（`app/src/main/java/com/example/java/network/MockService.java:49,64`）。
  - `db/AppDatabase.java`、`db/UserDao.java`、`db/UserEntity.java`：Room 持久化，按 `orderIndex` 排序（`app/src/main/java/com/example/java/db/UserDao.java:13`; `app/src/main/java/com/example/java/db/UserEntity.java:18`）。
- 领域逻辑
  - `UserRepository.java`：分页加载、软刷新同步、本地优先合并与服务端记忆（`app/src/main/java/com/example/java/UserRepository.java:39,64,91,103,137`）。
  - `UserViewModel.java`：分页偏移与加载状态管理（`app/src/main/java/com/example/java/UserViewModel.java:34,43,57`）。
- 展示层
  - `FollowListFragment.java`：列表、预加载、滚动监听与刷新（`app/src/main/java/com/example/java/FollowListFragment.java:34,49,52`）。
  - `UsersAdapter.java`：项渲染、Glide 加载、DiffUtil（`app/src/main/java/com/example/java/UsersAdapter.java:44,54,129`）。
- 图片与内存
  - `glide/MyGlideModule.java`：Glide 全局配置与 OkHttp 连接池/缓存（`app/src/main/java/com/example/java/glide/MyGlideModule.java:16,30`）。
  - `glide/AvatarPreloadProvider.java`：RecyclerView 预加载（`app/src/main/java/com/example/java/glide/AvatarPreloadProvider.java:1-40`）。
  - `App.java`：响应系统内存告警，清理 Glide 缓存（`app/src/main/java/com/example/java/App.java:1-16`）。

## 核心流程
- 初次进入：`refresh()` 若列表为空则拉取首批 10 条；否则软刷新同步状态（`UserViewModel.java:34-45`）。
- 上滑到底：触发 `loadNextPage()` 按 `currentOffset` 追加下一页并更新偏移（`UserViewModel.java:43-55`）。
- 状态更新：调用仓库更新数据库并同步服务端记忆，回读刷新 UI（`UserRepository.java:103-143`）。

## 性能优化
- 图片
  - WebP+固定尺寸 80×80（`MockService.java:32`），Glide `RGB_565`、短淡入过渡、圆形裁剪（`UsersAdapter.java:44-55`）。
  - 失败回退到 `robohash`，避免长时间默认头像（`UsersAdapter.java:54-65`）。
  - 预加载下一屏头像（`FollowListFragment.java:49-56`）。
- 列表与布局
  - 预取与缓存：`setItemPrefetchEnabled(true)`、`setInitialPrefetchItemCount(10)`、`setHasFixedSize(true)`、`setItemViewCacheSize(20)`、禁用 `ItemAnimator`（`FollowListFragment.java:34-41`）。
  - 扁平化布局：`item_user.xml`、`fragment_follow_list.xml` 使用 ConstraintLayout，≤3 层，控件固定尺寸。
  - ViewBinding 替代 `findViewById`（`FollowListFragment.java:24-29, 31, 54, 60`；`UsersAdapter.java:28-40, 98-120, 129-135`）。
- 数据与顺序
  - `orderIndex` 保持分页追加顺序稳定（`UserEntity.java:18`; `UserRepository.java:45`; `UserDao.java:13`）。
  - 软刷新：仅同步 `[0, currentCount)`，不清空、不重置人数（`UserRepository.java:91-118`）。
- 缓存与内存
  - Glide 内存/位图池/磁盘缓存（30MB/30MB/50MB），OkHttp 50MB Cache + 7 天 TTL（`MyGlideModule.java:16-41`）。
  - Application 响应 `onTrimMemory/onLowMemory`（`App.java:1-16`）。
  - 对象复用：`StringBuilder` 与 RecycledViewPool（`UsersAdapter.java:24-30`；`FollowListFragment.java:37-41`）。

## 技术难点与解决方案
- 分页无反应：偏移未更新导致覆盖 → 回读后 `currentOffset = data.size()`。
- 刷新覆盖本地修改：清空 DB 重插导致丢失 → 软刷新遍历 `[0,currentCount)` 分批 upsert 保留本地字段。
- 特别关注无法取消：服务端默认覆盖 → 更新时同步到服务端记忆 `updateUserState`，刷新不再覆盖。
- 重复用户观感：按时间排序乱序 → 引入 `orderIndex` 并按其排序。
- 头像失败/慢：源站部分 id 404 → 主源 WebP+预加载，失败回退 `robohash`。
- Room 构造器冲突：无参与有参冲突 → `@Ignore` 标注有参并补旧 8 参构造。
- 布局过度绘制：多层 LinearLayout → ConstraintLayout 扁平化与固定尺寸。

## 本地运行
- 构建：`./gradlew.bat assembleDebug`
- 安装到模拟器：`./gradlew.bat installDebug`
- 首次运行建议在 Wi‑Fi 环境以充分利用预加载与缓存。

## 目录概览
- `app/src/main/java/com/example/java/` 业务代码
- `app/src/main/java/com/example/java/db/` Room 数据库
- `app/src/main/java/com/example/java/network/` 模拟服务端
- `app/src/main/java/com/example/java/glide/` Glide 配置与预加载
- `app/src/main/res/layout/` 布局文件（扁平化）
- `docs/optimization_report.md` 优化分析与技术说明

## 维护建议
- 结合 Profiler 与 LeakCanary 观察特定设备上的内存与帧率，动态微调：预加载数量、缓存大小、图片尺寸与过渡时长。
- 可加条件预加载策略（Wi‑Fi 更积极、移动网络保守）。
- 按屏幕密度自适配头像尺寸，进一步降低编码与上传耗时。
