package org.pipepipe.rutube;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.schabi.newpipe.extractor.utils.Utils.EMPTY_STRING;
import static org.schabi.newpipe.extractor.utils.Utils.UTF_8;

/**
 * Shared helpers for the RuTube service: API endpoints, safe JSON accessors,
 * date parsing and URL builders.
 *
 * <p>All endpoints below were verified against the live RuTube API. RuTube serves
 * its public data as JSON under {@code https://rutube.ru/api/...} without any
 * authentication; the newer search backend lives under
 * {@code https://rutube.ru/pangolin/api/web/...}.</p>
 */
public final class RuTubeParsingHelper {

    public static final String BASE_URL = "https://rutube.ru";
    public static final String API_BASE = BASE_URL + "/api";

    /** Video metadata: title, description, author, views, duration, ... */
    public static final String VIDEO_API = API_BASE + "/video/%s/?format=json";
    /** Play options: HLS master playlist, live streams, captions, ACL. */
    public static final String PLAY_OPTIONS_API = API_BASE + "/play/options/%s/?format=json";
    /** Channel info: name, avatar, banner, subscribers, description. */
    public static final String CHANNEL_INFO_API = API_BASE + "/profile/user/%s/";
    /** Paginated channel videos (origin__type selects videos/shorts). */
    public static final String CHANNEL_VIDEOS_API =
            API_BASE + "/video/person/%s/?page=%s&format=json&origin__type=%s";
    /** Playlist info. */
    public static final String PLAYLIST_INFO_API =
            API_BASE + "/playlist/custom/%s/?format=json";
    /** Paginated playlist videos. */
    public static final String PLAYLIST_VIDEOS_API =
            API_BASE + "/playlist/custom/%s/videos?page=%s&format=json";
    /** Movie (metainfo/tv) info and videos. */
    public static final String MOVIE_INFO_API = API_BASE + "/metainfo/tv/%s/?format=json";
    public static final String MOVIE_VIDEOS_API =
            API_BASE + "/metainfo/tv/%s/video?page=%s&format=json";
    /** Search: videos and playlists (pangolin backend). */
    public static final String SEARCH_VIDEOS_API =
            BASE_URL + "/pangolin/api/web/v1/search/combined/video_playlist";
    /** Search: channels. */
    public static final String SEARCH_CHANNELS_API =
            API_BASE + "/search/combined/cards/list";
    /** Search: Shorts. */
    public static final String SEARCH_SHORTS_API = API_BASE + "/search/shorts";
    /** Search suggestions. */
    public static final String SEARCH_SUGGEST_API = API_BASE + "/search/suggest";
    /** Video comments (cursor pagination via the {@code next} URL). */
    public static final String COMMENTS_API = API_BASE + "/v3/comments/video/%s/?limit=20";
    /** Homepage / trending feed, used for the kiosk. */
    public static final String RECOMMENDATION_API =
            API_BASE + "/v2/video/recommendation/main?limit=20&offset=%s";

    /** origin__type values for the channel video list. */
    public static final String ORIGIN_VIDEOS = "rtb,rst,ifrm,rspa";
    public static final String ORIGIN_SHORTS = "rshorts";

    /** Items per page returned by the RuTube list endpoints. */
    public static final int ITEMS_PER_PAGE = 20;

    /**
     * Headers the RuTube web player sends with its API requests. RuTube answers
     * some requests (notably play options of TV channels) with a player stub when
     * they do not look like they come from the site, so they are sent along with
     * every API call.
     */
    private static final Map<String, List<String>> API_HEADERS;

    /** Headers used for media playlists and other CDN requests. */
    private static final Map<String, List<String>> MEDIA_HEADERS;

    /** The same headers as plain strings, for the player. */
    private static final Map<String, String> PLAYER_HEADERS;

    static {
        API_HEADERS = headers("application/json, text/plain, */*");
        MEDIA_HEADERS = headers("*/*");
        final Map<String, String> playerHeaders = new HashMap<>();
        playerHeaders.put("Referer", BASE_URL + "/");
        playerHeaders.put("Origin", BASE_URL);
        PLAYER_HEADERS = Collections.unmodifiableMap(playerHeaders);
    }

    @Nonnull
    private static Map<String, List<String>> headers(@Nonnull final String accept) {
        final Map<String, List<String>> map = new HashMap<>();
        map.put("Referer", Collections.singletonList(BASE_URL + "/"));
        map.put("Origin", Collections.singletonList(BASE_URL));
        map.put("Accept", Collections.singletonList(accept));
        return Collections.unmodifiableMap(map);
    }

    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
    };

    private RuTubeParsingHelper() {
    }

    // ------------------------------------------------------------------
    // HTTP / JSON
    // ------------------------------------------------------------------

    /**
     * Fetches a JSON object from the given URL.
     *
     * @throws ExtractionException when the request fails or the body is not a JSON object
     */
    @Nonnull
    public static JsonObject fetchJson(@Nonnull final Downloader downloader,
                                       @Nonnull final String url)
            throws IOException, ExtractionException {
        return fetchJson(downloader, url, API_HEADERS);
    }

    /**
     * Fetches a JSON object with the given headers. RuTube answers some requests
     * with a player stub ({@code {"detail": {"type": "player_stub"|"blocking_rule"}}})
     * instead of an error code; those are turned into an {@link ExtractionException}
     * carrying a human readable reason.
     */
    @Nonnull
    public static JsonObject fetchJson(@Nonnull final Downloader downloader,
                                       @Nonnull final String url,
                                       @Nonnull final Map<String, List<String>> headers)
            throws IOException, ExtractionException {
        final Response response = downloader.get(url, headers);
        if (response == null || Utils.isBlank(response.responseBody())) {
            throw new ExtractionException("Empty response from " + url);
        }
        final String body = response.responseBody();
        JsonObject json = null;
        if (body.trim().startsWith("{")) {
            try {
                json = JsonParser.object().from(body);
            } catch (final Exception ignored) {
                // not JSON after all, handled below
            }
        }
        if (json != null) {
            final JsonObject detail = getObject(json, "detail");
            if (detail != null && !getString(detail, "type").isEmpty()) {
                throw new ExtractionException(describeStub(detail));
            }
            return json;
        }
        if (response.responseCode() == 404) {
            throw new ExtractionException("Not found: " + url);
        }
        throw new ExtractionException("Could not parse JSON from " + url);
    }

    @Nonnull
    private static String describeStub(@Nonnull final JsonObject detail) {
        final String type = getString(detail, "type");
        final String name = getString(detail, "name");
        if ("stream_yet_finished".equals(name)) {
            return "Трансляция завершена";
        }
        if ("blocking_rule".equals(type)) {
            return "RuTube заблокировал это видео для вашего запроса (blocking_rule). "
                    + "Обычно это гео-ограничение или требование авторизации.";
        }
        return "RuTube не отдал видео (" + (name.isEmpty() ? type : name) + ")";
    }

    /**
     * Fetches a plain text body (used for the HLS master playlist).
     */
    @Nonnull
    public static String fetchText(@Nonnull final Downloader downloader,
                                   @Nonnull final String url)
            throws IOException, ExtractionException {
        return fetchText(downloader, url, MEDIA_HEADERS);
    }

    @Nonnull
    public static String fetchText(@Nonnull final Downloader downloader,
                                   @Nonnull final String url,
                                   @Nonnull final Map<String, List<String>> headers)
            throws IOException, ExtractionException {
        final Response response = downloader.get(url, headers);
        if (response == null || Utils.isBlank(response.responseBody())) {
            throw new ExtractionException("Empty response from " + url);
        }
        if (response.responseCode() >= 400) {
            throw new ExtractionException("HTTP " + response.responseCode()
                    + " from " + url);
        }
        return response.responseBody();
    }

    /**
     * RuTube still returns {@code http://} links for some lists (for example the
     * shorts search {@code next} URL points at {@code search-backend.rutube.ru}
     * over plain HTTP), which Android refuses to load. Such links are upgraded to
     * HTTPS.
     */
    /** Headers to give to the player for HLS/CDN requests. */
    @Nonnull
    public static Map<String, String> mediaHeaders() {
        return PLAYER_HEADERS;
    }

    /** Headers to send along with API requests. */
    @Nonnull
    public static Map<String, List<String>> apiHeaders() {
        return API_HEADERS;
    }

    @Nonnull
    public static String httpsify(@Nullable final String url) {
        if (url == null) {
            return EMPTY_STRING;
        }
        return url.startsWith("http://") ? "https://" + url.substring(7) : url;
    }

    /**
     * Returns the URLs of an HLS master playlist found anywhere inside
     * {@code video_balancer} / {@code live_streams} style JSON: values can be
     * strings, objects or arrays depending on the kind of stream.
     */
    @Nonnull
    public static List<String> collectHlsUrls(@Nullable final Object node,
                                              @Nonnull final List<String> urls) {
        if (node instanceof String) {
            final String value = ((String) node).trim();
            if (value.startsWith("http") && !urls.contains(value)) {
                urls.add(value);
            }
        } else if (node instanceof JsonObject) {
            final JsonObject object = (JsonObject) node;
            for (final String key : object.keySet()) {
                collectHlsUrls(object.get(key), urls);
            }
        } else if (node instanceof com.grack.nanojson.JsonArray) {
            final com.grack.nanojson.JsonArray array =
                    (com.grack.nanojson.JsonArray) node;
            for (final Object item : array) {
                collectHlsUrls(item, urls);
            }
        }
        return urls;
    }

    /** Joins the top level keys of a JSON object, for diagnostics. */
    @Nonnull
    public static String keysOf(@Nullable final JsonObject json) {
        if (json == null) {
            return "[]";
        }
        return String.join(",", json.keySet());
    }

    // ------------------------------------------------------------------
    // Safe JSON accessors (nanojson getters are lenient but can throw
    // ClassCastException on type mismatches, so every access is guarded)
    // ------------------------------------------------------------------

    @Nonnull
    public static String getString(@Nullable final JsonObject json,
                                   @Nonnull final String key) {
        return getString(json, key, EMPTY_STRING);
    }

    @Nonnull
    public static String getString(@Nullable final JsonObject json,
                                   @Nonnull final String key,
                                   @Nonnull final String defaultValue) {
        if (json == null) {
            return defaultValue;
        }
        try {
            final Object value = json.get(key);
            if (value instanceof String) {
                return (String) value;
            }
        } catch (final Exception ignored) {
            // fall through to the default
        }
        return defaultValue;
    }

    public static long getLong(@Nullable final JsonObject json,
                               @Nonnull final String key) {
        return getLong(json, key, -1);
    }

    public static long getLong(@Nullable final JsonObject json,
                               @Nonnull final String key,
                               final long defaultValue) {
        if (json == null) {
            return defaultValue;
        }
        try {
            final Object value = json.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } catch (final Exception ignored) {
            // fall through to the default
        }
        return defaultValue;
    }

    public static boolean getBoolean(@Nullable final JsonObject json,
                                     @Nonnull final String key) {
        if (json == null) {
            return false;
        }
        try {
            final Object value = json.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (final Exception ignored) {
            // fall through
        }
        return false;
    }

    @Nullable
    public static JsonObject getObject(@Nullable final JsonObject json,
                                       @Nonnull final String key) {
        if (json == null) {
            return null;
        }
        try {
            final Object value = json.get(key);
            if (value instanceof JsonObject) {
                return (JsonObject) value;
            }
        } catch (final Exception ignored) {
            // fall through
        }
        return null;
    }

    @Nullable
    public static JsonArray getArray(@Nullable final JsonObject json,
                                     @Nonnull final String key) {
        if (json == null) {
            return null;
        }
        try {
            final Object value = json.get(key);
            if (value instanceof JsonArray) {
                return (JsonArray) value;
            }
        } catch (final Exception ignored) {
            // fall through
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Dates
    // ------------------------------------------------------------------

    /**
     * Parses a RuTube timestamp such as {@code 2023-02-23T11:55:29} or
     * {@code 2026-03-14T18:13:41.558890} (with optional timezone offset).
     *
     * @return the parsed date, or {@code null} when it cannot be parsed
     */
    @Nullable
    public static DateWrapper parseDate(@Nullable final String dateString) {
        if (Utils.isBlank(dateString)) {
            return null;
        }
        final String trimmed = dateString.trim();
        for (final String pattern : DATE_PATTERNS) {
            try {
                final SimpleDateFormat format =
                        new SimpleDateFormat(pattern, Locale.US);
                format.setLenient(false);
                format.setTimeZone(TimeZone.getDefault());
                final Calendar calendar = Calendar.getInstance();
                calendar.setTime(format.parse(trimmed));
                return new DateWrapper(calendar);
            } catch (final ParseException ignored) {
                // try the next pattern
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // URLs
    // ------------------------------------------------------------------

    @Nonnull
    public static String buildVideoUrl(@Nonnull final String videoId) {
        return BASE_URL + "/video/" + videoId + "/";
    }

    @Nonnull
    public static String buildChannelUrl(@Nonnull final String channelId) {
        if (channelId.matches("\\d+")) {
            return BASE_URL + "/channel/" + channelId + "/";
        }
        return BASE_URL + "/u/" + channelId + "/";
    }

    @Nonnull
    public static String buildPlaylistUrl(@Nonnull final String playlistId) {
        return BASE_URL + "/plst/" + playlistId + "/";
    }

    /**
     * Extracts the private-video token ({@code p} query parameter) from a video URL,
     * if present. Private videos need this token to be passed to the API.
     */
    @Nullable
    public static String extractPrivateToken(@Nullable final String url) {
        if (Utils.isBlank(url)) {
            return null;
        }
        final int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return null;
        }
        final String query = url.substring(queryStart + 1);
        for (final String pair : query.split("&")) {
            final int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals("p")) {
                try {
                    return URLDecoder.decode(pair.substring(eq + 1), UTF_8);
                } catch (final UnsupportedEncodingException e) {
                    return pair.substring(eq + 1);
                }
            }
        }
        return null;
    }

    /**
     * Appends the private-video token to an API URL when present.
     */
    @Nonnull
    public static String withPrivateToken(@Nonnull final String apiUrl,
                                          @Nullable final String privateToken) {
        if (Utils.isBlank(privateToken)) {
            return apiUrl;
        }
        final String separator = apiUrl.contains("?") ? "&" : "?";
        return apiUrl + separator + "p=" + privateToken;
    }

    @Nonnull
    public static String encode(@Nonnull final String value) throws ParsingException {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (final UnsupportedEncodingException e) {
            throw new ParsingException("Could not encode query", e);
        }
    }

    /**
     * Resolves a channel slug (from {@code /u/{slug}/} URLs) to the numeric channel id
     * by parsing the server-rendered page state, like yt-dlp does.
     */
    public static String resolveChannelSlug(@Nonnull final Downloader downloader,
                                            @Nonnull final String slug)
            throws IOException, ExtractionException {
        final String html = fetchText(downloader, BASE_URL + "/u/" + slug + "/");
        final java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("channelIdBySlug.*?\"channel_id\"\\s*:\\s*(\\d+)")
                .matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new ExtractionException("Could not resolve channel slug: " + slug);
    }

    /**
     * Returns the numeric channel id for a channel link handler id, resolving slugs
     * when needed. Numeric ids are returned unchanged.
     */
    @Nonnull
    public static String resolveChannelId(@Nonnull final Downloader downloader,
                                          @Nonnull final String channelId)
            throws IOException, ExtractionException {
        if (channelId.matches("\\d+")) {
            return channelId;
        }
        return resolveChannelSlug(downloader, channelId);
    }
}
