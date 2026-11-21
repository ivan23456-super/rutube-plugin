# probe3 Sat Nov 21 08:14:10 UTC 2026

## ordering: created_ts of first 3 + hits

- default
[{"c":"2025-09-14T20:40:51","h":8993800},{"c":"2026-02-16T17:26:33","h":630473},{"c":"2026-03-03T23:15:30","h":4548315}]

- order_by=created_ts_down
[{"c":"2025-09-14T20:40:51","h":8993729},{"c":"2026-02-16T17:26:33","h":630473},{"c":"2026-03-03T23:15:30","h":4548304}]

- order_by=created_ts_up
[{"c":"2025-09-14T20:40:51","h":8993800},{"c":"2026-02-16T17:26:33","h":630473},{"c":"2026-03-03T23:15:30","h":4548315}]

- order_by=hits
[{"c":"2025-09-14T20:40:51","h":8993529},{"c":"2026-02-16T17:26:33","h":630473},{"c":"2026-03-03T23:15:30","h":4548315}]

- sort=created
[{"c":"2025-09-14T20:40:51","h":8993729},{"c":"2026-02-16T17:26:33","h":630473},{"c":"2026-03-03T23:15:30","h":4548304}]

- sort=hits
[{"c":"2025-09-14T20:40:51","h":8993729},{"c":"2026-02-16T17:26:33","h":630473},{"c":"2026-03-03T23:15:30","h":4548304}]

- order_by=date

## pangolin video item keys
["action_reason","author","category","common_subscription_product_codes","content_type","created_ts","description","duration","embed_url","feed_name","feed_url","hits","id","is_adult","is_audio","is_hidden","is_livestream","is_official","is_on_air","is_original_content","is_paid","is_reborn_channel","kind_sign_for_user","last_update_ts","origin_type","pg_rating","product_id","publication_ts","stream_type","thumbnail_url","title","video_url"]

## cards playlist
{"count":2,"next":null,"item":["absolute_url","can_subscribe","content","description","icon","id","is_official","is_original_content","is_original_sticker_2x2","is_reborn_channel","name","picture","subscribers_count","subscription_url","type","user_channel_image","video_count","view_url"],"items":[{"t":"?","n":"Кот Костян","vc":236,"id":5223991},{"t":"?","n":"Котёнок Котэ","vc":340,"id":31168197}]}

## cards channel
{"count":100,"next":"https://rutube.ru/api/search/combined/cards/list?query=%D0%BA%D0%BE%D1%82&page=2&content_type=channel&per_page=20","item":["absolute_url","can_subscribe","content","description","icon","id","is_official","is_original_content","is_original_sticker_2x2","is_reborn_channel","name","picture","subscribers_count","subscription_url","type","user_channel_image","video_count","view_url"],"items":[{"t":"?","n":"Кот Костян","id":5223991},{"t":"?","n":"Весёлый Кот","id":44664760},{"t":"?","n":"Рыжий Кот Games","id":25674336}]}

## cards playlist page 2 + created filter
{"count":3,"n":0}
{"count":3,"n":3}

## shorts item keys + next
{"count":100,"next":"http://search-backend.rutube.ru/api/search/shorts?query=%D0%BA%D0%BE%D1%82&page=2&per_page=20","item":["action_reason","author","category","created_ts","description","duration","embed_url","feed_name","feed_url","hits","id","is_adult","is_audio","is_deleted","is_hidden","is_livestream","is_locked","is_official","is_on_air","is_original_content","is_paid","is_reborn_channel","kind_sign_for_user","last_update_ts","origin_type","pg_rating","product_id","publication_ts","stream_type","thumbnail_url","title","video_url"],"first":[{"t":"?","ti":"#кот на компе#кот мо"}]}

## pangolin next url
{"count":20,"next":"https://rutube.ru/pangolin/api/web/v1/search/combined/video_playlist?client=wdp&page=2&per_page=20&query=%D0%BA%D0%BE%D1%82","per_page":20}

## live candidates from recommendation
[]

## search page html hints
bytes=281053
