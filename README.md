# Henry: バックエンドエンジニア課題 (大野祐貴)

### 使用した技術:
* Language: Java 17
* API: gRPC
* RDB: MySQL 8
* ORM: MyBatis
* Framework: Spring Boot 2.7

今回時間がなかったため、普段毎日使って慣れているものを選択しました。  
本当はMySQL5.7系を普段使っているのですが、今回自前のPCにインストールが成功しないため仕方なく8系です。  
MyBatisはORMではないと言われるかもしれませんが、一応広い意味でのORMの一つかと思います。

--- 

### 成果物:
* API定義書: ```doc/index.html```
* DB: ```src/sql/Hospital.sql```
  * Windows: ```Get-Content .\src\sql\Hospital.sql | mysql -u root -pPASS```
  * Other: ```mysql -u root -pPASS < ./src/sql/Hospital.sql```
* アプリケーション起動: ```./gradlew bootrun```  
  ```
  grpcurl -import-path ./src/main/proto/ -proto Hospital.proto -plaintext -d '{"name": "test"}' localhost:6565 hospital.Hospital/CreatePatient
  ```
* テストコード: ```src/test/java/com/example/hospital/HospitalTests.java```
* テストコード起動: ```./gradlew test```

※ 現在Windows環境でのみ確認 (pathのみ変更)

---

### 課題に対する疑問:
* 入院日を入院期間に含めるのか含めないのか (現在含めている)
* 移動日を入院期間に含めるのか含めないのか (現在含めていない)
* 退院日を入院期間に含めるのか含めないのか (現在含めている)

---

### 改善できそうな点:
* 使用している用語が間違っている可能性が高い (同じ意味を表す単語が複数存在)
* gRPC型とJava型を手動変換している、自動変換できるようにしたい
* LocalDateTimeでDBに記録しているが、UTCで記録するべきか
* validationの仕組みがなく手動でチェックしている
* 汎用エラーをthrowしていて専用エラーを用意していない
* Mapperのrequestを汎用のMap型としている、専用型を渡すようにすべきか
* 移動テーブルにfromとtoで両方持たせる必要はなかった？ (toのみで処理できる)
* SQLのwindow関数を使うともっと効率よくできる？
* 最初の病室IDを入院テーブルに持たせるのではなく、移動テーブルに持たせるべきか (別カラムに持たせる or start_date:nullにセット)
* 退院日も別テーブルに独立して管理するべきか
* 外出は期間ではなく外出日として一日単位で管理すべきか
* DBの更新を楽観ロックのみで問題ないのか
* 実行ユーザーまた権限を管理していない
* 時間が足りないため、コピーで済ませてしまったところが多く、もっと共通化できるはず (リファクタリングできていない)
* テストケースが足りていないため、まだまだ問題が隠れている可能性あり
* debugログが埋め込まれていない (動作を追えるようにできていない)
* 既に存在するレコードの確認ができていない (Duplicate entry)
