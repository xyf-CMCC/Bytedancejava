# 项目说明：关注/粉丝管理演示 App

本项目演示了常见的关注列表交互与数据持久化，包括：左右滑动标签页、返回按钮与标签同列、实时关注人数、列表项“关注/已关注”按钮、头像点击提示、下拉刷新，以及“…”底部弹窗（特别关注开关、设置备注、取消关注、关闭）。

## 功能概览
- 标签页导航：`互关 / 关注 / 粉丝 / 朋友`，支持左右滑动（ViewPager2 + TabLayout）
- 顶部布局：左上角返回按钮与四个标签同一行（无标题）
- 关注人数：列表上方弱化文案“我的关注（N人）”实时变化
- 列表项交互：
  - 头像、名称/备注、认证图标、右侧“关注/已关注”按钮与“…”菜单
  - 点击头像弹出 `已选中（昵称）` 提示（昵称优先取备注）
  - “关注/已关注”按钮即时切换：未关注红底白字“关注”，已关注灰底黑字“已关注”
  - 特别关注标签：名称右侧弱化「特别关注」细框，切换后立即显示/隐藏
- 下拉刷新：`SwipeRefreshLayout` 重新加载本地数据，保持最新状态
- 底部弹窗（点击“…”）：
  - 顶部独立灰底 Header：
    - 未备注：标题显示原始名字
    - 已备注：标题显示备注文本，下一行显示“名字：原始名字”
  - 特别关注开关：切换后持久化并局部刷新列表项
  - 设置备注：弹出输入框，保存后列表名称与弹窗标题同步更新
  - 取消关注：立即变为未关注并更新人数
  - 右上角 `×` 关闭弹窗

## 关键实现
- 列表项按钮两态与即时切换：`app/src/main/java/com/example/java/UsersAdapter.java:50`
- 头像点击提示（备注优先）：`app/src/main/java/com/example/java/UsersAdapter.java:57`
- 特别关注标签渲染：`app/src/main/java/com/example/java/UsersAdapter.java:48`
- 底部弹窗与逻辑：`app/src/main/java/com/example/java/UsersAdapter.java:61`、`app/src/main/res/layout/dialog_user_actions.xml`
- 备注保存后标题与副标题联动：`app/src/main/java/com/example/java/UsersAdapter.java:80`
- 列表局部刷新避免卡顿：`app/src/main/java/com/example/java/FollowListFragment.java:27`
- 异步持久化，避免主线程阻塞：`app/src/main/java/com/example/java/UserRepository.java:57`
- 关注人数实时统计：`app/src/main/java/com/example/java/FollowListFragment.java:68`
- 下拉刷新：`app/src/main/java/com/example/java/FollowListFragment.java:36`

## 项目结构
```
app/
  src/main/java/com/example/java/
    MainActivity.java            // 标签页与返回按钮
    FollowListFragment.java      // 关注页列表、刷新、统计与交互回调
    UsersAdapter.java            // 列表项渲染与点击逻辑、底部弹窗
    UserRepository.java          // 本地 JSON 持久化、异步保存、种子数据
    User.java                    // 数据模型（含 followed/special/remark）
  src/main/res/layout/
    activity_main.xml            // 顶部返回 + 标签
    fragment_follow_list.xml     // 关注人数 + 列表 + 下拉刷新
    item_user.xml                // 列表项视图（特别关注标签、按钮等）
    dialog_user_actions.xml      // 底部弹窗布局
  src/main/res/drawable/
    bg_follow_action.xml         // 红底“关注”按钮
    bg_follow_status.xml         // 灰底“已关注”按钮
    bg_chip_outline.xml          // 「特别关注」细框标签
```

## 环境与依赖
- Gradle 8.13（国内网络建议使用镜像）
- AndroidX：`appcompat 1.6.1`、`material 1.10.0`、`viewpager2 1.1.0`、`swiperefreshlayout 1.1.0`

## 构建与安装
- Windows 构建：
  - `./gradlew.bat assembleDebug`
- 安装到模拟器/设备：
  - `./gradlew.bat installDebug`
  - 或 `adb install -r app/build/intermediates/apk/debug/app-debug.apk`

## 常见问题与解决
- Gradle 下载超时：将 `gradle-wrapper.properties` 的 `distributionUrl` 指向国内镜像，并提高 `networkTimeout`
- 启动崩溃（整数溢出）：生成关注时间时改用 long（`UserRepository.java:76`）
- 点击卡顿：保存改为异步，列表只做局部刷新，避免整表 diff

