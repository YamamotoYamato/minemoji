# Minemoji

Fabric 26.1 向けのクライアント mod です。Minecraft のチャット欄で `:emoji_code` を入力すると、Slack Workspace のカスタム絵文字コード候補を表示します。

## 現状の仕様

- Slack の `emoji.list` を使って Workspace のカスタム絵文字と alias を取得します
- Slack の標準絵文字と内蔵の標準絵文字マップを候補に含めます
- 候補表示は通常チャット入力時のみで、`/` から始まるコマンド入力では無効です
- `Up` / `Down` で候補移動、`Tab` または `Enter` で確定、`Esc` で候補を閉じます

## Slack 側の準備

1. Slack App を作成します
2. `emoji:read` scope を追加します
3. App を Workspace にインストールします
4. 発行された bot token か user token を控えます

注意:

- scope を追加・変更した後は `Reinstall to Workspace` が必要です
- `missing_scope` が出る場合は、たいてい `emoji:read` 未付与か再インストール漏れです

公式ドキュメント:

- `emoji:read`: https://docs.slack.dev/reference/scopes/emoji.read/
- `emoji.list`: https://api.slack.com/methods/emoji.list

## Minecraft 側の設定

1. 一度ゲームを起動して `config/minemoji.json` を生成します
2. Mod Menu を入れていれば Minemoji の `Config` ボタンから、入れていなければ `/minemoji config` で設定画面を開くか、`config/minemoji.json` を直接編集します
3. `slackToken` に Slack token を設定します
4. 必要なら `maxSuggestions` と `minimumQueryLength` を調整します
5. `/minemoji refresh` を実行します

`/minemoji refresh` は設定ファイルを再読込してから Slack 絵文字一覧を更新します。`slackToken` を書き換えた後も、通常は Minecraft の再起動は不要です。

設定ファイル例:

```json
{
  "slackToken": "xoxb-xxxxxxxx",
  "maxSuggestions": 8,
  "minimumQueryLength": 1,
  "refreshOnStartup": true
}
```

## 開発

Java 25 が必要です。

```bash
./gradlew build
```

デバッグ機能を含むビルドは、明示的に `-PminemojiDebug=true` を指定します。

```bash
./gradlew build -PminemojiDebug=true
```

## License

MIT License
