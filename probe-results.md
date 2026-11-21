# probe12 Sat Nov 21 08:58:19 UTC 2026
http=200 bytes=207063
## around playlist/custom
,i=n(4481);n.d(t,{},{e:{fetchPlaylistMeta:e=>(0,r.uj)((0,i.Y)(`api/page_options/page_meta/catalog/playlist/${e}/`)),fetchPlaylistInfo:e=>(0,r.uj)((0,i.Y)(`/api/playlist/custom/${e}/`)),fetchPlaylistVideos:e=>(0,r.uj)((0,i.Y)(`/api/playlist/custom/${e.playlistId}/videos/`,{params:{page:e.page,limit:e.limit,video_id:e.videoId,sort:e.sortValue,sort_random_seed:e.sortRandomSee
## around author
CAROUSEL:"PLAYER_CAROUSEL",BANNER_CAROUSEL:"BANNER_CAROUSEL"},a:{AUTHORIZATION:"authorization"}})},29155(e,t,n){n.d(t,{},{l:{BANNED_USER_CANT_COMMENT:"Вы\xa0пока не\xa0можете комментировать: автор ограничил
t}=e;return(0,r.uj)((0,i.Y)("/api/comments/channel/block/",{method:"POST",data:{user_id:t}}))},reactToComment:(e,t)=>(0,r.uj)((0,i.Y)(`/api/comments/${t}/reactions/`,{method:"PUT",data:{reaction:e}})),delete
(e=>{let{broadcast_category:t,pg_rating:n,thumbnail_url:r,video_url:o,is_paid:i,author:{avatar_url:a,is_official:s,site_url:l,is_reborn_channel:c,kind_sign_for_user:u,...d},...p}=e;return{...p,pgRating:n,th
{cb:r.nL.fetchChannelPinnedVideo,transformResponse:e=>e?{...e,video:{...e.video,author:{...e.video.author,name:(0,a.k)(e.video.author.name)}}}:null})}),videos:e.query({queryFn:(0,o.t8)({cb:r.nL.fetchUserCha
nnelSearchResult:e=>(0,r.E7)((0,s.Y)("/api/search/video",{params:{query:e.query,author_ids:e.userChannelId,page:e.page}})),fetchUserChannelFavoritePlaylists:e=>(0,r.uj)((0,s.Y)("/api/metainfo/playlist/favorite
},checkStreamSlots:()=>(0,o.uj)((0,i.Y)(`${window.__env__.API}/api/video/stream/owner`)),fetchVideoPage:a,getBanUserStatus:e=>(0,o.uj)((0,i.Y)(` /api/comments/blocked/video/${e}`)),getReactionsCount:s,getU
!a||"child"===e.status;e.canCreateChildProfile=!!s,e.hasChildProfile=!!l;let c={user_id:r?.id,rupass_id:r?.rupassId,avatar:r?.avatarUrl},u={user_id:a?.user_id??i?.squirrel_id,rupass_id:i?.rupass_id,avatar:a?
id","screen_name","session_id","sso_id","sso_source","touch_point","user_auth","user_id","uuid","user_subscription","user_subscription_id","safe_mode","pageload_id","api_method","batch_id","segment_id"],I=e=
).getAll("utm_medium"),k=_?{category:c?.category.short_name,channelId:String(c?.author.id??""),videoId:c?.id}:{},I={userId:t,slug:n,cid:(0,g.fL)(),platform:b.f.isMobile()?"browser_mobile":"browser",subscrib
ow().toString(),event_timezone:(i()().utcOffset()/60).toString(),user_auth:I(t),user_id:t?.toString(),uuid:(0,r.yk)(),user_subscription:n(()=>I("ACTIVE"===(0,m.oy)(e))),user_subscription_id:n(()=>(0,m.ch)(e)
## channelTab playlist functions
voritePlaylists}),providesTags:["USER_CHANNEL_FAVORITE_PLAYLISTS"]}),pinnedPlaylists:e.query({queryFn:(0,o.t8)({cb:r.nL.fetchChannelPinnedPlaylists,transformResponse:e=>({...e,results:e.results?.filter(e=>e.videos.length>0)||[]})}),...(0,o.pq)(e=>e?.query?.page,{cacheKeys:["channelId"]})}),channelRecommendations:e.query({queryFn:(0,o.t8)({cb:r.nL.fetchChannelRecomm
erChannelFavoritePlaylists:e=>(0,r.uj)((0,s.Y)("/api/metainfo/playlist/favorite",{params:{author_id:e.userChannelId}})),fetchChannelPinnedPlaylists:e=>{let t={page:1,limit:10,order_by:"created_ts_desc",limit_video:20,...e.query};return(0,r.uj)((0,s.Y)(`api/v1/channels/${e.channelId}/pinned_playlists`,{params:t}))},fetchChannelRecommendations:(e,t)=>{let n={source:"
context:w,content_type:(0,_.gt)(t.resourceItem),channel_id:(0,h.r9)(n),content_id:t.resourceItem?.id}}).commit().event("channelTabMainPlaylist").params().permit(e=>{let{params:t}=e;return!t.isSubscribed}).message(e=>{let{context:t}=e;return{event_context:w,channel_id:(0,h.r9)(t)}}).commit().event("channelTabMainAudio").params().permit(e=>{let{params:t}=e;retur
## api paths with query templates
/api/playlist/custom/${e.playlistId}/videos/
/api/playlist/custom/${e}/
/api/playlist/future/list/
/api/playlist/future/video/${e}/
/api/playlist/user/${t}
