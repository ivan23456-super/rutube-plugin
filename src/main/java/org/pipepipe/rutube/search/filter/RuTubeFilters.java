package org.pipepipe.rutube.search.filter;

import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterGroup;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.search.filter.SearchFiltersBase;

/**
 * Search filters mirroring the RuTube search page.
 *
 * <p>Only the filters the RuTube API actually understands are exposed:</p>
 * <ul>
 *   <li>content type: videos, channels, Shorts (RuTube does not search playlists);</li>
 *   <li>publication date: {@code created=day|week|month|year};</li>
 *   <li>duration: {@code duration=short|medium|long|movie}.</li>
 * </ul>
 *
 * <p>Two details matter for the API:</p>
 * <ul>
 *   <li>the "all"/"any" entries must not send anything — RuTube answers
 *   {@code created=all-time} and {@code duration=any} with HTTP 400, which the app
 *   shows as "no results";</li>
 *   <li>ordering is not supported by the search backend ({@code order_by=...} is
 *   accepted but ignored, other spellings return HTTP 400), so no sort group is
 *   offered instead of a control that does nothing.</li>
 * </ul>
 */
public final class RuTubeFilters extends SearchFiltersBase {

    public static final String CONTENT_VIDEOS = "videos";
    public static final String CONTENT_CHANNELS = "channels";
    public static final String CONTENT_SHORTS = "shorts";

    public RuTubeFilters() {
        init();
        build();
    }

    @Override
    public String evaluateSelectedFilters(final String searchString) {
        final StringBuilder query = new StringBuilder();
        if (selectedSortFilter == null) {
            return "";
        }
        for (final FilterItem item : selectedSortFilter) {
            if (!(item instanceof RuTubeFilterItem)) {
                continue;
            }
            final String data = ((RuTubeFilterItem) item).getQueryData();
            if (data == null || data.isEmpty()) {
                // "all" / "any": nothing to send, the API rejects those values.
                continue;
            }
            if (query.indexOf(data) >= 0) {
                continue;
            }
            query.append('&').append(data);
        }
        return query.toString();
    }

    @Override
    protected void init() {
        // Content filters: the kind of content selects the search endpoint.
        final int contentVideos = builder.addFilterItem(
                new RuTubeFilterItem(CONTENT_VIDEOS, ""));
        final int contentChannels = builder.addFilterItem(
                new RuTubeFilterItem(CONTENT_CHANNELS, ""));
        final int contentShorts = builder.addFilterItem(
                new RuTubeFilterItem(CONTENT_SHORTS, ""));

        this.defaultContentFilterId = contentVideos;

        addContentFilter(builder.createSortGroup(null, true, new FilterItem[]{
                builder.getFilterForId(contentVideos),
                builder.getFilterForId(contentChannels),
                builder.getFilterForId(contentShorts),
        }));

        // Publication date (created=...). Values are validated by RuTube.
        final int createdAll = builder.addSortItem(
                new RuTubeFilterItem("all", ""));
        final int createdDay = builder.addSortItem(
                new RuTubeFilterItem("day", "created=day"));
        final int createdWeek = builder.addSortItem(
                new RuTubeFilterItem("week", "created=week"));
        final int createdMonth = builder.addSortItem(
                new RuTubeFilterItem("month", "created=month"));
        final int createdYear = builder.addSortItem(
                new RuTubeFilterItem("year", "created=year"));

        // Duration (duration=...).
        final int durationAny = builder.addSortItem(
                new RuTubeFilterItem("any", ""));
        final int durationShort = builder.addSortItem(
                new RuTubeFilterItem("short", "duration=short"));
        final int durationMedium = builder.addSortItem(
                new RuTubeFilterItem("medium", "duration=medium"));
        final int durationLong = builder.addSortItem(
                new RuTubeFilterItem("long", "duration=long"));
        final int durationMovie = builder.addSortItem(
                new RuTubeFilterItem("movie", "duration=movie"));

        final Filter allSortFilters = new Filter.Builder(new FilterGroup[]{
                builder.createSortGroup("created", true, new FilterItem[]{
                        builder.getFilterForId(createdAll),
                        builder.getFilterForId(createdDay),
                        builder.getFilterForId(createdWeek),
                        builder.getFilterForId(createdMonth),
                        builder.getFilterForId(createdYear),
                }),
                builder.createSortGroup("duration", true, new FilterItem[]{
                        builder.getFilterForId(durationAny),
                        builder.getFilterForId(durationShort),
                        builder.getFilterForId(durationMedium),
                        builder.getFilterForId(durationLong),
                        builder.getFilterForId(durationMovie),
                }),
        }).build();

        addContentFilterSortVariant(-1, allSortFilters);
        addContentFilterSortVariant(contentVideos, allSortFilters);
        addContentFilterSortVariant(contentChannels, allSortFilters);
        addContentFilterSortVariant(contentShorts, allSortFilters);
    }

    private static class RuTubeFilterItem extends FilterItem {
        private final String query;

        RuTubeFilterItem(final String name, final String query) {
            super(Filter.ITEM_IDENTIFIER_UNKNOWN, name);
            this.query = query;
        }

        public String getQueryData() {
            return query;
        }
    }
}
