package org.pipepipe.rutube.search.filter;

import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterGroup;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.search.filter.SearchFiltersBase;

/**
 * Search filters mirroring the RuTube search page: content type
 * (videos/channels/playlists/Shorts), publication date, duration and ordering.
 * The query fragments produced here are appended to the search API URLs by
 * {@link org.pipepipe.rutube.linkHandler.RuTubeSearchQueryHandlerFactory}.
 */
public final class RuTubeFilters extends SearchFiltersBase {

    public static final String CONTENT_VIDEOS = "videos";
    public static final String CONTENT_CHANNELS = "channels";
    public static final String CONTENT_PLAYLISTS = "playlists";
    public static final String CONTENT_SHORTS = "shorts";

    public RuTubeFilters() {
        init();
        build();
    }

    @Override
    public String evaluateSelectedFilters(final String searchString) {
        final StringBuilder query = new StringBuilder();
        if (selectedSortFilter != null) {
            for (final FilterItem item : selectedSortFilter) {
                if (item instanceof RuTubeFilterItem) {
                    final String data = ((RuTubeFilterItem) item).getQueryData();
                    if (data != null && !data.isEmpty()) {
                        query.append('&').append(data);
                    }
                }
            }
        }
        return query.toString();
    }

    @Override
    protected void init() {
        // Content filters: the kind of content selects the search endpoint.
        final int contentVideos = builder.addSortItem(
                new RuTubeFilterItem(CONTENT_VIDEOS, ""));
        final int contentChannels = builder.addSortItem(
                new RuTubeFilterItem(CONTENT_CHANNELS, ""));
        final int contentPlaylists = builder.addSortItem(
                new RuTubeFilterItem(CONTENT_PLAYLISTS, ""));
        final int contentShorts = builder.addSortItem(
                new RuTubeFilterItem(CONTENT_SHORTS, ""));

        this.defaultContentFilterId = contentVideos;

        addContentFilter(builder.createSortGroup(null, true, new FilterItem[]{
                builder.getFilterForId(contentVideos),
                builder.getFilterForId(contentChannels),
                builder.getFilterForId(contentPlaylists),
                builder.getFilterForId(contentShorts),
        }));

        // Publication date (created=...).
        final int createdAll = builder.addSortItem(
                new RuTubeFilterItem("all", "created=all-time"));
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
                new RuTubeFilterItem("any", "duration=any"));
        final int durationShort = builder.addSortItem(
                new RuTubeFilterItem("short", "duration=short"));
        final int durationMedium = builder.addSortItem(
                new RuTubeFilterItem("medium", "duration=medium"));
        final int durationLong = builder.addSortItem(
                new RuTubeFilterItem("long", "duration=long"));
        final int durationMovie = builder.addSortItem(
                new RuTubeFilterItem("movie", "duration=movie"));

        // Ordering (order_by=...). Only values accepted by the API are used.
        final int orderRelevance = builder.addSortItem(
                new RuTubeFilterItem("relevance", ""));
        final int orderDateNew = builder.addSortItem(
                new RuTubeFilterItem("date_new", "order_by=created_ts_down"));
        final int orderDateOld = builder.addSortItem(
                new RuTubeFilterItem("date_old", "order_by=created_ts_up"));
        final int orderViews = builder.addSortItem(
                new RuTubeFilterItem("views", "order_by=hits"));

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
                builder.createSortGroup("sortby", true, new FilterItem[]{
                        builder.getFilterForId(orderRelevance),
                        builder.getFilterForId(orderDateNew),
                        builder.getFilterForId(orderDateOld),
                        builder.getFilterForId(orderViews),
                }),
        }).build();

        addContentFilterSortVariant(-1, allSortFilters);
        addContentFilterSortVariant(contentVideos, allSortFilters);
        addContentFilterSortVariant(contentChannels, allSortFilters);
        addContentFilterSortVariant(contentPlaylists, allSortFilters);
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
