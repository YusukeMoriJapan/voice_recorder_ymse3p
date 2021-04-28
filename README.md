# オーディオレコーダー

# 目次
- [アプリダウンロード先](#アプリダウンロード先)
- [アプリ機能](#アプリ機能)
- [技術スタック](#技術スタック)
- [全体アーキテクチャ](#全体アーキテクチャ)
- [アプリのアピールポイント](#アプリのアピールポイント)

# アプリダウンロード先
GooglePlay Storeからインストール下さい  
https://play.google.com/store/apps/details?id=ymse3p.app.audiorecorder


# アプリ機能
* マイクを使用した録音


* 「録音時の移動経路」を記録


* 表示
  * 録音データの一覧表示
  * 再生状態の表示
  * Notificationを用いた、リアルタイムでの再生状態通知
  * 移動経路の表示
  * ダークモードへの対応


* データの操作
  * 録音デーの保存(「録音タイトル」「録音時間」「保存日時」をSQLデータベースに保管)
  * 録音データ削除(「一括削除」「指定されたデータの削除」に対応)


* 再生操作
  * 任意の音源データ再生(バックグラウンド再生や連続再生にも対応)
  * 通知バーでの再生音源データ切り替え
  * Bluetooth機器(イヤホンやリモコン等)からの再生操作受付
  * ボイスコマンドへの対応("OK google、次の曲を再生して"等)


* 他音声アプリとの協調動作
    * 他アプリから音声が流れる場合は、再生をストップ


* スマートウォッチ(Wear OS)への完全対応
    * Wear OS上から再生操作や選曲が可能

## アプリ画面詳細
* 再生データの切替に応じて、録音データ一覧(RecyclerView)の各行にもリアルタイムで再生状態が反映されます。

| メイン画面 | 通知バー | 録音開始~終了 |
|---|---|---|
|<img src="https://user-images.githubusercontent.com/53045385/112773427-04bafe80-9071-11eb-98b9-7e51ab9cab43.gif" width="240" />|<img src="https://user-images.githubusercontent.com/53045385/112773430-071d5880-9071-11eb-991a-99c437a03aa5.gif" width="240" /> |<img src="https://user-images.githubusercontent.com/53045385/112773432-084e8580-9071-11eb-989c-7333bacde238.gif" width="240" /> |

| データ削除(ダークモード) | Wear OSへの完全対応 | 移動経路の表示 |
|---|---|---|
|<img src="https://user-images.githubusercontent.com/53045385/112776013-78610980-9079-11eb-9fd9-9b40743f2287.gif" width="240" /> |<img src="https://user-images.githubusercontent.com/53045385/113507675-f7c57000-9586-11eb-8b7b-1dce65fec192.gif" width="240" /> | <img src="https://user-images.githubusercontent.com/53045385/116402045-e52d1680-a866-11eb-8e08-e8dcd1c97703.gif" width="240" />|


# 技術スタック
## ライブラリ群
* jetpack Library
  * Room Database (SQLite)
  * Data Biding & Binding Adapter
  * DataStore
  * Shimmer Recycler View
  * Navigation


* 音源再生
  * Media Session / Media Browser Serivce / Media Controller / Media Browser
  * Exo Player


* 録音
  * Media Recorder


* マルチスレッド・非同期処理
  * Kotlin Coroutines & Flows(Flow、SharedFlow、StateFlow)


* Dependency Injection
  * Dagger Hilt


* ネットワーク通信
 * Retrofit2
 * Okhttp3
 * Gson

## 連携しているWeb API
 * [Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/overview?hl=ja)  
　アプリ内での地図表示に使用
 * [Roads API Snap to Roads](https://developers.google.com/maps/documentation/roads/snap)  
　 移動経路情報の補正 (生の位置情報を実在の道路上にスナップ) に使用

| 経路情報補正イメージ |
|---|
|<img width="240" src="https://developers.google.com/maps/documentation/roads/images/snapped.png" />|


## MAD Score
<img width="1000" src="https://user-images.githubusercontent.com/53045385/112827234-057f7f00-90c9-11eb-8657-947598406f9e.png" />

# 全体アーキテクチャ
<img width="1000" src="https://user-images.githubusercontent.com/53045385/113559657-bdb6a580-963c-11eb-8489-bb832e39779e.jpeg" />

# アプリのアピールポイント
 ## 「クライアント/サーバー型」アーキテクチャを採用
 <img width="500" src="https://developer.android.com/guide/topics/media-apps/images/controller-and-session.png?hl=ja" />

Android上でメディアプレーヤーを実装する場合は、再生を担うコンポーネントを、「再生操作を要求する部分」と「要求を受け取り実際に再生操作を行う部分」に分離して、クライアント/サーバー型設計を採用することが推奨されています。

加えて、上記の設計をサポートするクラス(MediaSessionやMediaController等)がGoogleから提供されおり、当アプリでは、これら標準クラスを積極的に活用しています。

これにより、Android端末外部(スマートウォッチやBluetoothイヤホン等)からの操作要求についても、特別な処理を書かずに、対応させることができています。

 ## SOLID原則の「O」と「D」を意識した、Serviceクラスの設計
 DIやインターフェースを積極的に活用することで、クラス同士の相互依存・循環依存を解消させ、Model層からServiceクラスに向かった単一方向の依存関係を構築しました。これにより、テスタブル(テスト用オブジェクトに容易に差し替えられる)で、仕様変更に強い(仕様変更による影響範囲が安易に特定できる)アプリ設計を実現しています。

 ### 設計初期段階
<img width="500" src="https://user-images.githubusercontent.com/53045385/113543195-d4e79a00-9620-11eb-89b2-52200f4f21a9.jpeg" />

設計の初期段階では、Serviceクラスが依存しているオブジェクトを、"Serviceクラス内部"で生成していたため、クラス同士が密結合状態となっていました。このため、例えば、「Serviceクラスの挙動を確認するために、Serviceクラスの依存先をテスト用のクラスに差し替える」といったテクニックが使えない状態にありました。

 ### DIによる密結合の解消を試みましたが・・・
<img width="500" src="https://user-images.githubusercontent.com/53045385/113543202-d749f400-9620-11eb-98a5-af4a493adbb4.jpeg" />

そこで、DIパターンを用いて密結合状態の解消を試みました。しかしながら、Serviceクラス内部の実態は、「循環参照・相互参照が存在する依存関係のスパゲッティ状態」となっており、この状態では「依存オブジェクトを外部から注入する」ことができず、Google側が提供しているクラスをそのまま利用するような形では、密結合状態を解消できません。

 ### 抽象化による依存性の逆転
<img width="1000" src="https://user-images.githubusercontent.com/53045385/113544438-3577d680-9623-11eb-830f-3d3ff76e44f1.jpeg" />

こういった背景から、このアプリでは、既存の再生コンポーネント群(ExoPlayerやMediaSession等)を一つの集合として捉え、「PlaybackComponentインターフェース」として再定義しました。

加えて、再生コンポーネント群の実行状態(再生中・停止中・何番目の音源を再生中等)を、ComponentStateクラスとして別クラスに抜き出し、Observableとして再定義することで、単一方向の依存関係を構築しました。

これにより、複数存在したServiceクラスの依存先は一つになり、かつ抽象(インターフェース)への依存も実現できる形となりました。

上記の依存関係を構築することにより、 "具象クラス(実際に再生を担うクラス群)の変更が、Serviceクラスに直接影響を与えない" すなわち、テスタブルで(テスト用オブジェクトに容易に差し替えられる)、仕様変更に強い(仕様変更による影響範囲が安易に特定できる)クラス設計を実現させています。

 ## 再生状態の変化に応じて、動的に表示が変化するRecyclerViewの実装
 |<img src="https://user-images.githubusercontent.com/53045385/112773427-04bafe80-9071-11eb-98b9-7e51ab9cab43.gif" width="200" />

このアプリでは、動的に表示状態が変化するRecyclerViewを実装しています。  
具体的な制御の流れは、下記の通りです。
1. 再生状態の変化をAdapterが検知(再生音源データが切り替わった・再生が停止した等)
2. RecyclerViewが保持しているViewHolder全てに対して、最新の状態を反映

このロジックを適用することにより、例えば、「RecyclerView内に表示されているViewの中で、現在再生中の音源を示すViewに対してのみ、停止ボタンを表示させる」といったことが可能になっています。

しかしながら、このロジックのみでは、画面の上下スクロールに伴う再バインディグによってViewHolderの表示状態が初期化されてしまうため、onBindViewHolder()のタイミングでも、"その時点での再生状態" を反映させる処理を追加しています。

なお、上記処理は、生成されたViewHolderに対してのみ更新処理を行うため、例えば、表示件数が数万件を超える場合でも、ユーザーに見えるViewに対してのみ更新処理をかけることとなります。

これはすなわち、RecyclerViewの特徴である「ViewHolderの使い回し」によるメリット(必要最低限のリソースで大量の表示を実現させる)を失わずに、動的処理を実現できているとうことでもあります。
