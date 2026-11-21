# probe5 Sat Nov 21 08:17:17 UTC 2026

## distinct content types, query=сборник pages 1-3

- page 1: [{"ad":1},{"video":20}]

- page 2: [{"video":20}]

- page 3: [{"video":20}]

## playlist endpoint candidates (query=кот)

- https://rutube.ru/pangolin/api/web/v1/search/combined/playlist?query=%D0%BA%D0%BE%D1%82&page=1
http=404 bytes=275 
- https://rutube.ru/pangolin/api/web/v1/search/combined/playlists?query=%D0%BA%D0%BE%D1%82&page=1
http=404 bytes=277 
- https://rutube.ru/pangolin/api/web/v1/search/playlists?query=%D0%BA%D0%BE%D1%82&page=1
http=404 bytes=259 
- https://rutube.ru/pangolin/api/web/v1/search/combined/video_playlist?query=%D0%BA%D0%BE%D1%82&page=1&content_type=playlist
http=200 bytes=37977 {"n":21,"types":[{"ad":1},{"video":20}],"first":["block_code","content_type","position_rule"]}

- https://rutube.ru/api/search/combined/cards/list?query=%D0%BA%D0%BE%D1%82&page=1&content_type=playlists
http=200 bytes=3446 {"n":3,"types":[{"userchannel":3}],"first":["absolute_url","can_subscribe","content","description","icon","id","is_official","is_original_content","is_original_sticker_2x2","is_reborn_channel","name","picture","subscribers_count","subscription_url","type","user_channel_image","video_count","view_url"]}

- https://rutube.ru/api/search/combined/cards/list?query=%D0%BA%D0%BE%D1%82&page=1&content_type=playlist_author
http=200 bytes=3446 {"n":3,"types":[{"userchannel":3}],"first":["absolute_url","can_subscribe","content","description","icon","id","is_official","is_original_content","is_original_sticker_2x2","is_reborn_channel","name","picture","subscribers_count","subscription_url","type","user_channel_image","video_count","view_url"]}

- https://rutube.ru/api/search/combined/cards/list?query=%D0%BA%D0%BE%D1%82&page=1&type=playlist
http=200 bytes=3446 {"n":3,"types":[{"userchannel":3}],"first":["absolute_url","can_subscribe","content","description","icon","id","is_official","is_original_content","is_original_sticker_2x2","is_reborn_channel","name","picture","subscribers_count","subscription_url","type","user_channel_image","video_count","view_url"]}

## does cards/list playlist support filter+page
{"count":3,"n":3,"names":["ЕвгенБро сборник серий","Ютуб сборник","Синий трактор - Песенки и мультики для детей"]}

## shorts manual page 2
{"count":100,"n":20,"next":"http://search-backend.rutube.ru/api/search/shorts?query=%D0%BA%D0%BE%D1%82&page=3&per_page=20"}

## video_playlist manual page 2 with filters
{"count":20,"n":20,"next":"https://rutube.ru/pangolin/api/web/v1/search/combined/video_playlist?client=wdp&page=3&per_page=20&query=%D0%BA%D0%BE%D1%82&created=week&duration=short"}

## kiosk recommendation item keys + ad tick
{"n":10,"next":"https://rutube.ru/api/v2/video/recommendation/main?limit=20&offset=20","keys":["action_reason","age_range","author","category","created_ts","description","duration","embed_url","feed_name","feed_url","hits","html","id","is_adult","is_audio","is_classic","is_club","is_deleted","is_hidden","is_licensed","is_livestream","is_locked","is_official","is_on_air","is_original_content","is_original_sticker_2x2","is_paid","is_reborn_channel","kind_sign_for_user","last_update_ts","origin_type","pg_rating","picture_url","position","preview_url","product_id","publication_ts","stream_type","thumbnail_url","title","track_id","video_url"],"first":{"title":"ТРИ КОТА | ТРИ КОТА ","is_livestream":false,"id":"679ca28c05f4b04728d3b54af50a11d3"}}
