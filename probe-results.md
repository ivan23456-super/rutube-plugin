# probe13 Sat Nov 21 08:58:52 UTC 2026

## playlist/user
https://rutube.ru/api/playlist/user/5223991/?format=json&page=1&limit=20
http=200 bytes=669
{"count":null,"next":null,"n":1,"firstKeys":["author","author_sort_order","created_ts","id","is_hidden","is_official","is_reborn_channel","kind_sign_for_user","last_modified_ts","pepper","thumbnail_url","title","videos_count"]}

## playlist/user no format
https://rutube.ru/api/playlist/user/5223991/?page=1&limit=20
http=200 bytes=669
{"count":null,"next":null,"n":1,"firstKeys":["author","author_sort_order","created_ts","id","is_hidden","is_official","is_reborn_channel","kind_sign_for_user","last_modified_ts","pepper","thumbnail_url","title","videos_count"]}

## pinned_playlists
https://rutube.ru/api/v1/channels/5223991/pinned_playlists?page=1&limit=10&order_by=created_ts_desc&limit_video=20
http=204 bytes=0

## favorite playlists
https://rutube.ru/api/metainfo/playlist/favorite?author_id=5223991&format=json
http=401 bytes=59

## playlist/user first item (full)
{"id":1920264,"title":"Кот Костян Новое","videos_count":2,"created_ts":"2026-11-18T23:21:49+03:00","last_modified_ts":"2026-11-19T17:34:09+03:00","thumbnail_url":"https://pic.rtbcdn.ru/playlist/2026/11/18/0a/5c/a8cbe089-e38d-11f1-9d12-26d12b8e975f.jpg","author_sort_order":"","author":{"id":5223991,"name":"Кот Костян","avatar_url":"https://pic.rtbcdn.ru/user/e1/22/e122969ed236afacbeeddaaf5f95e177.jpeg","site_url":"https://rutube.ru/video/person/5223991/"},"is_official":false,"kind_sign_for_user":true,"is_reborn_channel":false,"is_hidden":false,"pepper":null}

## pinned first item

## playlist id -> 1920264

## playlist info
https://rutube.ru/api/playlist/custom/1920264/?format=json
http=200 bytes=558

## playlist videos
https://rutube.ru/api/playlist/custom/1920264/videos/?page=1&limit=20&format=json
http=200 bytes=3379
{"count":null,"next":null,"n":2,"firstKeys":["action_reason","author","category","created_ts","description","duration","embed_url","feed_name","feed_url","hits","id","is_adult","is_audio","is_deleted","is_hidden","is_livestream","is_locked","is_official","is_original_content","is_paid","kind_sign_for_user","origin_type","pg_rating","product_id","publication_ts","stream_type","thumbnail_url","title","video_url"]}

## playlist videos first item keys
{"count":null,"n":2,"keys":["action_reason","author","category","created_ts","description","duration","embed_url","feed_name","feed_url","hits","id","is_adult","is_audio","is_deleted","is_hidden","is_livestream","is_locked","is_official","is_original_content","is_paid","kind_sign_for_user","origin_type","pg_rating","product_id","publication_ts","stream_type","thumbnail_url","title","video_url"]}
