# Minemoji

Minecraftのチャット欄で `:emoji_code:` を入力すると、Slack Workspaceのカスタム絵文字と標準絵文字の候補を表示するFabricクライアントmodです。

## 対応ターゲット

MinecraftバージョンごとのMinecraft API・Mixin・描画処理は `targets/` に分離し、Slack連携・標準絵文字マップ・入力解析・設定は `common/` で共有しています。

| Minecraft | ターゲット | Java |
| --- | --- | --- |
| 1.21.11 | `targets/mc-1.21.11` | 21 |
| 26.1.2 | `targets/mc-26.1.2` | 25 |
| 26.2 | `targets/mc-26.2` | 25 |

## Slackの準備

1. Slack Appを作成します。
2. `emoji:read` scopeを追加します。
3. AppをWorkspaceにインストールします。
4. 発行されたbot tokenまたはuser tokenを設定します。

scopeを追加・変更した場合は、Workspaceへの再インストールが必要です。

## Minecraftの設定

ゲームを一度起動すると `config/minemoji.json` が作成されます。Mod MenuのConfigボタン、または `/minemoji config` から設定画面を開けます。

```json
{
  "slackToken": "xoxb-xxxxxxxx",
  "maxSuggestions": 8,
  "minimumQueryLength": 1,
  "refreshOnStartup": true
}
```

設定後に `/minemoji refresh` を実行すると、Slackの絵文字一覧を更新します。

## 開発

Java 21またはJava 25が必要です。Windowsでは `gradlew.bat` を使用してください。

```bash
./gradlew build
./gradlew :common:test
./gradlew :targets:mc-1.21.11:build
./gradlew :targets:mc-26.1.2:build
./gradlew :targets:mc-26.2:build
```

デバッグ用の広いヒットボックス・描画ログ・修正の切り替えは、明示的にデバッグプロファイルを指定したビルドだけに含まれます。

```bash
./gradlew build -PminemojiDebug=true
```

## License

MIT License
