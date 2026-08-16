# クラス構造

共通処理は `common` に集約し、Minecraftの描画・Mixin・テクスチャ登録など、Minecraft APIに依存する処理は `targets/mc-*` に分離する。

```mermaid
classDiagram
    class MinemojiConfig {
        +load(path) MinemojiConfig
        +reload()
    }
    class SlackEmojiService {
        +refreshAsync()
        +suggestions(query)
        +find(name)
        +clear()
    }
    class StandardEmojiDataset {
        +entries() Map
        +unicodeForSlackName(name) String
    }
    class ChatEmojiParser {
        +find(text) List
    }
    class MinemojiClient {
        +config() MinemojiConfig
        +slackEmojiService() SlackEmojiService
        +slackEmojiTextureCache() SlackEmojiTextureCache
    }
    class SlackEmojiTextureCache {
        +get(emoji)
        +clear()
    }
    class ChatEmojiOverlay {
        +renderTooltipScreen(...)
    }
    class ChatScreenMixin {
        <<target specific>>
    }
    class ChatComponentMixin {
        <<target specific>>
    }

    MinemojiConfig --> SlackEmojiService : 設定を共有
    SlackEmojiService --> StandardEmojiDataset : 標準絵文字を補完
    MinemojiClient --> MinemojiConfig
    MinemojiClient --> SlackEmojiService
    MinemojiClient --> SlackEmojiTextureCache
    SlackEmojiTextureCache --> SlackEmojiService
    ChatScreenMixin --> ChatEmojiParser : 入力解析
    ChatScreenMixin --> SlackEmojiService : 候補取得
    ChatComponentMixin --> ChatEmojiOverlay : プレビュー描画
    ChatEmojiOverlay --> ChatEmojiParser : 絵文字位置解析
    ChatEmojiOverlay --> SlackEmojiTextureCache : テクスチャ取得
```

`ChatComponentMixin`、`ChatScreenMixin`、`ChatEmojiOverlay`、`SlackEmojiTextureCache` はMinecraftバージョンごとに実装を持つ。特に描画APIと現在画面の取得方法はターゲット側で吸収し、Slack連携や絵文字名の解決ロジックは共通実装を使う。
