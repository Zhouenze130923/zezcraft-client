# ZezCraft Client

ZezCraft 服务器的开源客户端模组（Fabric，Minecraft 26.1.2）。装上它，服务端插件就能：

- 检测任意按键和鼠标的按下、松开、按住时长（包括长按左键）
- 下发 JSON 描述的整屏界面和 HUD：面板、按钮、图片、物品图标、玩家头像、进度条、输入框，带动画
- 用关键帧时间线驱动物品的 custom_model_data，动画在客户端本地插值
- 让玩家第三人称显示举枪姿势

没装模组的玩家不受影响，插件会自动退回原版界面。

## 隐私

除了服务器订阅的按键和四个动作键（换弹、检视、模式、技能），模组不上报任何输入；打开聊天栏或其他界面时也不上报。模组不读取、不上传任何文件或账号信息。

## 安装

1. 安装 Minecraft 26.1.2 的 Fabric Loader（0.17+）和 Fabric API。
2. 把 `zezclient-x.y.z.jar` 放进 `.minecraft/mods/`。

## 协议

插件消息通道 `zez:c`，完整文档：https://zezcraft.com/developers/docs/zezclient

## 构建

不依赖 Gradle，直接用 JDK 21+ 编译：

1. 把以下 jar 放进 `lib/`：Minecraft 26.1.2 客户端（clientOnly 与 common，未混淆）、Fabric Loader、Fabric API 各模块、Sponge Mixin、MixinExtras、Gson、JOML、Brigadier、DataFixerUpper、authlib、fastutil、guava、netty、lwjgl、slf4j。最简单的做法是从一个 Fabric 26.1.2 开发环境的 Gradle 缓存里复制。
2. 运行 `./build.sh 0.3.0`，产物是 `zezclient-0.3.0.jar`。

## 目录

| 路径 | 内容 |
|---|---|
| `src/com/zezcraft/client/Net.java` | 协议收发 |
| `src/com/zezcraft/client/Input.java` | 按键与鼠标检测 |
| `src/com/zezcraft/client/St.java` | 物品绑定与动画时间线 |
| `src/com/zezcraft/client/ui/` | 界面：`E` 元素、`R` 布局与绘制、`Ui` 屏幕与 HUD |
| `src/com/zezcraft/client/mixin/` | 攻击 / 使用拦截、custom_model_data 覆盖、持枪姿势 |
| `res/` | `fabric.mod.json`、mixin 配置、语言文件 |

## 许可证

MIT，见 [LICENSE](LICENSE)。
